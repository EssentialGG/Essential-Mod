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
package gg.essential.network.cosmetics.cape

import gg.essential.handlers.MojangSkinManager
import gg.essential.lib.gson.Gson
import gg.essential.lib.gson.TypeAdapter
import gg.essential.lib.gson.annotations.JsonAdapter
import gg.essential.lib.gson.annotations.SerializedName
import gg.essential.lib.gson.stream.JsonReader
import gg.essential.lib.gson.stream.JsonWriter
import gg.essential.util.APIException
import gg.essential.util.UUIDUtil
import gg.essential.util.httpClient
import net.minecraft.client.Minecraft
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException

object MojangCapeApi {
    private val JSON = MediaType.parse("application/json")
    private val URL_BASE = System.getProperty(
        "essential.mojang_profile_url",
        "https://api.minecraftservices.com/minecraft/profile",
    )

    fun fetchCurrentTextures(): Pair<String, String> = MojangSkinManager.getTextureProperty(UUIDUtil.getClientUUID())
        ?: throw IOException("Failed to fetch current texture property")

    fun fetchCapes(): List<Cape> {
        val accessToken = Minecraft.getMinecraft().session.token

        val request = Request.Builder().apply {
            url(URL_BASE)
            header("Authorization", "Bearer $accessToken")
        }.build()

        val responseStr = httpClient.join().newCall(request).execute().use { it.body()!!.string() }

        data class Response(val capes: List<Cape>)
        val response = Gson().fromJson(responseStr, Response::class.java)
        return response.capes
    }

    fun putCape(id: String?) {
        val accessToken = Minecraft.getMinecraft().session.token

        val request = Request.Builder().apply {
            url("$URL_BASE/capes/active")
            header("Authorization", "Bearer $accessToken")
            if (id != null) {
                data class Payload(val capeId: String)
                val payload = Gson().toJson(Payload(id))
                put(RequestBody.create(JSON, payload))
            } else {
                delete()
            }
        }.build()

        httpClient.join().newCall(request).execute().use { response ->
            if (response.code() != 200) {
                throw APIException(response.body()?.string() ?: "<null")
            }
        }
    }

    data class Cape(
        val id: String,
        @SerializedName("state")
        @JsonAdapter(StateAdapter::class)
        val active: Boolean,
        val url: String,
        @SerializedName("alias")
        val name: String,
    ) {
        val hash: String
            get() = url.split("/texture/")[1]

        private class StateAdapter : TypeAdapter<Boolean>() {
            override fun read(reader: JsonReader): Boolean {
                return reader.nextString() == "ACTIVE"
            }

            override fun write(out: JsonWriter, value: Boolean) {
                out.value(if (value) "ACTIVE" else "INACTIVE")
            }
        }
    }
}
