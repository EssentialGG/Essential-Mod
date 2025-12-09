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
package gg.essential.connectionmanager.common.util;

import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

public class LoginUtil {

    /** Taking the role of the Public Key from the normal handshake. Randomly generated (just needs to be unique). */
    private static final byte[] SHARED_CONSTANT = new BigInteger("173be201d4e5591dcef37bcaf701d136", 16).toByteArray();

    private static final int SHARED_SECRET_LENGTH = 16;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static byte[] sha1(byte[] source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return digest.digest(source);
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] generateSharedSecret() {
        byte[] bytes = new byte[SHARED_SECRET_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static String computeHash(byte @NotNull [] sharedSecret) {
        // buf = sharedSecret + SHARED_CONSTANT
        byte[] buf = Arrays.copyOf(sharedSecret, sharedSecret.length + SHARED_CONSTANT.length);
        System.arraycopy(SHARED_CONSTANT, 0, buf, sharedSecret.length, SHARED_CONSTANT.length);

        return new BigInteger(sha1(buf)).toString(16);
    }

}
