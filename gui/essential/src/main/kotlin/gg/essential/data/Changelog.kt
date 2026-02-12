/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.'s Essential Mod repository and is protected
 * under copyright registration # TX0009138511. For the full license, see:
 * https://github.com/EssentialGG/Essential/blob/main/LICENSE
 *
 * You may not use, copy, reproduce, modify, sell, license, distribute,
 * commercialize, or otherwise exploit, or create derivative works based
 * upon, this file or any other in this repository, all of which is reserved by Essential.
 */
package gg.essential.data

import gg.essential.lib.gson.TypeAdapter
import gg.essential.lib.gson.annotations.JsonAdapter
import gg.essential.lib.gson.annotations.SerializedName
import gg.essential.lib.gson.stream.JsonReader
import gg.essential.lib.gson.stream.JsonWriter

data class Changelog(
    @SerializedName("created_at")
    val timestamp: Long,
    @SerializedName("changelog")
    val entries: List<Part>,
    val branches: List<String>?,
    val version: String,
    val id: String,
    val summary: String,
) {
    data class Part(
        @SerializedName("value")
        @JsonAdapter(ValueAdapter::class)
        val content: String,
        val platforms: List<String>?,
    ) {
        private class ValueAdapter : TypeAdapter<String>() {
            override fun write(out: JsonWriter, value: String?) {
                out.value(value)
            }

            // Elementa doesn't currently support fenced code blocks and inline code blocks look weird, so remove the backticks and syntax indicator
            override fun read(reader: JsonReader): String {
                return reader.nextString().replace(Regex("```.*?[\\s\\n\\r]"), "").replace("`", "")
            }

        }
    }
}
