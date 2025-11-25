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
package gg.essential.sps

/**
 * We give each player a dedicated, fake SPS server address in the form of `UUID.TLD`.
 * This value defines the `.TLD` part. The `UUID` is just the player's UUID (with dashes).
 * The address is only resolved to the real SPS session IP+port (or ICE) when opening the actual TCP connection.
 * <p>
 * This allows us to mask the real address in their activity info as well as keep it consistent
 * even if the IP changes which is very useful for mods which store data per server (e.g. minimap).
 * The consistency in the activity info also allows us to easily identify and filter friends from the multiplayer
 * menu who are playing in a SPS session so we do not show the server twice / at all if we are not invited (i.e.
 * it is easy to identify if they are playing via SPS vs regular servers).
 */
const val SPS_TLD = ".essential-sps"

