package com.notebook.notes.checklist.ads

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdRemoteConfigJsonAdapterFactoryTest {

    private val adapter = Moshi.Builder()
        .add(AdRemoteConfigJsonAdapterFactory())
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter(AdRemoteConfig::class.java)

    @Test
    fun `plain top-level values beside ad units are skipped and ad units still parse`() {
        val json = """
            {
              "onboarding_repeat_count": 2,
              "screen_flow": ["LANGUAGE", "HOME"],
              "show_paywall_after_onboarding": true,
              "native_home": { "id": "home-id", "isEnable": true }
            }
        """.trimIndent()

        val config = adapter.fromJson(json)!!

        assertEquals(setOf("native_home"), config.ads.keys)
        assertEquals("home-id", config.ads.getValue("native_home").id)
        assertTrue(config.ads.getValue("native_home").isEnable)
    }

    @Test
    fun `callend_screen block does not break parsing of other ad units`() {
        val json = """
            {
              "callend_screen": {
                "is_call_end_show": true,
                "notification_country": [],
                "call_end_bacK_inter": { "callend_again_open_count": 2 }
              },
              "inter_splash": { "id": "splash-id", "isEnable": false }
            }
        """.trimIndent()

        val config = adapter.fromJson(json)!!

        assertEquals("splash-id", config.ads.getValue("inter_splash").id)
        assertFalse(config.ads.getValue("inter_splash").isEnable)
    }
}
