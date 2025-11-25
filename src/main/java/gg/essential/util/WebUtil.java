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

import gg.essential.Essential;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

@Deprecated // reason: HttpUtils should be used instead.
public class WebUtil {
    public static boolean LOG = false;

    public static JsonHolder fetchJSON(String url) {
        return new JsonHolder(fetchString(url));
    }

    public static String fetchString(String url) {
        url = url.replace(" ", "%20");
        if (LOG) Essential.logger.debug("Fetching {}", url);
        try (InputStream setup = setup(url, "Mozilla/4.76 (Essential)")) {
            return IOUtils.toString(setup, Charset.defaultCharset());
        } catch (Exception e) {
            Essential.logger.error("Failed to fetch from {}", url, e);
        }
        return "Failed to fetch";
    }

    public static void downloadToFile(String url, File file, String userAgent) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file);
             BufferedInputStream in = new BufferedInputStream(setup(url, userAgent))) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                outputStream.write(dataBuffer, 0, bytesRead);
            }
        }
    }

    public static InputStream setup(String url, String userAgent) throws IOException {
        URL u = new URL(url);
        URLConnection connection = u.openConnection();

        connection.setUseCaches(true);
        connection.addRequestProperty("User-Agent", userAgent);
        connection.setReadTimeout(15000);
        connection.setConnectTimeout(15000);
        connection.setDoOutput(true);

        return connection.getInputStream();
    }
}
