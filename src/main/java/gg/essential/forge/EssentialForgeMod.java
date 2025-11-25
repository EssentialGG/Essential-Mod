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
package gg.essential.forge;

import gg.essential.Essential;

//#if NEOFORGE
//$$ import net.neoforged.fml.common.Mod;
//#elseif FORGE
import net.minecraftforge.fml.common.Mod;
//#endif

//#if NEOFORGE && MC>=12104
//$$ import gg.essential.util.HelpersKt;
//$$ import gg.essential.util.ResourceManagerUtil;
//$$ import net.neoforged.fml.ModList;
//$$ import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
//#endif

//#if FORGELIKE
//#if MC>=11400
//$$ @Mod(Essential.MODID)
//#else
@Mod(modid = Essential.MODID, name = Essential.NAME, version = Essential.VERSION, clientSideOnly = true)
//#endif
//#endif
public class EssentialForgeMod {
    public static final boolean USE_NEW_NEOFORGE_RESOURCE_EVENT;
    static {
        //#if NEOFORGE && MC>=12104
        //$$ boolean eventExists;
        //$$ try {
        //$$     Class.forName("net.neoforged.neoforge.client.event.AddClientReloadListenersEvent");
        //$$     eventExists = true;
        //$$ } catch (ClassNotFoundException e) {
        //$$     eventExists = false;
        //$$ }
        //$$ USE_NEW_NEOFORGE_RESOURCE_EVENT = eventExists;
        //#else
        USE_NEW_NEOFORGE_RESOURCE_EVENT = false;
        //#endif
    }

    public EssentialForgeMod() {
        //#if NEOFORGE && MC>=12104
        //$$ if (USE_NEW_NEOFORGE_RESOURCE_EVENT) {
        //$$   // Note: Using an anonymous class instead of a lambda so we don't end up with synthetic methods using
        //$$   // missing classes in its signature, which will lead to NoClassDefFoundError when accessed via reflection.
        //$$   class Handler implements java.util.function.Consumer<AddClientReloadListenersEvent> {
        //$$       @Override
        //$$       public void accept(AddClientReloadListenersEvent event) {
        //$$           event.addListener(HelpersKt.identifier("essential", "resource_manager"), ResourceManagerUtil.INSTANCE);
        //$$       }
        //$$   }
        //$$   ModList.get().getModContainerById(Essential.MODID).get().getEventBus().addListener(new Handler());
        //$$ }
        //#endif
    }
}
