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
package gg.essential.network.connectionmanager.notices;

import com.google.common.collect.Maps;
import gg.essential.connectionmanager.common.packet.Packet;
import gg.essential.connectionmanager.common.packet.notices.ClientNoticeBulkDismissPacket;
import gg.essential.connectionmanager.common.packet.notices.ClientNoticeRequestPacket;
import gg.essential.connectionmanager.common.packet.notices.ServerNoticeBulkDismissPacket;
import gg.essential.connectionmanager.common.packet.notices.ServerNoticePopulatePacket;
import gg.essential.connectionmanager.common.packet.notices.ServerNoticeRemovePacket;
import gg.essential.handlers.ShutdownHook;
import gg.essential.network.CMConnection;
import gg.essential.network.connectionmanager.NetworkedManager;
import gg.essential.notices.NoticeType;
import gg.essential.notices.model.Notice;
import kotlin.Unit;
import kotlin.collections.SetsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class NoticesManager implements NetworkedManager, INoticesManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoticesManager.class);

    @NotNull
    private final Map<String, Notice> notices = Maps.newConcurrentMap();

    @NotNull
    private final Set<String> dismissNoticesQueue = new HashSet<>();

    @NotNull
    private final CMConnection connectionManager;

    private final List<NoticeListener> listeners = new ArrayList<>();

    public NoticesManager(@NotNull final CMConnection connectionManager) {
        this.connectionManager = connectionManager;

        connectionManager.registerPacketHandler(ServerNoticePopulatePacket.class, packet -> {
            populateNotices(packet.getNotices());
            return Unit.INSTANCE;
        });
        connectionManager.registerPacketHandler(ServerNoticeRemovePacket.class, packet -> {
            removeNotices(packet.getIds());
            return Unit.INSTANCE;
        });

        ShutdownHook.INSTANCE.register(this::flushDismissNotices);
    }

    public void register(NoticeListener listener) {
        this.listeners.add(listener);
    }

    @NotNull
    public Map<String, Notice> getNotices() {
        return this.notices;
    }

    @NotNull
    public Optional<Notice> getNotice(@NotNull final String notice) {
        return Optional.ofNullable(this.notices.get(notice));
    }

    @NotNull
    public List<Notice> getNotices(
        @NotNull final NoticeType noticeType, @Nullable final Set<String> metadataKeys,
        @Nullable final Map<String, Object> metadataValues
    ) {
        return this.notices.values().stream()
            /* Notice Type */
            .filter(notice -> notice.getType() == noticeType)
            /* Metadata Keys */
            .filter(notice -> {
                if (metadataKeys == null || metadataKeys.isEmpty()) {
                    return true;
                }

                final Map<String, Object> metadata = notice.getMetadata();

                if (metadata.isEmpty()) {
                    return false;
                }

                for (@NotNull final String metadataKey : metadataKeys) {
                    if (!metadata.containsKey(metadataKey)) {
                        return false;
                    }
                }

                return true;
            })
            /* Metadata Values */
            .filter(notice -> {
                if (metadataValues == null || metadataValues.isEmpty()) {
                    return true;
                }

                final Map<String, Object> metadata = notice.getMetadata();

                if (metadata.isEmpty()) {
                    return false;
                }

                for (@NotNull final Map.Entry<String, Object> entry : metadataValues.entrySet()) {
                    final Object metadataValue = metadata.get(entry.getKey());

                    if (metadataValue == null) {
                        return false;
                    }

                    return Objects.equals(metadataValue, entry.getValue());
                }

                return false;
            })
            /* Collect */
            .collect(Collectors.toList());
    }

    @Override
    public void populateNotices(@NotNull final Collection<? extends Notice> notices) {
        for (@NotNull final Notice notice : notices) {
            this.notices.put(notice.getId(), notice);
            for (NoticeListener listener : listeners) {
                try {
                    listener.noticeAdded(notice);
                } catch (Exception e) {
                    LOGGER.error("An error occurred within a notice listener for noticeAdded: {}", notice.getId(), e);
                }
            }
        }
    }

    @Override
    public void removeNotices(@Nullable final Set<String> noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            for (Notice value : notices.values()) {
                for (NoticeListener listener : listeners) {
                    try {
                        listener.noticeRemoved(value);
                    } catch (Exception e) {
                        LOGGER.error("An error occurred within a notice listener for noticeRemoved: {}", value.getId(), e);
                    }
                }
            }
            this.notices.clear();
            return;
        }

        for (@NotNull final String noticeId : noticeIds) {
            final Notice removed = this.notices.remove(noticeId);
            if (removed == null) {
                continue;
            }
            for (NoticeListener listener : listeners) {
                try {
                    listener.noticeRemoved(removed);
                } catch (Exception e) {
                    LOGGER.error("An error occurred within a notice listener for noticeRemoved: {}", removed.getId(), e);
                }
            }
        }
    }

    @Override
    public void dismissNotice(String noticeId) {
        dismissNotices(SetsKt.setOf(noticeId));
    }

    public void dismissNotices(Set<String> noticeIds) {
        dismissNoticesQueue.addAll(noticeIds);
        flushDismissNotices();
    }

    /**
     * Queues the notice to be dismissed until the queue is flushed using {@link #flushDismissNotices()}
     * @param noticeId the notice id to queue for dismissal
     */
    public void queueDismissNotice(String noticeId) {
        dismissNoticesQueue.add(noticeId);
    }

    public void flushDismissNotices() {
        if (dismissNoticesQueue.isEmpty()) {
            return;
        }
        final Set<String> notices = new HashSet<>(dismissNoticesQueue);
        this.dismissNoticesQueue.clear();
        this.connectionManager.send(new ClientNoticeBulkDismissPacket(notices), maybePacket -> {
            if (maybePacket.isPresent()) {
                Packet packet = maybePacket.get();
                if (packet instanceof ServerNoticeBulkDismissPacket) {
                    ServerNoticeBulkDismissPacket serverNoticeBulkDismissPacket = (ServerNoticeBulkDismissPacket) packet;
                    Set<String> noticeIds = serverNoticeBulkDismissPacket.getNoticeIds();
                    if (!noticeIds.isEmpty()) {
                        removeNotices(noticeIds);
                    }
                    for (ServerNoticeBulkDismissPacket.ErrorDetails error : serverNoticeBulkDismissPacket.getErrors()) {
                        switch (error.getReason()) {
                            case "NOTICE_NOT_FOUND":
                            case "NOTICE_ALREADY_DISMISSED": {
                                removeNotices(SetsKt.setOf(error.getNoticeId()));
                                break;
                            }
                            default: {
                                LOGGER.error("Notice unable to be dismissed: NoticeId: {}, Reason: {}", error.getNoticeId(), error.getReason());
                                break;
                            }
                        }
                    }
                    return;
                }
            }
            LOGGER.error("Unexpected notice response: {}", maybePacket);
        });
    }

    @Override
    public void resetState() {
        this.notices.clear();

        listeners.forEach(NoticeListener::resetState);
    }

    @Override
    public void onConnected() {
        resetState();

        connectionManager.call(new ClientNoticeRequestPacket(null, SetsKt.setOf(NoticeType.values()), null, null)).fireAndForget();
        listeners.forEach(NoticeListener::onConnect);
    }

}
