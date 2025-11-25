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

import gg.essential.Essential;
import gg.essential.gui.elementa.state.v2.MutableState;
import gg.essential.gui.elementa.state.v2.State;
import gg.essential.gui.state.Sale;
import gg.essential.notices.NoticeType;
import gg.essential.notices.model.Notice;
import gg.essential.util.Multithreading;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static gg.essential.gui.elementa.state.v2.StateKt.mutableStateOf;

public class SaleNoticeManager implements NoticeListener {

    private final boolean saleSuppressedByJvmFlag = System.getProperty("essential.disableSale", "false").equals("true");
    private final MutableState<@NotNull Set<Sale>> currentState = mutableStateOf(Collections.emptySet());

    private final Map<String, Sale> salesMap = new HashMap<>();
    private ScheduledFuture<?> nextUpdateFuture = null;

    @Override
    public void noticeAdded(Notice notice) {
        if (saleSuppressedByJvmFlag) {
            return;
        }

        if (notice.getType() == NoticeType.SALE) {
            if (notice.getExpiresAt() == null) {
                Essential.logger.error("Notice " + notice.getId() + " is type sale but does not have an expiration date set!");
                return;
            }
            final int discount = ((Number) notice.getMetadata().get("discount")).intValue();
            @Nullable Set<Integer> packagesSet = null;
            if (notice.getMetadata().containsKey("packages")) {
                packagesSet = new HashSet<>();
                for (Number packages : (Collection<Number>) notice.getMetadata().get("packages")) {
                    packagesSet.add(packages.intValue());
                }
                if (packagesSet.isEmpty()) {
                    packagesSet = null;
                }
            }
            Set<String> onlyCosmetics = null;
            if (notice.getMetadata().containsKey("cosmetics")) {
                onlyCosmetics = new HashSet<>((Collection<String>) notice.getMetadata().get("cosmetics"));
            }
            salesMap.put(
                notice.getId(),
                new Sale(
                    notice.getExpiresAt().toInstant(),
                    ((String) notice.getMetadata().get("sale_name")),
                    notice.getMetadata().containsKey("sale_name_compact") ? ((String) notice.getMetadata().get("sale_name_compact")) : (discount == 0 ? null : "SALE"),
                    discount,
                    (Boolean) notice.getMetadata().getOrDefault("display_time", Boolean.TRUE),
                    (String) notice.getMetadata().get("category"),
                    packagesSet,
                    onlyCosmetics,
                    (String) notice.getMetadata().get("tooltip"),
                    (String) notice.getMetadata().get("coupon")
                )
            );
            refreshState();
        }
    }

    private void refreshState() {
        currentState.set(new HashSet<>(salesMap.values()));
        scheduleUpdate();
    }

    private void scheduleUpdate() {
        if (nextUpdateFuture != null) {
            nextUpdateFuture.cancel(false);
        }
        Optional<Sale> first = salesMap.values().stream().min(Comparator.comparing(Sale::getExpiration));
        if (first.isPresent()) {
            final Sale sale = first.get();
            nextUpdateFuture = Multithreading.scheduleOnMainThread(() -> {
                final Instant now = Instant.now();

                if (salesMap.entrySet().removeIf(entry -> entry.getValue().getExpiration().isBefore(now))) {
                    refreshState();
                }
            }, Instant.now().until(sale.getExpiration(), ChronoUnit.MILLIS) + 1000, TimeUnit.MILLISECONDS);
        }
    }

    public State<@NotNull Set<Sale>> getSaleState() {
        return currentState;
    }

    @Override
    public void noticeRemoved(Notice notice) {
        if (notice.getType() == NoticeType.SALE) {
            salesMap.remove(notice.getId());
            refreshState();
        }
    }

    @Override
    public void onConnect() {}
}
