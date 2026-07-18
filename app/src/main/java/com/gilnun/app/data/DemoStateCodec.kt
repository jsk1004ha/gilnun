package com.gilnun.app.data

import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceId

/**
 * Dependency-free, strict JSON codec for the service-scoped durable demo state.
 *
 * Global envelope corruption fails closed to a complete default. Once the exact V2 service
 * envelope is established, corruption in one known service resets only that service.
 */
object DemoStateCodec {
    const val SCHEMA_VERSION = 2
    const val MAX_ENCODED_LENGTH = 16_384

    data class DecodeResult(
        val state: DemoState,
        val migratedFromV1: Boolean,
    )

    fun encode(state: DemoState): String =
        buildString {
            append('{')
            append("\"schemaVersion\":")
            append(SCHEMA_VERSION)
            append(",\"services\":{")
            ServiceId.entries.forEachIndexed { index, serviceId ->
                if (index > 0) append(',')
                appendJsonString(serviceId.persistedKey)
                append(':')
                val progress =
                    state.services
                        .getValue(serviceId)
                        .takeIf { candidate -> isValidProgress(serviceId, candidate) }
                        ?: ServiceProgress()
                appendProgress(serviceId, progress)
            }
            append("}}")
        }

    fun decode(json: String?): DemoState = decodeWithMetadata(json).state

    fun decodeOrDefault(json: String?): DemoState = decode(json)

    fun decodeWithMetadata(json: String?): DecodeResult {
        if (json.isNullOrBlank() || json.length > MAX_ENCODED_LENGTH) {
            return DecodeResult(DemoState(), migratedFromV1 = false)
        }

        return runCatching {
            val root = JsonParser(json).parseRoot().asObject()
            when (root.requiredInt("schemaVersion")) {
                SCHEMA_VERSION ->
                    DecodeResult(
                        state = root.toVersionTwoState(),
                        migratedFromV1 = false,
                    )
                LEGACY_SCHEMA_VERSION ->
                    DecodeResult(
                        state = root.toMigratedVersionOneState(),
                        migratedFromV1 = true,
                    )
                else -> throw InvalidStateJson()
            }
        }.getOrDefault(DecodeResult(DemoState(), migratedFromV1 = false))
    }

    private fun JsonObject.toVersionTwoState(): DemoState {
        requireExactKeys(V2_TOP_LEVEL_KEYS)
        if (requiredInt("schemaVersion") != SCHEMA_VERSION) throw InvalidStateJson()

        val serviceValues = requiredObject("services")
        serviceValues.requireExactKeys(SERVICE_KEYS)
        val services =
            ServiceId.entries.associateWith { serviceId ->
                runCatching {
                    serviceValues
                        .requiredObject(serviceId.persistedKey)
                        .toServiceProgress(serviceId)
                }.getOrDefault(ServiceProgress())
            }
        return DemoState(services)
    }

    private fun JsonObject.toMigratedVersionOneState(): DemoState {
        requireExactKeys(V1_TOP_LEVEL_KEYS)
        if (requiredInt("schemaVersion") != LEGACY_SCHEMA_VERSION) throw InvalidStateJson()

        val discardedPatch = requiredNullableObject("patch")?.toPatch()
        if (discardedPatch != null && !discardedPatch.hasValidSemanticFields()) {
            throw InvalidStateJson()
        }
        val discardedReceipt = requiredNullableObject("lastReceipt")?.toLegacyReceipt()
        if (discardedReceipt != null && !discardedReceipt.isTruthful()) {
            throw InvalidStateJson()
        }
        val helpLevel = requiredInt("helpLevel")
        if (helpLevel !in MIN_HELP_LEVEL..MAX_HELP_LEVEL) throw InvalidStateJson()

        return DemoState(
            services =
                ServiceId.entries.associateWith {
                    ServiceProgress(helpLevel = helpLevel)
                },
        )
    }

    private fun JsonObject.toServiceProgress(serviceId: ServiceId): ServiceProgress {
        requireExactKeys(SERVICE_PROGRESS_KEYS)
        val progress =
            ServiceProgress(
                helperPatchesByCheckpoint =
                    requiredObject("helperPatchesByCheckpoint").toHelperPatches(serviceId),
                helpLevel = requiredInt("helpLevel"),
                lastReceipt = optionalObject("lastReceipt")?.toReceipt(),
            )
        if (!isValidProgress(serviceId, progress)) throw InvalidStateJson()
        return progress
    }

    private fun JsonObject.toHelperPatches(serviceId: ServiceId): Map<String, PatchV1> {
        requireUniqueKeys()
        if (values.size > MAX_PATCHES_PER_SERVICE) throw InvalidStateJson()

        val knownCheckpoints =
            ServiceCatalog
                .require(serviceId)
                .steps
                .map { checkpoint -> checkpoint.id }
                .toSet()
        if (values.keys.any { checkpoint -> checkpoint !in knownCheckpoints }) {
            throw InvalidStateJson()
        }

        return values.mapValues { (checkpoint, value) ->
            val patch = (value as? JsonObject)?.toPatch() ?: throw InvalidStateJson()
            if (patch != ServiceCatalog.builtInPatch(serviceId, checkpoint)) {
                throw InvalidStateJson()
            }
            patch
        }
    }

    private fun JsonObject.toPatch(): PatchV1 {
        requireExactKeys(PATCH_KEYS)
        return PatchV1(
            pageId = requiredString("pageId"),
            compatibleRevision = requiredString("compatibleRevision"),
            stableKey = requiredString("stableKey"),
            role = requiredString("role"),
            accessibleName = requiredString("accessibleName"),
            expectedState = requiredString("expectedState"),
        )
    }

    private fun JsonObject.toReceipt(): ActionReceipt {
        requireExactKeys(RECEIPT_KEYS)
        val outcome =
            runCatching { ReceiptOutcome.valueOf(requiredString("outcome")) }
                .getOrElse { throw InvalidStateJson() }
        val source =
            runCatching { GuidanceSource.valueOf(requiredString("source")) }
                .getOrElse { throw InvalidStateJson() }
        return ActionReceipt(
            guidanceShown = requiredBoolean("guidanceShown"),
            userActionObserved = requiredBoolean("userActionObserved"),
            postconditionVerified = requiredBoolean("postconditionVerified"),
            outcome = outcome,
            source = source,
        )
    }

    private fun JsonObject.toLegacyReceipt(): ActionReceipt {
        requireExactKeys(LEGACY_RECEIPT_KEYS)
        val outcome =
            runCatching { ReceiptOutcome.valueOf(requiredString("outcome")) }
                .getOrElse { throw InvalidStateJson() }
        return ActionReceipt(
            guidanceShown = requiredBoolean("guidanceShown"),
            userActionObserved = requiredBoolean("userActionObserved"),
            postconditionVerified = requiredBoolean("postconditionVerified"),
            outcome = outcome,
        )
    }

    private fun isValidProgress(
        serviceId: ServiceId,
        progress: ServiceProgress,
    ): Boolean =
        progress.helpLevel in MIN_HELP_LEVEL..MAX_HELP_LEVEL &&
            progress.helperPatchesByCheckpoint.size <= MAX_PATCHES_PER_SERVICE &&
            progress.helperPatchesByCheckpoint.all { (checkpoint, patch) ->
                patch == ServiceCatalog.builtInPatch(serviceId, checkpoint)
            } &&
            (
                progress.lastReceipt == null ||
                    (
                        progress.lastReceipt.source != null &&
                            progress.lastReceipt.isTruthful()
                    )
            )

    private fun ActionReceipt.isTruthful(): Boolean {
        val fullyVerified = guidanceShown && userActionObserved && postconditionVerified
        return (outcome == ReceiptOutcome.VERIFIED) == fullyVerified
    }

    private fun StringBuilder.appendProgress(
        serviceId: ServiceId,
        progress: ServiceProgress,
    ) {
        append('{')
        append("\"helperPatchesByCheckpoint\":{")
        val orderedPatches =
            ServiceCatalog
                .require(serviceId)
                .steps
                .mapNotNull { checkpoint ->
                    progress.helperPatchesByCheckpoint[checkpoint.id]?.let { patch ->
                        checkpoint.id to patch
                    }
                }
        orderedPatches.forEachIndexed { index, (checkpoint, patch) ->
            if (index > 0) append(',')
            appendJsonString(checkpoint)
            append(':')
            appendPatch(patch)
        }
        append('}')
        append(",\"helpLevel\":")
        append(progress.helpLevel)
        append(",\"lastReceipt\":")
        appendReceipt(progress.lastReceipt)
        append('}')
    }

    private fun StringBuilder.appendPatch(patch: PatchV1) {
        append('{')
        appendJsonEntry("pageId", patch.pageId)
        append(',')
        appendJsonEntry("compatibleRevision", patch.compatibleRevision)
        append(',')
        appendJsonEntry("stableKey", patch.stableKey)
        append(',')
        appendJsonEntry("role", patch.role)
        append(',')
        appendJsonEntry("accessibleName", patch.accessibleName)
        append(',')
        appendJsonEntry("expectedState", patch.expectedState)
        append('}')
    }

    private fun StringBuilder.appendReceipt(receipt: ActionReceipt?) {
        if (receipt == null) {
            append("null")
            return
        }
        append('{')
        append("\"guidanceShown\":")
        append(receipt.guidanceShown)
        append(",\"userActionObserved\":")
        append(receipt.userActionObserved)
        append(",\"postconditionVerified\":")
        append(receipt.postconditionVerified)
        append(',')
        appendJsonEntry("outcome", receipt.outcome.name)
        append(',')
        appendJsonEntry("source", checkNotNull(receipt.source).name)
        append('}')
    }

    private fun StringBuilder.appendJsonEntry(
        key: String,
        value: String,
    ) {
        appendJsonString(key)
        append(':')
        appendJsonString(value)
    }

    private fun StringBuilder.appendJsonString(value: String) {
        append('"')
        value.forEach { character ->
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else ->
                    if (character.code < 0x20) {
                        append("\\u")
                        append(character.code.toString(16).padStart(4, '0'))
                    } else {
                        append(character)
                    }
            }
        }
        append('"')
    }

    private const val LEGACY_SCHEMA_VERSION = 1
    private const val MIN_HELP_LEVEL = 0
    private const val MAX_HELP_LEVEL = 3
    private const val MAX_PATCHES_PER_SERVICE = 3

    private val V2_TOP_LEVEL_KEYS = setOf("schemaVersion", "services")
    private val V1_TOP_LEVEL_KEYS =
        setOf("schemaVersion", "patch", "helpLevel", "lastReceipt")
    private val SERVICE_KEYS = ServiceId.entries.map(ServiceId::persistedKey).toSet()
    private val SERVICE_PROGRESS_KEYS =
        setOf("helperPatchesByCheckpoint", "helpLevel", "lastReceipt")
    private val PATCH_KEYS =
        setOf(
            "pageId",
            "compatibleRevision",
            "stableKey",
            "role",
            "accessibleName",
            "expectedState",
        )
    private val RECEIPT_KEYS =
        setOf(
            "guidanceShown",
            "userActionObserved",
            "postconditionVerified",
            "outcome",
            "source",
        )
    private val LEGACY_RECEIPT_KEYS = RECEIPT_KEYS - "source"
}

private sealed interface JsonValue

private data class JsonObject(
    val values: Map<String, JsonValue>,
    val hasDuplicateKeys: Boolean = false,
) : JsonValue {
    fun requireExactKeys(expected: Set<String>) {
        if (hasDuplicateKeys || values.keys != expected) throw InvalidStateJson()
    }

    fun requireUniqueKeys() {
        if (hasDuplicateKeys) throw InvalidStateJson()
    }

    fun requiredString(key: String): String =
        (values[key] as? JsonString)?.value ?: throw InvalidStateJson()

    fun requiredBoolean(key: String): Boolean =
        (values[key] as? JsonBoolean)?.value ?: throw InvalidStateJson()

    fun requiredInt(key: String): Int =
        (values[key] as? JsonNumber)?.value?.toIntOrNull() ?: throw InvalidStateJson()

    fun requiredObject(key: String): JsonObject =
        values[key] as? JsonObject ?: throw InvalidStateJson()

    fun requiredNullableObject(key: String): JsonObject? {
        if (key !in values) throw InvalidStateJson()
        return when (val value = values[key]) {
            JsonNull -> null
            is JsonObject -> value
            else -> throw InvalidStateJson()
        }
    }

    fun optionalObject(key: String): JsonObject? =
        when (val value = values[key]) {
            JsonNull -> null
            is JsonObject -> value
            else -> throw InvalidStateJson()
        }
}

private data class JsonString(
    val value: String,
) : JsonValue

private data class JsonBoolean(
    val value: Boolean,
) : JsonValue

private data class JsonNumber(
    val value: String,
) : JsonValue

private data object JsonNull : JsonValue

private class InvalidStateJson : RuntimeException()

private class JsonParser(
    private val source: String,
) {
    private var index = 0

    fun parseRoot(): JsonValue {
        skipWhitespace()
        val value = parseValue(depth = 0)
        skipWhitespace()
        if (index != source.length) throw InvalidStateJson()
        return value
    }

    private fun parseValue(depth: Int): JsonValue {
        if (depth > MAX_DEPTH || index >= source.length) throw InvalidStateJson()
        return when (source[index]) {
            '{' -> parseObject(depth + 1)
            '"' -> JsonString(parseString())
            't' -> {
                consumeLiteral("true")
                JsonBoolean(true)
            }
            'f' -> {
                consumeLiteral("false")
                JsonBoolean(false)
            }
            'n' -> {
                consumeLiteral("null")
                JsonNull
            }
            '-', in '0'..'9' -> JsonNumber(parseInteger())
            else -> throw InvalidStateJson()
        }
    }

    private fun parseObject(depth: Int): JsonObject {
        expect('{')
        skipWhitespace()
        if (consumeIf('}')) return JsonObject(emptyMap())

        val values = linkedMapOf<String, JsonValue>()
        var fieldCount = 0
        var hasDuplicateKeys = false
        while (true) {
            skipWhitespace()
            if (index >= source.length || source[index] != '"') throw InvalidStateJson()
            if (fieldCount >= MAX_OBJECT_FIELDS) throw InvalidStateJson()
            fieldCount += 1
            val key = parseString()
            if (key in values) hasDuplicateKeys = true
            skipWhitespace()
            expect(':')
            skipWhitespace()
            values[key] = parseValue(depth)
            skipWhitespace()
            if (consumeIf('}')) break
            expect(',')
        }
        return JsonObject(values, hasDuplicateKeys)
    }

    private fun parseString(): String {
        expect('"')
        val result = StringBuilder()
        while (index < source.length) {
            val character = source[index++]
            when {
                character == '"' -> return result.toString()
                character == '\\' -> result.append(parseEscape())
                character.code < 0x20 -> throw InvalidStateJson()
                else -> result.append(character)
            }
            if (result.length > DemoStateCodec.MAX_ENCODED_LENGTH) throw InvalidStateJson()
        }
        throw InvalidStateJson()
    }

    private fun parseEscape(): Char {
        if (index >= source.length) throw InvalidStateJson()
        return when (val escaped = source[index++]) {
            '"' -> '"'
            '\\' -> '\\'
            '/' -> '/'
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> parseUnicodeEscape()
            else -> throw InvalidStateJson()
        }
    }

    private fun parseUnicodeEscape(): Char {
        if (index + 4 > source.length) throw InvalidStateJson()
        val value = source.substring(index, index + 4).toIntOrNull(16) ?: throw InvalidStateJson()
        index += 4
        return value.toChar()
    }

    private fun parseInteger(): String {
        val start = index
        consumeIf('-')
        if (index >= source.length) throw InvalidStateJson()

        if (source[index] == '0') {
            index++
            if (index < source.length && source[index].isDigit()) throw InvalidStateJson()
        } else {
            if (source[index] !in '1'..'9') throw InvalidStateJson()
            while (index < source.length && source[index].isDigit()) index++
        }
        return source.substring(start, index)
    }

    private fun consumeLiteral(literal: String) {
        if (!source.regionMatches(index, literal, 0, literal.length)) throw InvalidStateJson()
        index += literal.length
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index] in JSON_WHITESPACE) index++
    }

    private fun expect(expected: Char) {
        if (index >= source.length || source[index] != expected) throw InvalidStateJson()
        index++
    }

    private fun consumeIf(expected: Char): Boolean {
        if (index >= source.length || source[index] != expected) return false
        index++
        return true
    }

    companion object {
        private const val MAX_DEPTH = 6
        private const val MAX_OBJECT_FIELDS = 16
        private val JSON_WHITESPACE = charArrayOf(' ', '\t', '\n', '\r')
    }
}

private fun JsonValue.asObject(): JsonObject = this as? JsonObject ?: throw InvalidStateJson()
