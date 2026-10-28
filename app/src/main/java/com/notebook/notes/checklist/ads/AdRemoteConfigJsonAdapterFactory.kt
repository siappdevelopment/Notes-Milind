package com.notebook.notes.checklist.ads

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type
import java.util.Locale

class AdRemoteConfigJsonAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: MutableSet<out Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) != AdRemoteConfig::class.java) {
            return null
        }
        val mapType: Type = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            AdUnitConfig::class.java
        )
        val mapAdapter: JsonAdapter<Map<String, AdUnitConfig>> =
            moshi.adapter<Map<String, AdUnitConfig>>(mapType).lenient()
        return object : JsonAdapter<AdRemoteConfig>() {
            override fun fromJson(reader: JsonReader): AdRemoteConfig {
                reader.isLenient = true
                if (reader.peek() == JsonReader.Token.NULL) {
                    reader.nextNull<Unit>()
                    return AdRemoteConfig()
                }
                val adsMap = mutableMapOf<String, AdUnitConfig>()
                reader.beginObject()
                while (reader.hasNext()) {
                    val key = reader.nextName()
                    // Flow settings such as `onboarding_repeat_count` sit beside the ad units as plain
                    // values; only objects are ad units, so skip the rest instead of failing the parse.
                    if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                        adsMap[key] = readAdUnitConfig(reader)
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
                return AdRemoteConfig(ads = adsMap)
            }

            override fun toJson(writer: JsonWriter, value: AdRemoteConfig?) {
                if (value == null) {
                    writer.nullValue()
                    return
                }
                mapAdapter.toJson(writer, value.ads)
            }

            private fun readAdUnitConfig(reader: JsonReader): AdUnitConfig {
                var id = ""
                var isEnable = false
                var enableUaCheck = false
                var reloadIntervalSeconds: Int? = null
                var colorCTA = "default"
                var heightCTA = 45
                var positionCTA = "BOTTOM"
                var components: List<String> = listOf("icon_headline", "body", "media", "cta")
                var clickCount = 1
                var type = "native"
                var bannerId = ""
                var nativeId = ""
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "id" -> id = reader.nextString()
                        "isEnable" -> isEnable = reader.nextBoolean()
                        "enable_ua_check" -> enableUaCheck = reader.nextBoolean()
                        "reloadIntervalSeconds" -> reloadIntervalSeconds = reader.nextInt()
                        "colorCTA" -> colorCTA = safeNextString(reader, "default")
                        "heightCTA" -> heightCTA = readHeightValue(reader)
                        "positionCTA" -> positionCTA = safeNextString(reader, "BOTTOM").uppercase(Locale.US)
                        "components" -> components = readComponentsList(reader)
                        "clickCount" -> clickCount = readHeightValue(reader).coerceAtLeast(1)
                        "type" -> type = safeNextString(reader, "native").lowercase(Locale.US)
                        "bannerId" -> bannerId = safeNextString(reader, "")
                        "nativeId" -> nativeId = safeNextString(reader, "")
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                return AdUnitConfig(
                    id = id,
                    isEnable = isEnable,
                    enableUaCheck = enableUaCheck,
                    reloadIntervalSeconds = reloadIntervalSeconds,
                    colorCTA = colorCTA,
                    heightCTA = heightCTA,
                    positionCTA = positionCTA,
                    components = components,
                    clickCount = clickCount,
                    type = type,
                    bannerId = bannerId,
                    nativeId = nativeId
                )
            }

            private fun readComponentsList(reader: JsonReader): List<String> {
                val list = mutableListOf<String>()
                if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                    reader.beginArray()
                    while (reader.hasNext()) {
                        if (reader.peek() == JsonReader.Token.STRING) {
                            list.add(reader.nextString())
                        } else {
                            reader.skipValue()
                        }
                    }
                    reader.endArray()
                } else {
                    reader.skipValue()
                }
                return list.ifEmpty { listOf("icon_headline", "body", "media", "cta") }
            }

            private fun readHeightValue(reader: JsonReader): Int {
                return when (reader.peek()) {
                    JsonReader.Token.NUMBER -> reader.nextInt()
                    JsonReader.Token.STRING -> {
                        val value = reader.nextString()
                        if (value.equals("default", ignoreCase = true)) 40 else value.toIntOrNull() ?: 40
                    }
                    JsonReader.Token.NULL -> {
                        reader.nextNull<Unit>()
                        40
                    }
                    else -> {
                        reader.skipValue()
                        40
                    }
                }
            }

            private fun safeNextString(reader: JsonReader, fallback: String): String {
                return when (reader.peek()) {
                    JsonReader.Token.STRING -> reader.nextString()
                    JsonReader.Token.NULL -> {
                        reader.nextNull<Unit>()
                        fallback
                    }
                    else -> {
                        reader.skipValue()
                        fallback
                    }
                }
            }
        }
    }
}
