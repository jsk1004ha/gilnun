package com.gilnun.app.web

import com.gilnun.app.catalog.CheckpointContract
import com.gilnun.app.catalog.EventContract
import com.gilnun.app.catalog.EventEffect
import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BridgeEventV2Test {
    @Test
    fun `all fixed ACTION and HELP contracts parse only at their checkpoint`() {
        ServiceCatalog.services.forEach { service ->
            service.route.forEach { checkpoint ->
                checkpoint.events.forEach { contract ->
                    val accepted = BridgeEventV2Parser.parse(interactionPayload(service, checkpoint, contract))
                    val event =
                        (accepted as BridgeEventV2Result.Accepted).event as
                            BridgeEventV2.ActionOrHelp

                    assertEquals(contract.type, event.type)
                    assertEquals(contract.effect, event.effect)
                    assertEquals(contract.stableKey, event.stableKey)
                    assertEquals(checkpoint.id, event.checkpoint)
                }
            }
        }
    }

    @Test
    fun `CHECKPOINT_CHANGED accepts every catalog checkpoint with its exact small shape`() {
        ServiceCatalog.services.forEach { service ->
            service.route.forEach { checkpoint ->
                val result =
                    BridgeEventV2Parser.parse(
                        checkpointPayload(service, checkpoint.id),
                    )
                val event =
                    (result as BridgeEventV2Result.Accepted).event as
                        BridgeEventV2.CheckpointChanged

                assertEquals(2, event.schemaVersion)
                assertEquals(service.id, event.serviceId)
                assertEquals(service.revision, event.revision)
                assertEquals(checkpoint.id, event.checkpoint)
            }
        }
    }

    @Test
    fun `unknown missing and duplicate fields are rejected`() {
        val valid = validProgressPayload()

        assertRejected(valid.replaceFirst("{", """{"unknown":"value","""))
        assertRejected(valid.replace(",\"effect\":\"PROGRESS\"", ""))
        assertRejected(
            valid.replace(
                """"role":"button"""",
                """"role":"button","role":"button"""",
            ),
        )
        assertRejected(
            valid.replace(
                """"type":"ACTION"""",
                """"t\u0079pe":"ACTION","type":"ACTION"""",
            ),
        )
    }

    @Test
    fun `wrong schema service revision checkpoint target name role and effect are rejected`() {
        val valid = validProgressPayload()
        val wrongValues =
            listOf(
                valid.replace(""""schemaVersion":2""", """"schemaVersion":1"""),
                valid.replace("basic-pension", "unknown-service"),
                valid.replace("2026-07", "2026-08"),
                valid.replace("pension-service", "unknown-checkpoint"),
                valid.replace("pension-service-select", "pension-self-apply"),
                valid.replace("기초연금 신청 연습", "다른 이름"),
                valid.replace(""""role":"button"""", """"role":"link""""),
                valid.replace(""""effect":"PROGRESS"""", """"effect":"NON_PROGRESS""""),
                valid.replace(""""type":"ACTION"""", """"type":"HELP""""),
            )

        wrongValues.forEach(::assertRejected)
    }

    @Test
    fun `cross-service values and action at another checkpoint are rejected`() {
        val pension = ServiceCatalog.services.first()
        val resident = ServiceCatalog.services[1]
        val pensionAction = pension.steps.first().primaryAction!!
        val residentAction = resident.steps.first().primaryAction!!

        assertRejected(
            interactionPayload(
                service = pension,
                checkpoint = pension.steps.first(),
                event = residentAction,
            ),
        )
        assertRejected(
            interactionPayload(
                service = pension,
                checkpoint = pension.steps[1],
                event = pensionAction,
            ),
        )
        assertRejected(
            checkpointPayload(pension, resident.steps.first().id),
        )
    }

    @Test
    fun `HELP is fixed to its catalog contract and cannot carry arbitrary text`() {
        val resident = ServiceCatalog.services[1]
        val checkpoint = resident.requireCheckpoint("resident-delivery")
        val help = checkpoint.events.single { it.type.name == "HELP" }
        val valid = interactionPayload(resident, checkpoint, help)

        assertTrue(BridgeEventV2Parser.parse(valid) is BridgeEventV2Result.Accepted)
        assertRejected(valid.replace("수령 방법 안내", "원하는 내용을 자유롭게 입력"))
        assertRejected(valid.replaceFirst("{", """{"text":"자유 입력","""))
        assertRejected(valid.replace(""""type":"HELP"""", """"type":"ACTION""""))
    }

    @Test
    fun `oversized strings and UTF-8 payloads are rejected before contract matching`() {
        val valid = validProgressPayload()
        val oversizedName =
            valid.replace(
                "기초연금 신청 연습",
                "가".repeat(BridgeEventV2Parser.MAX_STRING_LENGTH + 1),
            )
        val oversizedPayload =
            """{"value":"${"가".repeat(BridgeEventV2Parser.MAX_PAYLOAD_BYTES)}"}"""

        assertRejected(oversizedName)
        assertEquals(
            BridgeEventV2Result.Rejected(BridgeEventV2Error.PAYLOAD_TOO_LARGE),
            BridgeEventV2Parser.parse(oversizedPayload),
        )
    }

    @Test
    fun `wrong JSON value types event variants and checkpoint shape are rejected`() {
        val valid = validProgressPayload()
        val pension = ServiceCatalog.services.first()
        val checkpointChanged = checkpointPayload(pension, pension.route.first().id)

        assertRejected(valid.replace(""""schemaVersion":2""", """"schemaVersion":"2""""))
        assertRejected(valid.replace(""""accessibleName":"기초연금 신청 연습"""", """"accessibleName":true"""))
        assertRejected(valid.replace(""""type":"ACTION"""", """"type":"CLICK""""))
        assertRejected("[]")
        assertRejected(checkpointChanged.replaceFirst("{", """{"effect":"PROGRESS","""))
        assertRejected(checkpointChanged.replace(""""revision":"2026-07",""", ""))
    }

    private fun validProgressPayload(): String {
        val service = ServiceCatalog.services.first()
        val checkpoint = service.steps.first()
        return interactionPayload(service, checkpoint, checkpoint.primaryAction!!)
    }

    private fun interactionPayload(
        service: ServiceContract,
        checkpoint: CheckpointContract,
        event: EventContract,
    ): String =
        """{"schemaVersion":2,"type":"${event.type.name}","serviceId":"${service.id.persistedKey}","revision":"${service.revision}","checkpoint":"${checkpoint.id}","stableKey":"${event.stableKey}","role":"${event.role}","accessibleName":"${event.accessibleName}","effect":"${event.effect.name}"}"""

    private fun checkpointPayload(
        service: ServiceContract,
        checkpoint: String,
    ): String =
        """{"schemaVersion":2,"type":"CHECKPOINT_CHANGED","serviceId":"${service.id.persistedKey}","revision":"${service.revision}","checkpoint":"$checkpoint"}"""

    private fun assertRejected(payload: String) {
        assertTrue(
            "Expected payload rejection: $payload",
            BridgeEventV2Parser.parse(payload) is BridgeEventV2Result.Rejected,
        )
    }
}
