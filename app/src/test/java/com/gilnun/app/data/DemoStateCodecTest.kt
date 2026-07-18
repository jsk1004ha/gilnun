package com.gilnun.app.data

import com.gilnun.app.catalog.ServiceCatalog
import com.gilnun.app.catalog.ServiceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoStateCodecTest {
    private val verifiedReceipt =
        ActionReceipt(
            guidanceShown = true,
            userActionObserved = true,
            postconditionVerified = true,
            outcome = ReceiptOutcome.VERIFIED,
            source = GuidanceSource.SAME_DEVICE_HELPER,
        )

    @Test
    fun `version two state round trips service patches help and receipt source`() {
        val state =
            stateWith(
                ServiceId.BASIC_PENSION to
                    ServiceProgress(
                        helperPatchesByCheckpoint =
                            mapOf(
                                "pension-applicant" to
                                    builtInPatch(
                                        ServiceId.BASIC_PENSION,
                                        "pension-applicant",
                                    ),
                            ),
                        helpLevel = 1,
                        lastReceipt = verifiedReceipt,
                    ),
                ServiceId.RESIDENT_RECORD to
                    ServiceProgress(
                        helperPatchesByCheckpoint =
                            mapOf(
                                "resident-type" to
                                    builtInPatch(ServiceId.RESIDENT_RECORD, "resident-type"),
                                "resident-review" to
                                    builtInPatch(ServiceId.RESIDENT_RECORD, "resident-review"),
                            ),
                        helpLevel = 2,
                        lastReceipt = verifiedReceipt.copy(source = GuidanceSource.PREVERIFIED),
                    ),
                ServiceId.HEALTH_SCREENING to ServiceProgress(helpLevel = 0),
            )

        val encoded = DemoStateCodec.encode(state)

        assertTrue(encoded.contains("\"schemaVersion\":2"))
        assertTrue(encoded.contains("\"source\":\"SAME_DEVICE_HELPER\""))
        assertEquals(state, DemoStateCodec.decode(encoded))
    }

    @Test
    fun `default state contains exactly all three services at help level three`() {
        val state = DemoStateCodec.decode(null)

        assertEquals(ServiceId.entries.toSet(), state.services.keys)
        state.services.values.forEach { progress ->
            assertEquals(3, progress.helpLevel)
            assertTrue(progress.helperPatchesByCheckpoint.isEmpty())
            assertEquals(null, progress.lastReceipt)
        }
    }

    @Test
    fun `encoded state contains only reviewed durable guidance data`() {
        val encoded =
            DemoStateCodec.encode(
                stateWith(
                    ServiceId.BASIC_PENSION to
                        ServiceProgress(
                            helperPatchesByCheckpoint =
                                mapOf(
                                    "pension-review" to
                                        builtInPatch(ServiceId.BASIC_PENSION, "pension-review"),
                                ),
                            helpLevel = 2,
                            lastReceipt = verifiedReceipt,
                        ),
                ),
            )

        assertFalse(encoded.contains("monotonicMs"))
        assertFalse(encoded.contains("rawEvents"))
        assertFalse(encoded.contains("selectedService"))
        assertFalse(encoded.contains("formValue"))
        assertFalse(encoded.contains("domValue"))
        assertFalse(encoded.contains("tts", ignoreCase = true))
        assertFalse(encoded.contains("screenState"))
        assertFalse(encoded.contains("url", ignoreCase = true))
        assertFalse(encoded.contains('['))
    }

    @Test
    fun `valid version one state migrates help to every service and discards patch and receipt`() {
        val legacy =
            """
            {
              "schemaVersion":1,
              "patch":{
                "pageId":"welfare-basic-class",
                "compatibleRevision":"2026-07",
                "stableKey":"review-next",
                "role":"button",
                "accessibleName":"신청 내용 확인",
                "expectedState":"review-ready"
              },
              "helpLevel":2,
              "lastReceipt":{
                "guidanceShown":true,
                "userActionObserved":true,
                "postconditionVerified":true,
                "outcome":"VERIFIED"
              }
            }
            """.trimIndent()

        val result = DemoStateCodec.decodeWithMetadata(legacy)

        assertTrue(result.migratedFromV1)
        assertEquals(setOf(2), result.state.services.values.map(ServiceProgress::helpLevel).toSet())
        result.state.services.values.forEach { progress ->
            assertTrue(progress.helperPatchesByCheckpoint.isEmpty())
            assertEquals(null, progress.lastReceipt)
        }
    }

    @Test
    fun `decode metadata distinguishes migrated version one from version two and invalid data`() {
        val versionTwo = DemoStateCodec.decodeWithMetadata(DemoStateCodec.encode(DemoState()))
        val invalid = DemoStateCodec.decodeWithMetadata("""{"schemaVersion":1}""")

        assertFalse(versionTwo.migratedFromV1)
        assertFalse(invalid.migratedFromV1)
        assertEquals(DemoState(), invalid.state)
    }

    @Test
    fun `version one requires exact shape and an integer help level in range`() {
        val invalidStates =
            listOf(
                """{"schemaVersion":1,"patch":null,"helpLevel":4,"lastReceipt":null}""",
                """{"schemaVersion":1,"patch":null,"helpLevel":1.0,"lastReceipt":null}""",
                """{"schemaVersion":1,"patch":null,"helpLevel":1,"lastReceipt":null,"extra":true}""",
                """{"schemaVersion":1,"schemaVersion":1,"patch":null,"helpLevel":1,"lastReceipt":null}""",
            )

        invalidStates.forEach { encoded ->
            assertEquals(DemoState(), DemoStateCodec.decode(encoded))
            assertFalse(DemoStateCodec.decodeWithMetadata(encoded).migratedFromV1)
        }
    }

    @Test
    fun `version one validates discarded patch and receipt before retaining help`() {
        val validPatch =
            """
            {
              "pageId":"welfare-basic-class",
              "compatibleRevision":"2026-07",
              "stableKey":"review-next",
              "role":"button",
              "accessibleName":"신청 내용 확인",
              "expectedState":"review-ready"
            }
            """.trimIndent()
        val validReceipt =
            """
            {
              "guidanceShown":true,
              "userActionObserved":true,
              "postconditionVerified":true,
              "outcome":"VERIFIED"
            }
            """.trimIndent()
        val invalidStates =
            listOf(
                """{"schemaVersion":1,"patch":{},"helpLevel":1,"lastReceipt":null}""",
                """{"schemaVersion":1,"patch":${validPatch.dropLast(1)},"extra":true},"helpLevel":1,"lastReceipt":null}""",
                """{"schemaVersion":1,"patch":${validPatch.replace("\"stableKey\":\"review-next\"", "\"stableKey\":\"review-next\",\"stableKey\":\"review-next\"")},"helpLevel":1,"lastReceipt":null}""",
                """{"schemaVersion":1,"patch":${validPatch.replace("review-next", "")},"helpLevel":1,"lastReceipt":null}""",
                """{"schemaVersion":1,"patch":null,"helpLevel":1,"lastReceipt":{}}""",
                """{"schemaVersion":1,"patch":null,"helpLevel":1,"lastReceipt":${validReceipt.dropLast(1)},"extra":true}}""",
                """{"schemaVersion":1,"patch":null,"helpLevel":1,"lastReceipt":${validReceipt.replace("\"outcome\":\"VERIFIED\"", "\"outcome\":\"UNKNOWN\"")}}""",
                """{"schemaVersion":1,"patch":null,"helpLevel":1,"lastReceipt":${validReceipt.replace("\"postconditionVerified\":true", "\"postconditionVerified\":false")}}""",
            )

        invalidStates.forEach { encoded ->
            val result = DemoStateCodec.decodeWithMetadata(encoded)
            assertEquals(DemoState(), result.state)
            assertFalse(result.migratedFromV1)
        }
    }

    @Test
    fun `invalid known service resets only that service`() {
        val valid =
            DemoStateCodec.encode(
                stateWith(
                    ServiceId.BASIC_PENSION to ServiceProgress(helpLevel = 0),
                    ServiceId.RESIDENT_RECORD to ServiceProgress(helpLevel = 1),
                    ServiceId.HEALTH_SCREENING to ServiceProgress(helpLevel = 2),
                ),
            )
        val invalidBasic = valid.replace("\"helpLevel\":0", "\"helpLevel\":9")
        val decoded = DemoStateCodec.decode(invalidBasic)

        assertEquals(ServiceProgress(), decoded.services.getValue(ServiceId.BASIC_PENSION))
        assertEquals(1, decoded.services.getValue(ServiceId.RESIDENT_RECORD).helpLevel)
        assertEquals(2, decoded.services.getValue(ServiceId.HEALTH_SCREENING).helpLevel)
    }

    @Test
    fun `unknown and duplicate fields inside one service are isolated to that service`() {
        val valid =
            DemoStateCodec.encode(
                stateWith(
                    ServiceId.BASIC_PENSION to ServiceProgress(helpLevel = 0),
                    ServiceId.RESIDENT_RECORD to ServiceProgress(helpLevel = 1),
                    ServiceId.HEALTH_SCREENING to ServiceProgress(helpLevel = 2),
                ),
            )
        val corruptions =
            listOf(
                valid.replace("\"helpLevel\":0", "\"helpLevel\":0,\"extra\":true"),
                valid.replace("\"helpLevel\":0", "\"helpLevel\":0,\"helpLevel\":0"),
            )

        corruptions.forEach { encoded ->
            val decoded = DemoStateCodec.decode(encoded)
            assertEquals(ServiceProgress(), decoded.services.getValue(ServiceId.BASIC_PENSION))
            assertEquals(1, decoded.services.getValue(ServiceId.RESIDENT_RECORD).helpLevel)
            assertEquals(2, decoded.services.getValue(ServiceId.HEALTH_SCREENING).helpLevel)
        }
    }

    @Test
    fun `global schema syntax size and service key corruption recover to safe default`() {
        val valid = DemoStateCodec.encode(DemoState())
        val invalidStates =
            listOf(
                valid.replace("\"schemaVersion\":2", "\"schemaVersion\":99"),
                valid.replace(
                    "\"schemaVersion\":2",
                    "\"schemaVersion\":2,\"schemaVersion\":2",
                ),
                valid.dropLast(1) + ",\"extra\":true}",
                valid.replace("\"services\":{", "\"services\":{\"unknown-service\":{},"),
                valid.replace("\"resident-record\":", "\"basic-pension\":"),
                """{"schemaVersion":2,"services":[]}""",
                """{"schemaVersion":2,"services":{""",
                "x".repeat(DemoStateCodec.MAX_ENCODED_LENGTH + 1),
            )

        invalidStates.forEach { encoded ->
            assertEquals(DemoState(), DemoStateCodec.decode(encoded))
        }
    }

    @Test
    fun `cross service built in patch resets only the owning service slot`() {
        val basicPatch = builtInPatch(ServiceId.BASIC_PENSION, "pension-applicant")
        val residentPatch = builtInPatch(ServiceId.RESIDENT_RECORD, "resident-type")
        val valid =
            DemoStateCodec.encode(
                stateWith(
                    ServiceId.BASIC_PENSION to
                        ServiceProgress(
                            helperPatchesByCheckpoint =
                                mapOf("pension-applicant" to basicPatch),
                            helpLevel = 0,
                        ),
                    ServiceId.RESIDENT_RECORD to ServiceProgress(helpLevel = 1),
                ),
            )
        val crossService =
            valid
                .replace(basicPatch.pageId, residentPatch.pageId)
                .replace(basicPatch.stableKey, residentPatch.stableKey)
                .replace(basicPatch.accessibleName, residentPatch.accessibleName)
                .replace(basicPatch.expectedState, residentPatch.expectedState)

        val decoded = DemoStateCodec.decode(crossService)

        assertEquals(ServiceProgress(), decoded.services.getValue(ServiceId.BASIC_PENSION))
        assertEquals(1, decoded.services.getValue(ServiceId.RESIDENT_RECORD).helpLevel)
    }

    @Test
    fun `unknown and more than three checkpoint patches reset only that service`() {
        val patch = builtInPatch(ServiceId.BASIC_PENSION, "pension-applicant")
        val valid =
            DemoStateCodec.encode(
                stateWith(
                    ServiceId.BASIC_PENSION to
                        ServiceProgress(
                            helperPatchesByCheckpoint =
                                mapOf("pension-applicant" to patch),
                            helpLevel = 0,
                        ),
                    ServiceId.RESIDENT_RECORD to ServiceProgress(helpLevel = 1),
                ),
            )
        val unknownCheckpoint =
            valid.replace("\"pension-applicant\":", "\"unknown-checkpoint\":")
        val fourPatches =
            valid.replace(
                "\"pension-applicant\":${patchJson(patch)}",
                listOf("one", "two", "three", "four")
                    .joinToString(prefix = "", separator = ",") { checkpoint ->
                        "\"$checkpoint\":${patchJson(patch)}"
                    },
            )

        listOf(unknownCheckpoint, fourPatches).forEach { encoded ->
            val decoded = DemoStateCodec.decode(encoded)
            assertEquals(ServiceProgress(), decoded.services.getValue(ServiceId.BASIC_PENSION))
            assertEquals(1, decoded.services.getValue(ServiceId.RESIDENT_RECORD).helpLevel)
        }
    }

    @Test
    fun `contradictory receipt and unknown source reset only that service`() {
        val valid =
            DemoStateCodec.encode(
                stateWith(
                    ServiceId.BASIC_PENSION to
                        ServiceProgress(helpLevel = 0, lastReceipt = verifiedReceipt),
                    ServiceId.RESIDENT_RECORD to ServiceProgress(helpLevel = 1),
                ),
            )
        val corruptions =
            listOf(
                valid.replace(
                    "\"postconditionVerified\":true",
                    "\"postconditionVerified\":false",
                ),
                valid.replace("SAME_DEVICE_HELPER", "REMOTE_HELPER"),
            )

        corruptions.forEach { encoded ->
            val decoded = DemoStateCodec.decode(encoded)
            assertEquals(ServiceProgress(), decoded.services.getValue(ServiceId.BASIC_PENSION))
            assertEquals(1, decoded.services.getValue(ServiceId.RESIDENT_RECORD).helpLevel)
        }
    }

    @Test
    fun `invalid in memory service encodes as one safe service default`() {
        val state =
            stateWith(
                ServiceId.BASIC_PENSION to ServiceProgress(helpLevel = 99),
                ServiceId.RESIDENT_RECORD to ServiceProgress(helpLevel = 1),
            )

        val decoded = DemoStateCodec.decode(DemoStateCodec.encode(state))

        assertEquals(ServiceProgress(), decoded.services.getValue(ServiceId.BASIC_PENSION))
        assertEquals(1, decoded.services.getValue(ServiceId.RESIDENT_RECORD).helpLevel)
    }

    private fun builtInPatch(
        serviceId: ServiceId,
        checkpoint: String,
    ): PatchV1 = checkNotNull(ServiceCatalog.builtInPatch(serviceId, checkpoint))

    private fun stateWith(
        vararg updates: Pair<ServiceId, ServiceProgress>,
    ): DemoState {
        val services = DemoState().services.toMutableMap()
        updates.forEach { (serviceId, progress) -> services[serviceId] = progress }
        return DemoState(services = services)
    }

    private fun patchJson(patch: PatchV1): String =
        """
        {"pageId":"${patch.pageId}","compatibleRevision":"${patch.compatibleRevision}","stableKey":"${patch.stableKey}","role":"${patch.role}","accessibleName":"${patch.accessibleName}","expectedState":"${patch.expectedState}"}
        """.trimIndent()
}
