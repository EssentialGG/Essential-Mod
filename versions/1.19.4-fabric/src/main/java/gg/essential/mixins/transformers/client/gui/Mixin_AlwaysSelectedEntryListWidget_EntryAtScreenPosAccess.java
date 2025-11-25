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
package gg.essential.mixins.transformers.client.gui;

import gg.essential.mixins.impl.client.gui.EssentialEntryAtScreenPosAccess;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AlwaysSelectedEntryListWidget.class)
public abstract class Mixin_AlwaysSelectedEntryListWidget_EntryAtScreenPosAccess<E extends AlwaysSelectedEntryListWidget.Entry<E>> extends EntryListWidget<E> implements EssentialEntryAtScreenPosAccess<E> {

    /**
     * Required for access to this method due to the protected subclass it is defined by at the source class
     * @return the entry at the given screen position
     */
    @Override
    public E essential$getEntryAtScreenPosition(double x, double y) {
        return getEntryAtPosition(x, y);
    }

    public Mixin_AlwaysSelectedEntryListWidget_EntryAtScreenPosAccess() {
        //#if MC>=12004
        //$$ super(null, 0, 0, 0, 0);
        //#else
        super(null, 0, 0, 0, 0, 0);
        //#endif
    }
}