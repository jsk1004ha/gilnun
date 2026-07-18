package com.gilnun.app.web

import com.gilnun.app.catalog.CheckpointContract
import com.gilnun.app.catalog.EventEffect
import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceContract
import com.gilnun.app.catalog.ServiceEventType
import com.gilnun.app.catalog.ServiceId

sealed interface BridgeEventV2 {
    val schemaVersion: Int
    val serviceId: ServiceId
    val revision: String
    val checkpoint: String

    data class ActionOrHelp(
        override val schemaVersion: Int,
        val type: ServiceEventType,
        override val serviceId: ServiceId,
        override val revision: String,
        override val checkpoint: String,
        val stableKey: String,
        val role: String,
        val accessibleName: String,
        val effect: EventEffect,
    ) : BridgeEventV2

    data class CheckpointChanged(
        override val schemaVersion: Int,
        override val serviceId: ServiceId,
        override val revision: String,
        override val checkpoint: String,
    ) : BridgeEventV2
}

enum class BridgeEventV2Error {
    INVALID_PAYLOAD,
    PAYLOAD_TOO_LARGE,
    INVALID_SCHEMA,
    INVALID_EVENT,
    INVALID_CONTRACT,
}

sealed interface BridgeEventV2Result {
    data class Accepted(
        val event: BridgeEventV2,
    ) : BridgeEventV2Result

    data class Rejected(
        val error: BridgeEventV2Error,
    ) : BridgeEventV2Result
}

/**
 * Strict, dependency-free parser for Bridge V2.
 *
 * It accepts only catalog-owned semantic identifiers. There is no field for free-form help text,
 * form data, selectors, coordinates, URLs, or executable content.
 */
object BridgeEventV2Parser {
    const val SCHEMA_VERSION = 2
    const val MAX_PAYLOAD_BYTES = 4_096
    const val MAX_STRING_LENGTH = 256

    private val interactionFields =
        setOf(
            "schemaVersion",
            "type",
            "serviceId",
            "revision",
            "checkpoint",
            "stableKey",
            "role",
            "accessibleName",
            "effect",
        )
    private val checkpointFields =
        setOf(
            "schemaVersion",
            "type",
            "serviceId",
            "revision",
            "checkpoint",
        )

    fun parse(payload: String?): BridgeEventV2Result {
        if (payload == null) return rejected(BridgeEventV2Error.INVALID_PAYLOAD)
        if (payload.length > MAX_PAYLOAD_BYTES ||
            payload.toByteArray(Charsets.UTF_8).size > MAX_PAYLOAD_BYTES
        ) {
            return rejected(BridgeEventV2Error.PAYLOAD_TOO_LARGE)
        }

        val fields =
            try {
                StrictFlatJsonParser(payload, MAX_STRING_LENGTH).parse()
            } catch (_: InvalidBridgeJson) {
                return rejected(BridgeEventV2Error.INVALID_PAYLOAD)
            }

        val type = fields.string("type")
            ?: return rejected(BridgeEventV2Error.INVALID_SCHEMA)
        return when (type) {
            ServiceEventType.ACTION.name,
            ServiceEventType.HELP.name,
            -> parseActionOrHelp(fields, type)

            CHECKPOINT_CHANGED -> parseCheckpointChanged(fields)
            else -> rejected(BridgeEventV2Error.INVALID_EVENT)
        }
    }

    private fun parseActionOrHelp(
        fields: Map<String, FlatJsonValue>,
        rawType: String,
    ): BridgeEventV2Result {
        if (fields.keys != interactionFields) {
            return rejected(BridgeEventV2Error.INVALID_SCHEMA)
        }
        val schemaVersion = fields.schemaVersion()
            ?: return rejected(BridgeEventV2Error.INVALID_SCHEMA)
        val common = fields.validCommonContract()
            ?: return rejected(BridgeEventV2Error.INVALID_CONTRACT)
        val stableKey = fields.semanticString("stableKey")
            ?: return rejected(BridgeEventV2Error.INVALID_CONTRACT)
        val role = fields.semanticString("role")
            ?: return rejected(BridgeEventV2Error.INVALID_CONTRACT)
        val accessibleName = fields.semanticString("accessibleName")
            ?: return rejected(BridgeEventV2Error.INVALID_CONTRACT)
        val effect =
            fields.semanticString("effect")
                ?.let { runCatching { EventEffect.valueOf(it) }.getOrNull() }
                ?: return rejected(BridgeEventV2Error.INVALID_CONTRACT)
        val type = ServiceEventType.valueOf(rawType)
        val contract =
            common.checkpoint.events.singleOrNull { event -> event.stableKey == stableKey }
                ?: return rejected(BridgeEventV2Error.INVALID_CONTRACT)

        if (contract.type != type ||
            contract.role != role ||
            contract.accessibleName != accessibleName ||
            contract.effect != effect
        ) {
            return rejected(BridgeEventV2Error.INVALID_CONTRACT)
        }

        return BridgeEventV2Result.Accepted(
            BridgeEventV2.ActionOrHelp(
                schemaVersion = schemaVersion,
                type = type,
                serviceId = common.service.id,
                revision = common.service.revision,
                checkpoint = common.checkpoint.id,
                stableKey = contract.stableKey,
                role = contract.role,
                accessibleName = contract.accessibleName,
                effect = contract.effect,
            ),
        )
    }

    private fun parseCheckpointChanged(
        fields: Map<String, FlatJsonValue>,
    ): BridgeEventV2Result {
        if (fields.keys != checkpointFields) {
            return rejected(BridgeEventV2Error.INVALID_SCHEMA)
        }
        val schemaVersion = fields.schemaVersion()
            ?: return rejected(BridgeEventV2Error.INVALID_SCHEMA)
        val common = fields.validCommonContract()
            ?: return rejected(BridgeEventV2Error.INVALID_CONTRACT)

        return BridgeEventV2Result.Accepted(
            BridgeEventV2.CheckpointChanged(
                schemaVersion = schemaVersion,
                serviceId = common.service.id,
                revision = common.service.revision,
                checkpoint = common.checkpoint.id,
            ),
        )
    }

    private fun Map<String, FlatJsonValue>.schemaVersion(): Int? =
        integer("schemaVersion")?.takeIf { it == SCHEMA_VERSION }

    private fun Map<String, FlatJsonValue>.validCommonContract(): CommonContract? {
        val serviceId =
            ServiceId.fromPersistedKey(semanticString("serviceId"))
                ?: return null
        val revision = semanticString("revision") ?: return null
        val checkpointId = semanticString("checkpoint") ?: return null
        val service = ServiceCatalog.find(serviceId) ?: return null
        if (revision != service.revision) return null
        val checkpoint = service.checkpoint(checkpointId) ?: return null
        return CommonContract(service, checkpoint)
    }

    private fun Map<String, FlatJsonValue>.semanticString(key: String): String? =
        string(key)?.takeIf { value ->
            value.isNotBlank() &&
                value.length <= MAX_STRING_LENGTH &&
                value.none { character ->
                    character.isISOControl() || character.isSurrogate()
                }
        }

    private fun Map<String, FlatJsonValue>.string(key: String): String? =
        (get(key) as? FlatJsonString)?.value

    private fun Map<String, FlatJsonValue>.integer(key: String): Int? =
        (get(key) as? FlatJsonInteger)?.value?.toIntOrNull()

    private fun rejected(error: BridgeEventV2Error): BridgeEventV2Result =
        BridgeEventV2Result.Rejected(error)

    private data class CommonContract(
        val service: ServiceContract,
        val checkpoint: CheckpointContract,
    )

    private const val CHECKPOINT_CHANGED = "CHECKPOINT_CHANGED"
}

private sealed interface FlatJsonValue

private data class FlatJsonString(
    val value: String,
) : FlatJsonValue

private data class FlatJsonInteger(
    val value: String,
) : FlatJsonValue

private class InvalidBridgeJson : RuntimeException()

/**
 * The bridge schema is a flat object, so accepting arrays, nested objects, booleans, nulls, or
 * decimal numbers would only create unneeded parser surface.
 */
private class StrictFlatJsonParser(
    private val source: String,
    private val maximumStringLength: Int,
) {
    private var index = 0

    fun parse(): Map<String, FlatJsonValue> {
        skipWhitespace()
        expect('{')
        skipWhitespace()
        if (consumeIf('}')) return finish(emptyMap())

        val fields = linkedMapOf<String, FlatJsonValue>()
        while (true) {
            skipWhitespace()
            val key = parseString()
            if (key in fields || fields.size >= MAX_FIELDS) throw InvalidBridgeJson()
            skipWhitespace()
            expect(':')
            skipWhitespace()
            fields[key] = parseValue()
            skipWhitespace()
            if (consumeIf('}')) return finish(fields)
            expect(',')
        }
    }

    private fun parseValue(): FlatJsonValue {
        if (index >= source.length) throw InvalidBridgeJson()
        return when (source[index]) {
            '"' -> FlatJsonString(parseString())
            '-', in '0'..'9' -> FlatJsonInteger(parseInteger())
            else -> throw InvalidBridgeJson()
        }
    }

    private fun parseString(): String {
        expect('"')
        val result = StringBuilder()
        while (index < source.length) {
            val character = source[index++]
            when {
                character == '"' -> return result.toString()
                character == '\\' -> result.append(parseEscape())
                character.code < 0x20 -> throw InvalidBridgeJson()
                else -> result.append(character)
            }
            if (result.length > maximumStringLength) throw InvalidBridgeJson()
        }
        throw InvalidBridgeJson()
    }

    private fun parseEscape(): Char {
        if (index >= source.length) throw InvalidBridgeJson()
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
            else -> throw InvalidBridgeJson()
        }
    }

    private fun parseUnicodeEscape(): Char {
        if (index + 4 > source.length) throw InvalidBridgeJson()
        val value = source.substring(index, index + 4).toIntOrNull(16) ?: throw InvalidBridgeJson()
        index += 4
        return value.toChar()
    }

    private fun parseInteger(): String {
        val start = index
        consumeIf('-')
        if (index >= source.length) throw InvalidBridgeJson()

        if (source[index] == '0') {
            index++
            if (index < source.length && source[index].isDigit()) throw InvalidBridgeJson()
        } else {
            if (source[index] !in '1'..'9') throw InvalidBridgeJson()
            while (index < source.length && source[index].isDigit()) index++
        }
        return source.substring(start, index)
    }

    private fun finish(fields: Map<String, FlatJsonValue>): Map<String, FlatJsonValue> {
        skipWhitespace()
        if (index != source.length) throw InvalidBridgeJson()
        return fields
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index] in JSON_WHITESPACE) index++
    }

    private fun expect(expected: Char) {
        if (index >= source.length || source[index] != expected) throw InvalidBridgeJson()
        index++
    }

    private fun consumeIf(expected: Char): Boolean {
        if (index >= source.length || source[index] != expected) return false
        index++
        return true
    }

    companion object {
        private const val MAX_FIELDS = 9
        private val JSON_WHITESPACE = charArrayOf(' ', '\t', '\n', '\r')
    }
}
