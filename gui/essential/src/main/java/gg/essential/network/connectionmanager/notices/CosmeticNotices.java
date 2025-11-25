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

import gg.essential.gui.elementa.state.v2.MutableState;
import gg.essential.gui.elementa.state.v2.ObservedInstant;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.network.connectionmanager.cosmetics.CosmeticsData;
import gg.essential.network.cosmetics.Cosmetic;
import gg.essential.notices.NoticeType;
import gg.essential.notices.model.Notice;
import kotlin.Unit;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static gg.essential.cosmetics.ExtensionsKt.isAvailable;
import static gg.essential.gui.elementa.state.v2.StateKt.mutableStateOf;

public class CosmeticNotices implements NoticeListener {

    private final NoticesManager noticesManager;
    private final CosmeticsData cosmeticsData;
    private final String METADATA_KEY = "cosmetic_id";
    private final ConcurrentHashMap<String, MutableState<Boolean>> cosmeticToNewStateMap = new ConcurrentHashMap<>();
    private final MutableState<Boolean> hasAnyNewCosmetics = mutableStateOf(false);

    public CosmeticNotices(NoticesManager noticesManager, CosmeticsData cosmeticsData) {
        this.noticesManager = noticesManager;
        this.cosmeticsData = cosmeticsData;
    }

    public State<Boolean> getNewState(String cosmeticId) {
        return cosmeticToNewStateMap.computeIfAbsent(cosmeticId, ignored -> mutableStateOf(false));
    }

    private Notice getNotice(String cosmeticId) {
        List<Notice> notices = noticesManager.getNotices(NoticeType.NEW_BANNER, null, new HashMap<String, Object>() {{
            put(METADATA_KEY, cosmeticId);
        }});
        if (!notices.isEmpty()) {
            return notices.get(0);
        }
        return null;
    }

    public void clearNewState(String cosmeticId) {
        MutableState<Boolean> booleanState = cosmeticToNewStateMap.get(cosmeticId);
        if (booleanState != null) {
            booleanState.set(false);
        }

        Notice notice = getNotice(cosmeticId);
        if (notice != null) {
            noticesManager.queueDismissNotice(notice.getId());
        }

        updateGlobalState();
    }

    private void updateGlobalState() {
        hasAnyNewCosmetics.set(
            cosmeticToNewStateMap.entrySet().stream()
                .anyMatch(entry -> {
                    if (!entry.getValue().get()) {
                        return false;
                    }
                    final Cosmetic cosmetic = cosmeticsData.getCosmetic(entry.getKey());
                    if (cosmetic == null) {
                        return false;
                    }

                    // TODO make this whole class properly State-based
                    return isAvailable(cosmetic, new ObservedInstant(Instant.now(), __ -> Unit.INSTANCE));
                })
        );
    }

    public State<Boolean> getHasAnyNewCosmetics() {
        return hasAnyNewCosmetics;
    }

    @Override
    public void noticeAdded(Notice notice) {
        if (notice.getType() == NoticeType.NEW_BANNER && notice.getMetadata().containsKey(METADATA_KEY)) {
            String cosmeticId = (String) notice.getMetadata().get(METADATA_KEY);
            cosmeticToNewStateMap.computeIfAbsent(cosmeticId, ignored -> mutableStateOf(true)).set(true);

            updateGlobalState();
        }
    }

    @Override
    public void noticeRemoved(Notice notice) {
        // No impl for this manager
    }

    @Override
    public void onConnect() {
        resetState();
    }

    public void cosmeticAdded(String id) {
        final MutableState<Boolean> existingState = cosmeticToNewStateMap.get(id);
        if (existingState != null && existingState.get()) {
            updateGlobalState();
        }

    }
}
