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
package gg.essential.compat.aether;


import net.minecraft.entity.player.EntityPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

// 1.21+ fabric and neoforge also 1.20.1 fabric
//#if MC >= 12100 && !FORGE || MC == 12001 && FABRIC
//$$ import io.wispforest.accessories.api.AccessoriesCapability;
//$$ import io.wispforest.accessories.api.slot.SlotEntryReference;
//$$ import io.wispforest.accessories.pond.AccessoriesAPIAccess;
//#endif

// 1.19.2 & 1.19.4 forge only
// 1.20.1 fabric & forge (+ technically neoforge)
// 1.20.4 neoforge only
//#if MC >= 11902 && MC <= 11904 && FORGE || MC == 12001 && FORGE || MC == 12004 && NEOFORGE
//$$ import java.util.Optional;
//$$ import top.theillusivec4.curios.api.CuriosApi;
//$$ import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
//$$ import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
//#endif

//#if MC == 11202
import com.gildedgames.the_aether.api.AetherAPI;
import com.gildedgames.the_aether.api.player.IPlayerAether;
import net.minecraft.item.ItemStack;
//#endif

/*
Explanation for the long preprocessor conditions:
1.12.2 Aether is Forge only
no releases until 1.19.2, Note: the github does have unused 1.16 and 1.18 branches
1.19.2 is forge only
1.19.4 is forge only
1.20.1 fabric and forge (+ technically the first neoforge builds that were just straight forge copies) (both use different accessory mods)
1.20.2 is neoforge only (but we don't support that neoforge version)
1.20.4 is neoforge only
no 1.20.6 builds
1.21.1 which has fabric and neoforge and will presumably be the case going forwards (as of writing 1.21.1 is the latest aether release) (both now use the same accessory dependency)
 */

public class AetherGlovesCompat {
    private static final boolean AETHER_LOADED;
    private static final Logger LOGGER = LogManager.getLogger("Essential Logger - AetherGlovesCompat");

    static {
        boolean loaded = false;
        //#if MC >= 11200
        try {
            // Accessory dependency mod check, is internal to Aether in 1.12.2
            //#if MC >= 12100 && !FORGE || MC == 12001 && FABRIC
            //$$ Class.forName("io.wispforest.accessories.api.AccessoriesAPI");
            //#elseif MC >= 11902 && MC <= 11904 && FORGE || MC == 12001 && FORGE || MC == 12004 && NEOFORGE
            //$$ Class.forName("top.theillusivec4.curios.api.CuriosApi");
            //#endif

            // Aether mod check
            // Note: the unused 1.16 and 1.18 github branches use "com.gildedgames.aether.Aether"
            //#if MC >= 11902 && MC <= 11904 && FORGE || MC == 12001 || MC == 12004 && NEOFORGE || MC >= 12100 && !FORGE
            //$$ Class.forName("com.aetherteam.aether.Aether");
            //$$ loaded = true;
            //#elseif MC == 11202
            Class.forName("com.gildedgames.the_aether.Aether");
            loaded = true;
            //#endif
        } catch (Exception e) {
            if (e instanceof ClassNotFoundException) LOGGER.debug("The Aether mod is not present, or is an unsupported version. Cosmetic glove compatibility feature disabled.");
            else LOGGER.error("Unexpected exception checking for The Aether mod:", e);
        }
        //#endif
        AETHER_LOADED = loaded;
    }

    public static boolean isWearingGloves(EntityPlayer player) {
        if (!AETHER_LOADED || player == null) return false;
        return isWearingGlovesImpl(player);
    }

    private static boolean isWearingGlovesImpl(@NotNull EntityPlayer player) {
        // 1.21+ fabric and neoforge also 1.20.1 fabric
        //#if MC >= 12100 && !FORGE || MC == 12001 && FABRIC
        //$$ try {
        //$$     AccessoriesCapability capability = ((AccessoriesAPIAccess) player).accessoriesCapability();
        //$$     if (capability == null) return false;
        //$$
        //$$     for (SlotEntryReference slotEntryReference : capability.getAllEquipped()) {
        //$$         // Check if this is our glove slot, and if it is marked visible
        //$$         if (slotEntryReference.reference().slotName()
                        //#if MC >= 12100
                        //$$ .equals("aether:gloves_slot")
                        //#else
                        //$$ .equals("aether_gloves")
                        //#endif
        //$$                 && slotEntryReference.reference().slotContainer().shouldRender(0)) return true;
        //$$     }
        //$$ } catch (Exception e) { warnOnce(e); }
        //#endif

        // 1.19.2 & 1.19.4 forge only
        // 1.20.1 forge (+ technically neoforge)
        // 1.20.4 neoforge only
        //#if MC >= 11902 && MC <= 11904 && FORGE || MC == 12001 && FORGE || MC == 12004 && NEOFORGE
        //$$ try {
            //#if MC >= 12004
            //$$ var handler = CuriosApi.getCuriosInventory(player);
            //#else
            //$$ var handler = CuriosApi.getCuriosHelper().getCuriosHandler(player).resolve();
            //#endif
        //$$     if (handler.isEmpty()) return false;
        //$$     ICurioStacksHandler it = handler.get().getCurios().get("aether_gloves");
        //$$     // Check if the stack is not air, and also if the slot has been marked visible
        //$$     if (!it.getStacks().getStackInSlot(0).isEmpty() && it.getRenders().get(0)) return true;
        //$$ } catch (Exception e) { warnOnce(e); }
        //#endif

        //#if MC == 11202
        try {
            IPlayerAether aether = AetherAPI.getInstance().get(player);
            // The 1.12.2 api can only return a list of all accessory ItemStacks equipped which will all be air if empty
            for (ItemStack o : aether.getAccessoryInventory().getAccessories()) {
                // 1.12.2 Aether also doesn't have accessory hiding so no visibility check needed
                if (o.getUnlocalizedName().endsWith("_gloves")) return true;
            }
        } catch (Exception e) { warnOnce(e); }
        //#endif

        return false;
    }

    private static boolean once = false;

    // Just in case (third party code)
    private static void warnOnce(Exception e) {
        if (once) return;
        LOGGER.warn("Essential's cosmetic compatibility with [The Aether] and its glove armor has broken, and is now disabled. Cause:", e);
        once = true;
    }
}