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
package gg.essential.network.connectionmanager.media;

import com.google.common.collect.Sets;
import com.sparkuniverse.toolbox.chat.model.Channel;
import com.sparkuniverse.toolbox.util.DateTime;
import gg.essential.Essential;
import gg.essential.config.EssentialConfig;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.connectionmanager.common.packet.chat.ServerChatChannelMessagePacket;
import gg.essential.connectionmanager.common.packet.media.ClientMediaCreatePacket;
import gg.essential.connectionmanager.common.packet.media.ClientMediaDeleteRequestPacket;
import gg.essential.connectionmanager.common.packet.media.ClientMediaGetUploadUrlPacket;
import gg.essential.connectionmanager.common.packet.media.ClientMediaRequestPacket;
import gg.essential.connectionmanager.common.packet.media.ClientMediaUpdatePacket;
import gg.essential.connectionmanager.common.packet.media.ServerMediaPopulatePacket;
import gg.essential.connectionmanager.common.packet.media.ServerMediaUploadUrlPacket;
import gg.essential.connectionmanager.common.packet.response.ResponseActionPacket;
import gg.essential.event.render.RenderTickEvent;
import gg.essential.gui.NotificationsKt;
import gg.essential.gui.notification.Notifications;
import gg.essential.gui.screenshot.LocalScreenshot;
import gg.essential.gui.screenshot.ScreenshotId;
import gg.essential.gui.screenshot.ScreenshotOverlay;
import gg.essential.gui.screenshot.ScreenshotUploadToast;
import gg.essential.gui.screenshot.action.PostScreenshotAction;
import gg.essential.gui.screenshot.components.ScreenshotComponentsKt;
import gg.essential.gui.screenshot.components.ScreenshotProperties;
import gg.essential.gui.screenshot.components.ScreenshotProviderManager;
import gg.essential.gui.screenshot.concurrent.PrioritizedCallable;
import gg.essential.gui.screenshot.concurrent.PriorityThreadPoolExecutor;
import gg.essential.gui.screenshot.downsampling.PixelBuffer;
import gg.essential.gui.screenshot.handler.ScreenshotChecksumManager;
import gg.essential.gui.screenshot.handler.ScreenshotMetadataManager;
import gg.essential.gui.screenshot.image.ForkedImageClipboard;
import gg.essential.gui.screenshot.providers.FileCachedWindowedImageProvider;
import gg.essential.handlers.io.DirectoryWatcher;
import gg.essential.handlers.io.FileSystemEvent;
import gg.essential.handlers.screenshot.ClientScreenshotMetadata;
import gg.essential.handlers.screenshot.FileSystemEventKt;
import gg.essential.handlers.screenshot.ScreenshotUploadUtil;
import gg.essential.image.imagescaling.ResampleOp;
import gg.essential.lib.gson.Gson;
import gg.essential.media.model.Media;
import gg.essential.media.model.MediaLocationMetadata;
import gg.essential.media.model.MediaMetadata;
import gg.essential.media.model.MediaVariant;
import gg.essential.network.connectionmanager.ConnectionManager;
import gg.essential.network.connectionmanager.NetworkedManager;
import gg.essential.network.connectionmanager.chat.ChatManager;
import gg.essential.network.connectionmanager.handler.screenshot.ServerScreenshotListPacketHandler;
import gg.essential.universal.UDesktop;
import gg.essential.util.EssentialSounds;
import gg.essential.util.ExtensionsKt;
import gg.essential.util.GuiUtil;
import gg.essential.util.HSBColor;
import gg.essential.util.HelpersKt;
import gg.essential.util.MinecraftUtils;
import gg.essential.util.Multithreading;
import gg.essential.util.TemporaryFile;
import gg.essential.util.TimeFormatKt;
import gg.essential.util.UUIDUtil;
import gg.essential.util.lwjgl3.Lwjgl3Loader;
import gg.essential.util.lwjgl3.api.NativeImageReader;
import io.netty.buffer.UnpooledByteBufAllocator;
import kotlin.Unit;
import me.kbrewster.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static gg.essential.gui.screenshot.providers.WindowProviderKt.toSingleWindowRequest;

public class ScreenshotManager implements NetworkedManager, IScreenshotManager {

    private final NativeImageReader nativeImageReader;
    private final File editorStateFile;
    private final Map<String, Media> uploadedScreenshots = new HashMap<>();
    private final ConnectionManager connectionManager;
    private final ScreenshotMetadataManager screenshotMetadataManager;
    private final Set<String> screenshotFiles = Sets.newConcurrentHashSet();

    private final PriorityThreadPoolExecutor backgroundExecutor = new PriorityThreadPoolExecutor(1);
    private final FileCachedWindowedImageProvider minResolutionProvider;
    private final List<WeakReference<Consumer<ScreenshotCollectionChangeEvent>>> screenshotCollectionChangeHandlers = new ArrayList<>();
    private int frameCounter = -1;
    // Set and initialized in getter if null
    private HSBColor[] screenshotColors;

    private final ScreenshotChecksumManager screenshotChecksumManager;
    @NotNull
    private final Gson gson = new Gson();

    private final DirectoryWatcher screenshotFolderWatcher;

    private int latestScreenshotActionId = 0;

    public ScreenshotManager(ConnectionManager connectionManager, File baseDir, Lwjgl3Loader lwjgl3) {
        this.connectionManager = connectionManager;
        connectionManager.registerPacketHandler(ServerMediaPopulatePacket.class, new ServerScreenshotListPacketHandler(this));

        // Essential.getInstance() cannot be used at this point
        File metadataFolder = new File(baseDir, "screenshot-metadata");
        if (!metadataFolder.exists()) {
            Essential.logger.debug("Screenshot metadata directory not found. Creating...");
            try {
                Files.createDirectories(metadataFolder.toPath());
                Essential.logger.debug("Created screenshot metadata directory.");
            } catch (IOException e) {
                Essential.logger.error("Failed to create screenshot metadata directory.", e);
            }
        }
        nativeImageReader = lwjgl3.get(NativeImageReader.class);
        editorStateFile = new File(baseDir, "screenshot-editor.json");
        screenshotChecksumManager = new ScreenshotChecksumManager(new File(baseDir, "screenshot-checksum-caches.json"));
        screenshotMetadataManager = new ScreenshotMetadataManager(metadataFolder, screenshotChecksumManager);
        Essential.EVENT_BUS.register(this);
        minResolutionProvider = ScreenshotProviderManager.Companion.createFileCachedBicubicProvider(ScreenshotProviderManager.minResolutionTargetResolution, backgroundExecutor, UnpooledByteBufAllocator.DEFAULT, baseDir, nativeImageReader, true);
        Multithreading.runAsync(this::preloadScreenshots);
        screenshotFolderWatcher = new DirectoryWatcher(HelpersKt.getScreenshotFolder().toPath(), false, 1, TimeUnit.SECONDS);
        screenshotFolderWatcher.onBatchUpdate(this::flushFilesystemOperationsQueue);
    }

    @Override
    public void onConnected() {
        this.connectionManager.send(new ClientMediaRequestPacket(null));
    }

    private void preloadScreenshots() {
        // Primes the cache with all screenshot metadata
        File[] files = HelpersKt.getScreenshotFolder().listFiles();
        if (files != null) {
            for (File file : files) {
                if (!fileNameMatchesImage(file.getName())) {
                    continue;
                }

                screenshotFiles.add(file.getName());
                precompute(file);
                getScreenshotMetadataManager().getMetadata(file);
            }
        }
    }

    private boolean fileNameMatchesImage(String fileName) {
        return fileName.endsWith(".png") && fileName.length() > 4;
    }

    public NativeImageReader getNativeImageReader() {
        return nativeImageReader;
    }

    public void registerScreenshotCollectionChangeHandler(final Consumer<ScreenshotCollectionChangeEvent> handler) {
        this.screenshotCollectionChangeHandlers.add(new WeakReference<>(handler));
    }

    private void callScreenshotCollectionChangeHandlers(final ScreenshotCollectionChangeEvent event) {
        for (WeakReference<Consumer<ScreenshotCollectionChangeEvent>> reference : new ArrayList<>(this.screenshotCollectionChangeHandlers)) {
            Consumer<ScreenshotCollectionChangeEvent> handler = reference.get();
            if (handler == null) {
                this.screenshotCollectionChangeHandlers.remove(reference);
                continue;
            }
            handler.accept(event);
        }
    }

    public CompletableFuture<Media> upload(Path path) {
        return upload(path, screenshotMetadataManager.getOrCreateMetadata(path));
    }

    public CompletableFuture<Media> upload(Path path, ClientScreenshotMetadata metadata) {
        return upload(path, metadata, ScreenshotOverlay.INSTANCE.pushUpload());
    }

    public CompletableFuture<Media> upload(Path path, ClientScreenshotMetadata metadata, Consumer<ScreenshotUploadToast.ToastProgress> progressConsumer) {
        Media existingMediaIfPresent = getUploadedMedia(path);
        if (existingMediaIfPresent != null) {
            return CompletableFuture.completedFuture(existingMediaIfPresent);
        }
        CompletableFuture<Media> uploadFuture = new CompletableFuture<>();
        this.connectionManager.send(new ClientMediaGetUploadUrlPacket(), packetOptional -> {
            Packet packet = packetOptional.orElse(null);
            if (!(packet instanceof ServerMediaUploadUrlPacket)) {
                if (packet == null) {
                    uploadFuture.completeExceptionally(new ScreenshotUploadException("No response"));
                } else {
                    uploadFuture.completeExceptionally(new ScreenshotUploadException("Unexpected response: " + packet));
                }
            } else {
                progressConsumer.accept(new ScreenshotUploadToast.ToastProgress.Step(25));
                Multithreading.runAsync(() -> upload(path, metadata, (ServerMediaUploadUrlPacket) packet, progressConsumer, uploadFuture));
            }
        });
        uploadFuture.whenCompleteAsync((media, throwable) -> {
            if (throwable != null) {
                throwable.printStackTrace();
                if (throwable instanceof ScreenshotUploadException) {
                    progressConsumer.accept(new ScreenshotUploadToast.ToastProgress.Complete("Failed: " + throwable.getMessage(), false));
                } else {
                    progressConsumer.accept(new ScreenshotUploadToast.ToastProgress.Complete("Failed: An unknown error occurred", false));
                }
            }
        }, ExtensionsKt.getExecutor(Minecraft.getMinecraft()));
        return uploadFuture;
    }

    public CompletableFuture<Media> uploadAndCopyLinkToClipboard(Path path) {
        return uploadAndCopyLinkToClipboard(path, screenshotMetadataManager.getOrCreateMetadata(path));
    }

    public CompletableFuture<Media> uploadAndCopyLinkToClipboard(Path path, ClientScreenshotMetadata metadata) {
        return uploadAndCopyLinkToClipboard(path, metadata, ScreenshotOverlay.INSTANCE.pushUpload());
    }

    public CompletableFuture<Media> uploadAndCopyLinkToClipboard(Path path, ClientScreenshotMetadata metadata, Consumer<ScreenshotUploadToast.ToastProgress> progressConsumer) {
        return uploadAndAcceptMedia(path, metadata, progressConsumer, media -> copyLinkToClipboard(media, progressConsumer));
    }

    public CompletableFuture<Media> uploadAndShareLinkToChannels(List<Channel> channels, Path path) {
        return uploadAndShareLinkToChannels(channels, path, screenshotMetadataManager.getOrCreateMetadata(path));
    }

    public CompletableFuture<Media> uploadAndShareLinkToChannels(List<Channel> channels, Path path, ClientScreenshotMetadata metadata) {
        return uploadAndShareLinkToChannels(channels, path, metadata, ScreenshotOverlay.INSTANCE.pushUpload());
    }

    public CompletableFuture<Media> uploadAndShareLinkToChannels(List<Channel> channels, Path path, ClientScreenshotMetadata metadata, Consumer<ScreenshotUploadToast.ToastProgress> progressConsumer) {
        return uploadAndAcceptMedia(path, metadata, progressConsumer, media -> shareLinkToChannels(channels, media, progressConsumer));
    }

    public void copyLinkToClipboard(Media media) {
        copyLinkToClipboard(media, ScreenshotOverlay.INSTANCE.pushUpload());
    }

    private void copyLinkToClipboard(Media media, Consumer<ScreenshotUploadToast.ToastProgress> progressConsumer) {
        final MediaVariant embed = media.getVariants().get("embed");
        if (embed == null) {
            progressConsumer.accept(new ScreenshotUploadToast.ToastProgress.Complete("Error: Media link not supplied", false));
            return;
        }
        UDesktop.setClipboardString(embed.getUrl());
        progressConsumer.accept(new ScreenshotUploadToast.ToastProgress.Complete("Link copied to clipboard", true));
    }

    public void shareLinkToChannels(List<Channel> channels, Media media) {
        shareLinkToChannels(channels, media, ScreenshotOverlay.INSTANCE.pushUpload());
    }

    public void shareLinkToChannels(List<Channel> channels, Media media, Consumer<ScreenshotUploadToast.ToastProgress> progressConsumer) {
        final MediaVariant embed = media.getVariants().get("embed");
        if (embed == null) {
            progressConsumer.accept(new ScreenshotUploadToast.ToastProgress.Complete("Error: Media link not supplied", false));
            return;
        }

        Map<Channel, CompletableFuture<Boolean>> messageFutures = new HashMap<>();

        ChatManager chatManager = Essential.getInstance().getConnectionManager().getChatManager();
        for (Channel channel : channels) {
            CompletableFuture<Boolean> messageFuture = new CompletableFuture<>();
            messageFutures.put(channel, messageFuture);
            chatManager.sendMessage(
                    channel.getId(),
                    embed.getUrl(),
                    response -> {
                        messageFuture.complete(response.isPresent() && response.get() instanceof ServerChatChannelMessagePacket);
                    }
            );
        }

        CompletableFuture.allOf(messageFutures.values().toArray(new CompletableFuture[0])).whenCompleteAsync(
                (ignored, throwable) -> {
                    boolean anySucceeded = false;

                    for (Map.Entry<Channel, CompletableFuture<Boolean>> entry : messageFutures.entrySet()) {
                        // I used join, so we don't need to catch exceptions from future.get(), as we know all of them completed anyway
                        if (entry.getValue().join()) {
                            anySucceeded = true;
                        } else {
                            ScreenshotOverlay.INSTANCE.pushUpload().accept(new ScreenshotUploadToast.ToastProgress.Complete("Error: Failed to share to " + entry.getKey().getName(), false));
                        }
                    }

                    if (anySucceeded) {
                        progressConsumer.accept(new ScreenshotUploadToast.ToastProgress.Complete("Picture shared", true, channels));
                    } else {
                        progressConsumer.accept(new ScreenshotUploadToast.ToastProgress.Complete("Error: All the messages failed to send.", false));
                    }
                },
                ExtensionsKt.getExecutor(Minecraft.getMinecraft())
        );
    }

    public void screenshotsReceived(List<Media> screenshots) {
        if (screenshots == null) return;
        for (Media media : screenshots) {
            uploadedScreenshots.put(media.getId(), media);
        }
    }

    private CompletableFuture<Media> uploadAndAcceptMedia(Path path, ClientScreenshotMetadata metadata, Consumer<ScreenshotUploadToast.ToastProgress> progressConsumer, Consumer<Media> mediaConsumer) {
        CompletableFuture<Media> uploadFuture = upload(path, metadata, progressConsumer);
        uploadFuture.whenCompleteAsync((media, throwable) -> {
            if (media != null) {
                mediaConsumer.accept(media);
            }
        }, ExtensionsKt.getExecutor(Minecraft.getMinecraft()));

        return uploadFuture;
    }

    private void upload(Path path, @NotNull ClientScreenshotMetadata metadata, ServerMediaUploadUrlPacket packet, Consumer<ScreenshotUploadToast.ToastProgress> progressConsumer, CompletableFuture<Media> uploadFuture) {
        try {
            if (ScreenshotUploadUtil.INSTANCE.httpUpload(packet.getUploadUrl(), FileUtils.readFileToByteArray(path.toFile()))) {
                progressConsumer.accept(new ScreenshotUploadToast.ToastProgress.Step(50));
                // Successful upload
                UUIDUtil.getName(metadata.getAuthorId()).whenCompleteAsync((username, throwable) -> {
                    if (throwable != null) {
                        // Probably an edge case, but if the account that took a screenshot cannot be resolved because it does not exist,
                        // we fallback to the current username, which should exist if they are connected to the CM.
                        UUIDUtil.getName(UUIDUtil.getClientUUID()).whenCompleteAsync((username1, throwable1) -> {
                            if (throwable1 != null) {
                                uploadFuture.completeExceptionally(new ScreenshotUploadException("Unable to resolve current users username", throwable1));
                            } else {
                                completeUpload(metadata, packet, progressConsumer, uploadFuture, username1);
                            }
                        }, ExtensionsKt.getExecutor(Minecraft.getMinecraft()));
                    } else {
                        completeUpload(metadata, packet, progressConsumer, uploadFuture, username);
                    }
                }, ExtensionsKt.getExecutor(Minecraft.getMinecraft()));
            } else {
                uploadFuture.completeExceptionally(new ScreenshotUploadException("Unable to upload file to Cloudflare Images"));
            }

        } catch (IOException e) {
            uploadFuture.completeExceptionally(new ScreenshotUploadException("Unable to upload file to Cloudflare Images", e));
        }

    }

    private void completeUpload(@NotNull ClientScreenshotMetadata metadata, ServerMediaUploadUrlPacket packet, Consumer<ScreenshotUploadToast.ToastProgress> progressConsumer, CompletableFuture<Media> uploadFuture, String username) {
        progressConsumer.accept(new ScreenshotUploadToast.ToastProgress.Step(75));
        final DateTime time = metadata.getEditTime() != null ? metadata.getEditTime() : metadata.getTime();
        final String identifier = metadata.getLocationMetadata().getIdentifier();
        connectionManager.send(new ClientMediaCreatePacket(packet.getMediaId(), username + "'s Screenshot", "Captured " + TimeFormatKt.formatDateAndTime(time.toInstant()), new MediaMetadata(
            metadata.getAuthorId(),
            time,
            new MediaLocationMetadata(metadata.getLocationMetadata().getType().toNetworkType(), identifier, identifier == null ? null : connectionManager.getSpsManager().getHostFromSpsAddress(identifier)),
            metadata.getFavorite(),
            metadata.getEdited()
        )), packetOptional -> {
            final Packet packet1 = packetOptional.orElse(null);
            if (!(packet1 instanceof ServerMediaPopulatePacket)) {
                uploadFuture.completeExceptionally(new ScreenshotUploadException("Server gave unexpected response"));
            } else {
                uploadFuture.complete(((ServerMediaPopulatePacket) packet1).getMedias().get(0));
            }
            // Update the screenshot metadata store the media ID
            metadata.setMediaId(packet.getMediaId());
            Multithreading.runAsync(() -> screenshotMetadataManager.updateMetadata(metadata));
        });
    }

    public ClientScreenshotMetadata getCurrentMetadata() {
        String identifier = "Unknown";
        ClientScreenshotMetadata.Location.Type type = ClientScreenshotMetadata.Location.Type.UNKNOWN;
        if (Minecraft.getMinecraft().getCurrentServerData() != null) {
            String address = Minecraft.getMinecraft().getCurrentServerData().serverIP;
            if (connectionManager.getSpsManager().isSpsAddress(address)) {
                UUID spsHost = connectionManager.getSpsManager().getHostFromSpsAddress(address);
                if (spsHost != null) {
                    identifier = address;
                    type = ClientScreenshotMetadata.Location.Type.SHARED_WORLD;
                }
            }
            if ("Unknown".equals(identifier)) {
                type = ClientScreenshotMetadata.Location.Type.MULTIPLAYER;
                identifier = Minecraft.getMinecraft().getCurrentServerData().serverIP;
            }
        } else {
            String currentWorldFile = MinecraftUtils.INSTANCE.getWorldName();
            if (currentWorldFile != null) {
                identifier = currentWorldFile;
                type = ClientScreenshotMetadata.Location.Type.SINGLE_PLAYER;
            } else {
                GuiScreen guiScreen = GuiUtil.INSTANCE.openedScreen();
                if (guiScreen != null) {
                    identifier = GuiUtil.INSTANCE.getScreenName(guiScreen);
                    type = ClientScreenshotMetadata.Location.Type.MENU;
                }
            }
        }
        return new ClientScreenshotMetadata(
            UUIDUtil.getClientUUID(),
            new DateTime(System.currentTimeMillis()),
            "checksum",
            null,
            new ClientScreenshotMetadata.Location(type, identifier),
            false,
            false,
            null);
    }

    /**
     * Saves the content of the incoming image to the supplied file asynchronously
     */
    public void saveScreenshotAsync(RenderedImage img, File imgFile, ClientScreenshotMetadata sourceMetadata) {
        Multithreading.runAsync(() -> {
            try {
                String checksum = saveScreenshot(img, imgFile);
                ClientScreenshotMetadata metadata = ScreenshotComponentsKt.cloneWithNewChecksum(sourceMetadata, checksum);
                handleNewScreenshot(imgFile, metadata, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    /**
     * Saves the content of the provided image
     */
    public CompletableFuture<Void> saveDownloadedImageAsync(RenderedImage img) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Multithreading.runAsync(() -> {
            try {
                File imgFile = getDownloadedName(new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()));
                saveScreenshot(img, imgFile);
                future.complete(null);
            } catch (IOException e) {
                future.completeExceptionally(e);
                throw new RuntimeException(e);
            }
        });
        return future;
    }

    private File getDownloadedName(String name) {
        int i = 0;

        while (true) {
            File file1 = new File(HelpersKt.getScreenshotFolder(), "saved_" + name + (i == 0 ? "" : "_" + i) + ".png");
            if (!file1.exists()) {
                return file1;
            }

            ++i;
        }
    }

    private void precompute(File source) {
        backgroundExecutor.submit(new PrecomputeTask(source, minResolutionProvider));
    }

    /**
     * Used to notify when a new screenshot has been taken
     */
    public void handleNewScreenshot(File imgFile, ClientScreenshotMetadata metadata, boolean showOverlay) {
        screenshotChecksumManager.set(imgFile, metadata.getChecksum());
        screenshotMetadataManager.updateMetadata(metadata);
        screenshotFiles.add(imgFile.getName());
        precompute(imgFile);

        if (showOverlay) {
            ExtensionsKt.getExecutor(Minecraft.getMinecraft()).execute(() -> ScreenshotOverlay.INSTANCE.push(imgFile));
        }

        ExtensionsKt.getExecutor(Minecraft.getMinecraft()).execute(() -> {
            callScreenshotCollectionChangeHandlers(new ScreenshotCollectionChangeEvent(true, Collections.emptySet()));

            int currentId = ++latestScreenshotActionId;
            Multithreading.scheduleOnMainThread(
                () -> {
                    if (currentId != latestScreenshotActionId) {
                        return;
                    }

                    PostScreenshotAction.current().run(imgFile);
                },
                // We want to run the post-screenshot action with a short delay after the screenshot has been taken.
                // If another screenshot was taken in this time-span, we cancel the original scheduled runnable and
                // create a new one for the latest screenshot.
                // Time decided here: https://linear.app/essential/issue/EM-1933#comment-11ae93ec
                600,
                TimeUnit.MILLISECONDS
            );
        });
    }

    /**
     * Saves the supplied image at the designated file
     *
     * @return File checksum of newly saved file
     */
    private String saveScreenshot(RenderedImage image, File destination) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        final byte[] data = os.toByteArray();
        FileUtils.writeByteArrayToFile(destination, data);
        return DigestUtils.md5Hex(data);
    }

    @Nullable
    public String getChecksum(File file) {
        return screenshotChecksumManager.get(file);
    }

    @Override
    public Collection<Media> getUploadedMedia() {
        return uploadedScreenshots.values();
    }

    public Media getUploadedMedia(String mediaId) {
        return uploadedScreenshots.get(mediaId);
    }

    @NotNull
    public List<Path> getUploadedLocalPathsCache(String mediaId) {
        ClientScreenshotMetadata metadata = screenshotMetadataManager.getMetadataCache(mediaId);
        if (metadata == null) {
            return Collections.emptyList();
        }
        return screenshotChecksumManager.getPathsForChecksum(metadata.getChecksum());
    }

    public Media getUploadedMedia(Path path) {
        return getUploadedMedia(screenshotMetadataManager.getOrCreateMetadata(path).getMediaId());
    }

    public void copyScreenshotToClipboard(ScreenshotId screenshot) {
        try (TemporaryFile tmpFile = new TemporaryFile("screenshot", ".png");
             InputStream in = screenshot.open()) {
            Files.copy(in, tmpFile.getFile(), StandardCopyOption.REPLACE_EXISTING);
            copyScreenshotToClipboard(tmpFile.getFile().toFile(), screenshot.getName());
        } catch (IOException e) {
            e.printStackTrace();
            Notifications.INSTANCE.push("Error Copying Screenshot", "An unknown error occurred. Check logs for details");
        }
    }

    public void copyScreenshotToClipboard(File screenshot) {
        copyScreenshotToClipboard(screenshot, screenshot.getName());
    }

    public void copyScreenshotToClipboard(File screenshot, String name) {
        copyScreenshotToClipboardWithMessage(screenshot, "Successfully copied " + name + " to clipboard.");
    }

    public void copyScreenshotToClipboardWithMessage(File screenshot, String successMessage) {
        try (ForkedImageClipboard clipboard = new ForkedImageClipboard()) {
            if (clipboard.copy(screenshot)) {
                // When removing feature flag, the message param will no longer be used, so maybe clean it up
                NotificationsKt.sendPictureCopiedNotification();
            } else {
                gg.essential.gui.notification.ExtensionsKt.error(Notifications.INSTANCE, "Failed to copy picture", "");
            }
        }
    }

    @Override
    public ClientScreenshotMetadata setFavorite(Path path, boolean favorite) {
        final ClientScreenshotMetadata metadata = screenshotMetadataManager.getOrCreateMetadata(path);
        metadata.setFavorite(favorite);
        screenshotMetadataManager.updateMetadata(metadata);
        return metadata;
    }

    @Override
    public ClientScreenshotMetadata setFavorite(Media media, boolean favorite) {
        connectionManager.send(new ClientMediaUpdatePacket(media.getId(), null, null, favorite), maybeReply -> {
            Packet reply = maybeReply.orElse(null);
            if (reply instanceof ServerMediaPopulatePacket) {
                return; // all good
            }
            Essential.logger.error("Failed to update favorite state of {}: {}", media.getId(), reply);
        });
        media.getMetadata().setFavorite(favorite);
        return new ClientScreenshotMetadata(media);
    }

    public static DateTime getImageTime(Path path, @Nullable ClientScreenshotMetadata metadata, boolean includeEditTime) {
        return HelpersKt.getImageTime(new ScreenshotProperties(new LocalScreenshot(path), metadata), includeEditTime);
    }

    @Override
    public ScreenshotMetadataManager getScreenshotMetadataManager() {
        return screenshotMetadataManager;
    }

    /**
     * Synchronizes the screenshot browser and associated caches with any
     * external changes to the screenshot folder.
     */
    private Unit flushFilesystemOperationsQueue(List<FileSystemEvent> events) {

        // New list created from events that has modify operations replaced with a
        // delete then add operation. Redundant operations have also been removed.
        // See filterRedundancy docs for more info.
        final List<FileSystemEvent> items = FileSystemEventKt.filterRedundancy(events);


        Set<Path> deletedPaths = new HashSet<>();
        boolean anyNewItems = false;
        for (FileSystemEvent event : items) {
            if (!fileNameMatchesImage(event.getPath().getFileName().toString())) {
                continue;
            }
            switch (event.getEventType()) {
                case CREATE: {
                    if (screenshotFiles.add(event.getPath().getFileName().toString())) {
                        anyNewItems = true;
                    }
                    break;
                }
                case DELETE: {
                    if (handleDelete(event.getPath().toFile(), true)) {
                        deletedPaths.add(event.getPath());
                    }
                    break;
                }
                case MODIFY: throw new AssertionError("MODIFY should have been replaced with DELETE+CREATE");
            }
        }

        callScreenshotCollectionChangeHandlers(new ScreenshotCollectionChangeEvent(anyNewItems, deletedPaths));
        return Unit.INSTANCE;
    }

    /**
     * Handles the cleanup of caches, metadata, and states when a file is deleted
     *
     * @param file     the file to delete or cleanup
     * @param external whether the file was deleted externally or by the user
     * @return true if the file was removed from the [screenshotFiles] list, false if it was not present.
     */
    public boolean handleDelete(@NotNull File file, boolean external) {

        // Clean up downsampled caches
        final File screenshot_cache = new File(Essential.getInstance().getBaseDir(), "screenshot-cache");
        if (screenshot_cache.exists()) {
            final File[] files = screenshot_cache.listFiles();
            if (files != null) {
                for (File directory : files) {
                    if (directory.isDirectory()) {
                        final File downsampledCache = new File(directory, file.getName());
                        downsampledCache.delete();
                    }
                }
            }
        }

        boolean mutated = screenshotFiles.remove(file.getName());
        ScreenshotOverlay.INSTANCE.delete(file);
        if (external) {
            getScreenshotMetadataManager().handleExternalDelete(file.getName());
        } else {
            getScreenshotMetadataManager().deleteMetadata(file);
            file.delete();
        }
        return mutated;
    }

    public void deleteMedia(String mediaId, Path localFile) {
        uploadedScreenshots.remove(mediaId);

        if (localFile != null) {
            // FIXME: Should ideally only update this once CM confirms that it's been deleted.
            //        But that gets us into callback hell, so it's only something to consider once this class starts
            //        using coroutines.
            ClientScreenshotMetadata metadata = screenshotMetadataManager.getMetadata(localFile);
            if (metadata != null) {
                screenshotMetadataManager.updateMetadata(metadata.withMediaId(null));
            }
        }

        connectionManager.send(new ClientMediaDeleteRequestPacket(mediaId), maybeReply -> {
            Packet reply = maybeReply.orElse(null);
            if (reply instanceof ResponseActionPacket && ((ResponseActionPacket) reply).isSuccessful()) {
                return; // all good
            }
            Essential.logger.error("Failed to delete media with id {}: {}", mediaId, reply);
            Notifications.INSTANCE.push("Error Deleting Screenshot", "An unknown error occurred. Check logs for details");
        });
    }

    @Override
    @NotNull
    public List<Path> getOrderedPaths() {
        return HelpersKt.getOrderedPaths(this.screenshotFiles, HelpersKt.getScreenshotFolder().toPath(), path -> getImageTime(path, screenshotMetadataManager.getMetadata(path), true));
    }

    private File getEditedName(ScreenshotId source) {
        String base = source.getName().replaceFirst("(_edited(_\\d+)?)?\\.png", "");
        int i = 1;

        while (true) {
            File file1 = new File(HelpersKt.getScreenshotFolder(), base + "_edited" + (i == 1 ? "" : "_" + i) + ".png");
            if (!file1.exists()) {
                return file1;
            }

            ++i;
        }
    }

    public File handleScreenshotEdited(@NotNull ScreenshotId source, @NotNull ClientScreenshotMetadata originalMetadata, @NotNull BufferedImage screenshot, boolean favorite) {
        File output = getEditedName(source);

        try {
            final String checksum = saveScreenshot(screenshot, output);

            screenshotMetadataManager.updateMetadata(new ClientScreenshotMetadata(originalMetadata.getAuthorId(), originalMetadata.getTime(), checksum, new DateTime(System.currentTimeMillis()), originalMetadata.getLocationMetadata(), favorite, true, null));

            screenshotFiles.add(output.getName());
            // Precomputing not done here because the UI is already open and will it will generate what it needs as it needs it

            ExtensionsKt.getExecutor(Minecraft.getMinecraft()).execute(() -> {
                NotificationsKt.sendCheckmarkNotification("Picture saved as copy");
            });
            return output;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateEditorColors(HSBColor[] colors) {
        if (Arrays.equals(screenshotColors, colors)) {
            return;
        }
        screenshotColors = colors;
        try {
            FileUtils.write(editorStateFile, gson.toJson(colors), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HSBColor[] getEditorColors() {
        if (screenshotColors == null) {
            if (editorStateFile.exists()) {
                try {
                    screenshotColors = gson.fromJson(FileUtils.readFileToString(editorStateFile, StandardCharsets.UTF_8), HSBColor[].class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (screenshotColors == null || screenshotColors.length != 5) {
                screenshotColors = new HSBColor[]{new HSBColor(0xD32121), new HSBColor(0xEAB600), new HSBColor(0x3B8A2F), new HSBColor(0x0085FF), new HSBColor(0x000000)};
            }
        }
        return screenshotColors;
    }

    private boolean hasOverlay() {
        return Notifications.INSTANCE.hasActiveNotifications() || ScreenshotOverlay.INSTANCE.hasActiveNotifications();
    }

    public void handleScreenshotKeyPressed() {
        if (hasOverlay()) {
            Notifications.INSTANCE.hide();
            ScreenshotOverlay.INSTANCE.hide();
            frameCounter = 2; // RenderTickEvent will be fired later this same tick, so we want to wait for the second call next frame
        } else {
            takeNow();
        }
    }

    @Subscribe
    public void tick(RenderTickEvent event) {
        if (!event.isPre()) {
            return;
        }
        if (frameCounter > 0) {
            frameCounter--;
        }
        if (frameCounter == 0) {
            frameCounter = -1;
            takeNow();
            Notifications.INSTANCE.show();
            ScreenshotOverlay.INSTANCE.show();
        }
    }

    public boolean suppressBufferSwap() {
        return frameCounter == 1;
    }

    /**
     * Takes a screenshot using the Minecraft handling, which will eventually call back to this class
     * via saveScreenshotAsync() or handleNewScreenshot()
     */
    private void takeNow() {
        Minecraft mc = Minecraft.getMinecraft();

        if (EssentialConfig.INSTANCE.getScreenshotSounds()) {
            EssentialSounds.INSTANCE.playScreenshotSound();
        }

        //#if MC<=11202
        screenshotMessageCallback(ScreenShotHelper.saveScreenshot(mc.mcDataDir, mc.displayWidth, mc.displayHeight, mc.getFramebuffer()));
        //#elseif MC<11700
        //$$  ScreenShotHelper.saveScreenshot(mc.gameDir, mc.getMainWindow().getWidth(), mc.getMainWindow().getHeight(), mc.getFramebuffer(), message -> screenshotMessageCallback(message));
        //#else
        //$$  ScreenshotRecorder.saveScreenshot(mc.runDirectory, mc.getFramebuffer(), message -> screenshotMessageCallback(message));
        //#endif
    }

    private void screenshotMessageCallback(ITextComponent component) {
        if (EssentialConfig.INSTANCE.getEnableVanillaScreenshotMessage()) {
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(component);
        }
    }

    private static class PrecomputeTask extends PrioritizedCallable<Void> {

        private final File file;
        private final FileCachedWindowedImageProvider provider;

        public PrecomputeTask(File file, FileCachedWindowedImageProvider provider) {
            super(Integer.MAX_VALUE, PRECOMPUTE, 0);
            this.file = file;
            this.provider = provider;
        }

        @Override
        public Void call() throws Exception {
            try {
                ResampleOp.isBackgroundTask.set(true);
                provider.setItems(Collections.singletonList(new LocalScreenshot(file.toPath())));
                provider.provide(toSingleWindowRequest(0), Collections.emptySet()).values().forEach(PixelBuffer::release);
                return null;
            } finally {
                ResampleOp.isBackgroundTask.set(false);
            }
        }
    }
}
