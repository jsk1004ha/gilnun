package com.gilnun.app.data

/**
 * Dependency-free, strict JSON codec for the minimal durable demo state.
 *
 * Decoding is fail closed: malformed JSON, duplicate/unknown fields, unknown schemas, invalid
 * semantic fields, out-of-range help, and contradictory receipts all recover to [DemoState].
 * Raw interaction events have no representation in this schema.
 */
object DemoStateCodec {
    const val SCHEMA_VERSION = 1
    const val MAX_ENCODED_LENGTH = 16_384

    fun encode(state: DemoState): String {
        val safeState = state.takeIf(::isValidState) ?: DemoState()
        return buildString {
            append('{')
            append("\"schemaVersion\":")
            append(SCHEMA_VERSION)
            append(",\"patch\":")
            appendPatch(safeState.patch)
            append(",\"helpLevel\":")
            append(safeState.helpLevel)
            append(",\"lastReceipt\":")
            appendReceipt(safeState.lastReceipt)
            append('}')
        }
    }

    fun decode(json: String?): DemoState = decodeOrDefault(json)

    fun decodeOrDefault(json: String?): DemoState {
        if (json.isNullOrBlank() || json.length > MAX_ENCODED_LENGTH) return DemoState()

        return runCatching {
            val root = JsonParser(json).parseRoot().asObject()
            root.requireOnlyKeys(TOP_LEVEL_KEYS)
            if (root.requiredInt("schemaVersion") != SCHEMA_VERSION) {
                throw InvalidStateJson()
            }

            val state =
                DemoState(
                    patch = root.optionalObject("patch")?.toPatch(),
                    helpLevel = root.optionalInt("helpLevel") ?: 0,
                    lastReceipt = root.optionalObject("lastReceipt")?.toReceipt(),
                )
            if (!isValidState(state)) throw InvalidStateJson()
            state
        }.getOrDefault(DemoState())
    }

    private fun isValidState(state: DemoState): Boolean =
        state.helpLevel in MIN_HELP_LEVEL..MAX_HELP_LEVEL &&
            (state.patch == null || state.patch.hasValidSemanticFields()) &&
            (state.lastReceipt == null || state.lastReceipt.isTruthful())

    private fun ActionReceipt.isTruthful(): Boolean {
        val fullyVerified = guidanceShown && userActionObserved && postconditionVerified
        return (outcome == ReceiptOutcome.VERIFIED) == fullyVerified
    }

    private fun JsonObject.toPatch(): PatchV1 {
        requireOnlyKeys(PATCH_KEYS)
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
        requireOnlyKeys(RECEIPT_KEYS)
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

    private fun StringBuilder.appendPatch(patch: PatchV1?) {
        if (patch == null) {
            append("null")
            return
        }
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

    private const val MIN_HELP_LEVEL = 0
    private const val MAX_HELP_LEVEL = 3

    private val TOP_LEVEL_KEYS =
        setOf("schemaVersion", "patch", "helpLevel", "lastReceipt")
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
        )
}

private sealed interface JsonValue

private data class JsonObject(
    val values: Map<String, JsonValue>,
) : JsonValue {
    fun requireOnlyKeys(allowed: Set<String>) {
        if (values.keys.any { it !in allowed }) throw InvalidStateJson()
    }

    fun requiredString(key: String): String =
        (values[key] as? JsonString)?.value ?: throw InvalidStateJson()

    fun requiredBoolean(key: String): Boolean =
        (values[key] as? JsonBoolean)?.value ?: throw InvalidStateJson()

    fun requiredInt(key: String): Int =
        (values[key] as? JsonNumber)?.value?.toIntOrNull() ?: throw InvalidStateJson()

    fun optionalInt(key: String): Int? =
        when (val value = values[key]) {
            null -> null
            is JsonNumber -> value.value.toIntOrNull() ?: throw InvalidStateJson()
            else -> throw InvalidStateJson()
        }

    fun optionalObject(key: String): JsonObject? =
        when (val value = values[key]) {
            null, JsonNull -> null
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
        while (true) {
            skipWhitespace()
            if (index >= source.length || source[index] != '"') throw InvalidStateJson()
            val key = parseString()
            if (key in values || values.size >= MAX_OBJECT_FIELDS) throw InvalidStateJson()
            skipWhitespace()
            expect(':')
            skipWhitespace()
            values[key] = parseValue(depth)
            skipWhitespace()
            if (consumeIf('}')) break
            expect(',')
        }
        return JsonObject(values)
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
        private const val MAX_DEPTH = 4
        private const val MAX_OBJECT_FIELDS = 16
        private val JSON_WHITESPACE = charArrayOf(' ', '\t', '\n', '\r')
    }
}

private fun JsonValue.asObject(): JsonObject = this as? JsonObject ?: throw InvalidStateJson()
