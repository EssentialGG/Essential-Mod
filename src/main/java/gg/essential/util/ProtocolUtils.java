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
package gg.essential.util;

public class ProtocolUtils {

    // https://en.wikipedia.org/wiki/Internet_Protocol_version_4#Header
    // The IPv4 header can vary in length, but the shortest version is most common
    public static final int IPV4_HEADER_SIZE = 20;

    // https://en.wikipedia.org/wiki/IPv6_packet#Fixed_header
    // IPv6 has a fixed header size, optionally followed by some extensions, which we'll ignore
    public static final int IPV6_HEADER_SIZE = 40;

    // https://en.wikipedia.org/wiki/User_Datagram_Protocol#UDP_datagram_structure
    public static final int UDP_HEADER_SIZE = 8;

}
