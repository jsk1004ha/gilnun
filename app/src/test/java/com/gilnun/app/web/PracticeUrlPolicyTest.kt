package com.gilnun.app.web

import com.gilnun.app.catalog.ServiceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeUrlPolicyTest {
    @Test
    fun `PatchV1 wire version remains one while Bridge events use V2`() {
        assertEquals(1, PATCH_SCHEMA_VERSION)
        assertEquals(2, GilnunBridge.SCHEMA_VERSION)
    }

    @Test
    fun `exact service and layout query is accepted`() {
        ServiceId.entries.forEach { serviceId ->
            PracticeLayout.entries.forEach { layout ->
                val url = PracticeUrlPolicy.pageUrl(serviceId, layout)

                assertEquals(
                    PracticePageRequest(serviceId, layout),
                    PracticeUrlPolicy.parseNavigation(url),
                )
                assertTrue(PracticeUrlPolicy.isAllowedResource(url))
            }
        }
    }

    @Test
    fun `origin path authority and fragment deviations are rejected`() {
        val valid = PracticeUrlPolicy.pageUrl(ServiceId.BASIC_PENSION, PracticeLayout.A)
        val invalid =
            listOf(
                valid.replace("https://", "http://"),
                valid.replace("appassets.androidplatform.net", "example.invalid"),
                valid.replace("appassets.androidplatform.net", "user@appassets.androidplatform.net"),
                valid.replace("appassets.androidplatform.net", "appassets.androidplatform.net:443"),
                valid.replace("/assets/welfare/index.html", "/assets/welfare/other.html"),
                valid.replace("/assets/welfare/index.html", "/assets/welfare/%69ndex.html"),
                "$valid#fragment",
            )

        invalid.forEach { assertNull("Unexpected accepted URL: $it", PracticeUrlPolicy.parseNavigation(it)) }
    }

    @Test
    fun `query extras duplicates omissions and encodings are rejected`() {
        val base = PracticeUrlPolicy.INDEX_URL
        val invalid =
            listOf(
                "$base?service=basic-pension",
                "$base?layout=A",
                "$base?service=basic-pension&layout=A&extra=1",
                "$base?service=basic-pension&service=basic-pension&layout=A",
                "$base?service=basic-pension&layout=A&layout=A",
                "$base?serviceId=basic-pension&layout=A",
                "$base?service=basic%2Dpension&layout=A",
                "$base?service=basic-pension&lay%6Fut=A",
                "$base?service=unknown&layout=A",
                "$base?service=basic-pension&layout=C",
                "$base?service=basic-pension;layout=A",
                "$base?service=basic-pension&layout=A&",
            )

        invalid.forEach { assertNull("Unexpected accepted URL: $it", PracticeUrlPolicy.parseNavigation(it)) }
    }

    @Test
    fun `only owned static resources without query are allowed`() {
        assertTrue(PracticeUrlPolicy.isAllowedResource("${PracticeUrlPolicy.APP_ORIGIN}/assets/welfare/style.css"))
        assertTrue(PracticeUrlPolicy.isAllowedResource("${PracticeUrlPolicy.APP_ORIGIN}/assets/welfare/app.js"))
        assertFalse(PracticeUrlPolicy.isAllowedResource("${PracticeUrlPolicy.APP_ORIGIN}/assets/welfare/style.css?x=1"))
        assertFalse(PracticeUrlPolicy.isAllowedResource("${PracticeUrlPolicy.APP_ORIGIN}/assets/welfare/logo.svg"))
        assertFalse(PracticeUrlPolicy.isAllowedResource("https://example.invalid/app.js"))
    }

    @Test
    fun `navigation must match the exact active service and layout`() {
        val expected = PracticePageRequest(ServiceId.BASIC_PENSION, PracticeLayout.A)

        assertTrue(
            PracticeUrlPolicy.isExpectedNavigation(
                PracticeUrlPolicy.pageUrl(ServiceId.BASIC_PENSION, PracticeLayout.A),
                expected,
            ),
        )
        assertFalse(
            PracticeUrlPolicy.isExpectedNavigation(
                PracticeUrlPolicy.pageUrl(ServiceId.RESIDENT_RECORD, PracticeLayout.A),
                expected,
            ),
        )
        assertFalse(
            PracticeUrlPolicy.isExpectedNavigation(
                PracticeUrlPolicy.pageUrl(ServiceId.BASIC_PENSION, PracticeLayout.B),
                expected,
            ),
        )
    }
}
