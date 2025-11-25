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
package gg.essential.util.crash;

import gg.essential.Essential;
import gg.essential.util.HttpUtils;
import gg.essential.util.Multithreading;
import kotlin.Unit;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Taken from VanillaFix, as it's MIT and allows us to
 * https://github.com/DimensionalDevelopment/VanillaFix/blob/master/src/main/java/org/dimdev/vanillafix/crashes/StacktraceDeobfuscator.java
 */
public class StacktraceDeobfuscator {

    private static final boolean DEBUG_IN_DEV = false; // Makes this MCP -> SRG for testing in dev. Don't forget to set to false when done!

    public static final String MAPPINGS_VERSION =
            //#if MC==10809
            //$$ "22-1.8.9";
            //#else
            "39-1.12";
            //#endif

    private static final String mappingsLink =
            "https://maven.minecraftforge.net/de/oceanlabs/mcp/mcp_stable_nodoc/" + MAPPINGS_VERSION + "/mcp_stable_nodoc-" + MAPPINGS_VERSION + ".zip";

    private static CompletableFuture<StacktraceDeobfuscator> instanceFuture;

    private final Map<String, String> srgMcpMethodMap;

    public StacktraceDeobfuscator(Map<String, String> srgMcpMethodMap) {
        this.srgMcpMethodMap = srgMcpMethodMap;
    }

    public static void setup(File file) {
        if (instanceFuture != null) {
            return;
        }

        // First try to load from local cache
        instanceFuture = fromFile(file).exceptionally(throwable -> {
            // Failed to load from the file, if the file exist then we should log this
            if (file.exists()) {
                Essential.logger.error("Failed to read mappings from " + file, throwable);
                // and clean it up
                FileUtils.deleteQuietly(file);
            }
            return null;
        }).thenComposeAsync(result -> {
            // If we succeeded in loading from the local cache, we're good to go
            if (result != null) {
                return CompletableFuture.completedFuture(result);
            }

            // File missing or corrupted, (re-)download it
            downloadToFile(file);

            // and then try loading once more.
            return fromFile(file);
        }, Multithreading.POOL).exceptionally(throwable -> {
            Essential.logger.error("Failed to download mappings from " + mappingsLink, throwable);
            return null; // we tried but we just can't get it
        });
    }

    private static void downloadToFile(File file) {
        try {
            Path zipFile = Files.createTempFile("mappings", ".zip");

            HttpUtils.httpGetToFileBlocking(mappingsLink, zipFile, builder -> {
                builder.addHeader("User-Agent", "Mozilla/4.76 (Essential)");
                return Unit.INSTANCE;
            });

            try (FileSystem zipFileSystem = FileSystems.newFileSystem(zipFile, (ClassLoader) null)) {
                Files.copy(zipFileSystem.getPath("methods.csv"), file.toPath());
            }
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private static CompletableFuture<StacktraceDeobfuscator> fromFile(File file) {
        return CompletableFuture.supplyAsync(() -> readFromFile(file), Multithreading.POOL);
    }

    private static StacktraceDeobfuscator readFromFile(File file) {
        Map<String, String> toMerge = new HashMap<>();
        try (Scanner scanner = new Scanner(file)) {
            scanner.nextLine(); // Skip CSV header
            while (scanner.hasNext()) {
                String mappingLine = scanner.nextLine();
                int commaIndex = mappingLine.indexOf(',');
                String srgName = mappingLine.substring(0, commaIndex);
                String mcpName = mappingLine.substring(commaIndex + 1, commaIndex + 1 + mappingLine.substring(commaIndex + 1).indexOf(','));
                if (!DEBUG_IN_DEV) {
                    toMerge.put(srgName, mcpName);
                } else {
                    toMerge.put(mcpName, srgName);
                }
            }
        } catch (IOException e) {
            throw new CompletionException(e);
        }
        return new StacktraceDeobfuscator(toMerge);
    }

    public static StacktraceDeobfuscator get() {
        if (instanceFuture == null) {
            return null;
        }
        try {
            return instanceFuture.get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ignore) {
            return null;
        }
    }

    public void deobfuscateThrowable(Throwable t) {
        Deque<Throwable> queue = new ArrayDeque<>();
        queue.add(t);
        while (!queue.isEmpty()) {
            t = queue.remove();
            t.setStackTrace(deobfuscateStacktrace(t.getStackTrace()));
            if (t.getCause() != null) queue.add(t.getCause());
            Collections.addAll(queue, t.getSuppressed());
        }
    }

    public StackTraceElement[] deobfuscateStacktrace(StackTraceElement[] stackTrace) {
        int index = 0;
        for (StackTraceElement el : stackTrace) {
            stackTrace[index++] =
                new StackTraceElement(
                    el.getClassName(),
                    deobfuscateMethodName(el.getMethodName()),
                    el.getFileName(),
                    el.getLineNumber());
        }
        return stackTrace;
    }

    public String deobfuscateMethodName(String srgName) {
        String mcpName = srgMcpMethodMap.get(srgName);
        return mcpName != null ? mcpName : srgName;
    }
}
