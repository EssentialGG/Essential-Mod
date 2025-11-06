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
package gg.essential.mixins.impl.client.renderer.entity;

import gg.essential.config.EssentialConfig;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;

public class ArmorRenderingUtil {
    public static EssentialConfig.CosmeticOrArmor getCosmeticArmorSetting(Entity entity) {
        if (entity instanceof EntityPlayerSP) {
            return EssentialConfig.INSTANCE.getCosmeticArmorSettingSelf();
        } else {
            return EssentialConfig.INSTANCE.getCosmeticArmorSettingOther();
        }
    }
}
