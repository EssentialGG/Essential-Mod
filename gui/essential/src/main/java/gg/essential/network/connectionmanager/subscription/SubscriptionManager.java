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
package gg.essential.network.connectionmanager.subscription;

import gg.essential.connectionmanager.common.packet.subscription.SubscriptionUpdatePacket;
import gg.essential.gui.elementa.state.v2.MutableState;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.gui.elementa.state.v2.collections.MutableTrackedSet;
import gg.essential.gui.elementa.state.v2.collections.TrackedSet;
import gg.essential.network.CMConnection;
import gg.essential.network.connectionmanager.NetworkedManager;
import gg.essential.network.connectionmanager.queue.PacketQueue;
import gg.essential.network.connectionmanager.queue.SequentialPacketQueue;
import gg.essential.util.USession;
import kotlin.collections.SetsKt;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static gg.essential.gui.elementa.state.v2.SetKt.addAll;
import static gg.essential.gui.elementa.state.v2.SetKt.clear;
import static gg.essential.gui.elementa.state.v2.SetKt.mutableSetState;
import static gg.essential.gui.elementa.state.v2.SetKt.removeAll;
import static gg.essential.gui.elementa.state.v2.SetKt.toSetState;

public class SubscriptionManager implements NetworkedManager {

    @NotNull
    private final PacketQueue packetQueue;

    private final List<Listener> listeners = new ArrayList<>();

    private final MutableState<MutableTrackedSet<UUID>> subscriptions = mutableSetState();
    private final State<TrackedSet<UUID>> subscriptionsAndSelf = toSetState(observer -> {
        Set<UUID> combined = new LinkedHashSet<>();
        combined.add(USession.Companion.getActive().get(observer).getUuid());
        combined.addAll(subscriptions.get(observer));
        return combined;
    });

    public SubscriptionManager(@NotNull CMConnection cmConnection) {
        this.packetQueue = new SequentialPacketQueue.Builder(cmConnection)
                .onTimeoutRetransmit()
                .create();
    }

    public boolean isSubscribedOrSelf(@NotNull UUID uuid) {
        return this.isSubscribed(uuid) || uuid.equals(USession.Companion.activeNow().getUuid());
    }

    public boolean isSubscribed(@NotNull UUID uuid) {
        return this.subscriptions.getUntracked().contains(uuid);
    }

    public State<TrackedSet<UUID>> getSubscriptionsAndSelf() {
        return subscriptionsAndSelf;
    }

    public void subscribeToFeeds(@NotNull Set<UUID> uuids) {
        UUID ownUuid = USession.Companion.activeNow().getUuid();
        if (uuids.contains(ownUuid)) {
            subscribeToFeeds(SetsKt.minus(uuids, ownUuid));
            return;
        }

        addAll(this.subscriptions, uuids);
        this.packetQueue.enqueue(new SubscriptionUpdatePacket(uuids.toArray(new UUID[0]), true), response -> {
            for (Listener listener : this.listeners) {
                listener.onSubscriptionAdded(uuids);
            }
        });
    }

    public void unSubscribeFromFeeds(@NotNull Set<UUID> uuids) {
        UUID ownUuid = USession.Companion.activeNow().getUuid();
        if (uuids.contains(ownUuid)) {
            unSubscribeFromFeeds(SetsKt.minus(uuids, ownUuid));
            return;
        }

        removeAll(this.subscriptions, uuids);
        this.packetQueue.enqueue(new SubscriptionUpdatePacket(uuids.toArray(new UUID[0]), false), response -> {
            for (Listener listener : this.listeners) {
                listener.onSubscriptionRemoved(uuids);
            }
        });
    }

    @Override
    public void onConnected() {
        this.packetQueue.reset();

        // Clear all subscriptions, they are no longer valid as we may have missed updates
        Set<UUID> uuids = this.subscriptions.getUntracked();

        resetState();

        // Re-subscribe to all the UUIDs we are still interested in
        this.subscribeToFeeds(uuids);
    }

    @Override
    public void resetState() {
        Set<UUID> uuids = this.subscriptions.getUntracked();

        clear(this.subscriptions);

        for (Listener listener : this.listeners) {
            listener.onSubscriptionRemoved(uuids);
        }
    }

    public void addListener(@NotNull Listener listener) {
        this.listeners.add(listener);
    }

    public interface Listener {
        default void onSubscriptionAdded(@NotNull Set<UUID> uuids){}
        default void onSubscriptionRemoved(@NotNull Set<UUID> uuids){}
    }
}
