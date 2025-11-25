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

import gg.essential.Essential
import gg.essential.gui.about.Category
import gg.essential.gui.about.components.ChangelogComponent
import gg.essential.lib.caffeine.cache.AsyncLoadingCache
import gg.essential.lib.caffeine.cache.Caffeine
import gg.essential.lib.gson.Gson
import gg.essential.util.httpGetToStringBlocking
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object MenuData {

    internal val BASE_URL = System.getProperty(
        "essential.api.url",
        System.getenv().getOrDefault("ESSENTIAL_API_URL", "https://api.essential.gg")
    )

    val CHANGELOGS: AsyncLoadingCache<String, Pair<String?, ChangelogComponent.Changelog>> = Caffeine.newBuilder()
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .buildAsync { version -> fetchChangelogs(version) }

    val INFO: AsyncLoadingCache<FetchableCategory, String> = Caffeine.newBuilder()
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .buildAsync { category -> fetchInfo(category) }

    // Fetch five changelogs before provided version and populate CHANGELOGS cache, then return current changelog
    private fun fetchChangelogs(version: String): Pair<String?, ChangelogComponent.Changelog> {
        val encodedVersion = URLEncoder.encode(version, StandardCharsets.UTF_8.toString()).replace("+", "%20").replace("#", "%23")
        val dataString = httpGetToStringBlocking("$BASE_URL/mods/v1/essential:essential/changelogs?branch=${VersionData.essentialBranch}&before=$encodedVersion")
        val data = Gson().fromJson(dataString, Array<ChangelogComponent.Changelog>::class.java).toList()

        var nextVersion: String? = null
        var previousVersion = version
        var previousLog: ChangelogComponent.Changelog? = null

        for (log in data) {
            if (previousLog != null) {
                CHANGELOGS.put(previousVersion, CompletableFuture.completedFuture(Pair(log.version, previousLog)))
            } else {
                nextVersion = log.version
            }
            previousLog = log
            previousVersion = log.version
        }

        val versionResponse = httpGetToStringBlocking("$BASE_URL/mods/v1/essential:essential/versions/$encodedVersion/changelog")
        return Pair(nextVersion, Gson().fromJson(versionResponse, ChangelogComponent.Changelog::class.java))
    }

    private fun fetchInfo(category: FetchableCategory): String {

        var url = "$BASE_URL/v2/documents/"
        url += category.query

        var data: String
        try {
            data = httpGetToStringBlocking(url)
        } catch (e: IOException) {
            INFO.asMap().remove(category)
            Essential.logger.error("Failed to fetch menu data", e)
            data = "An error occurred fetching this data. Please check your internet connection and try again."
        }

        // Remove custom html placeholders
        data = data.replace(Regex("===.*"), "").replace(Regex("\\{\\..*?}"), "")

        return data
    }

    enum class FetchableCategory(val category: Category, val query: String) {
        PRIVACY(Category.PRIVACY, "privacy-policy");
    }
}
