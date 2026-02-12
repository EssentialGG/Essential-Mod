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
package gg.essential.main;

import gg.essential.mixins.MixinErrorHandler;
import gg.essential.mixins.IntegrationTestsPlugin;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;

//#if MC<11400
import net.minecraft.launchwrapper.Launch;
//#endif

public class Bootstrap {
    /**
     * Used to get things going
     */
    @SuppressWarnings("unused") // called from api.EssentialTweaker
    public static void initialize() {
        MixinBootstrap.init();

        Mixins.addConfiguration("mixins.essential.json");
        Mixins.addConfiguration("mixins.essential.init.json");
        //#if MC>11400
        //$$ Mixins.addConfiguration("mixins.essential.modcompat.json");
        //#endif

        Mixins.registerErrorHandlerClass(MixinErrorHandler.class.getName());

        //#if MC<11400
        CodeSource codeSource = Bootstrap.class.getProtectionDomain().getCodeSource();
        if (codeSource != null) {
            URL location = codeSource.getLocation();
            try {
                // With LaunchWrapper, every class gets their own protection domain and urls are of the form
                // jar:file:/some/path/to/essential.jar!/package/of/ForkedJvm.class
                String classSuffix = "!/" + Bootstrap.class.getName().replace('.', '/') + ".class";
                String urlFile = location.getFile();
                if (location.getProtocol().equals("jar") && urlFile.endsWith(classSuffix)) {
                    location = new URL(urlFile.substring(0, urlFile.length() - classSuffix.length()));
                }

                File file = new File(location.toURI());
                if (file.isFile()) {
                    // This forces forge to reexamine the jar file for FML mods
                    // Should eventually be handled by Mixin itself, maybe?
                    net.minecraftforge.fml.relauncher.CoreModManager.getIgnoredMods().remove(file.getName());
                }
            } catch (URISyntaxException | MalformedURLException e) {
                e.printStackTrace();
            }

        } else {
            LogManager.getLogger().warn("No CodeSource, if this is not a development environment we might run into problems!");
            LogManager.getLogger().warn(Bootstrap.class.getProtectionDomain());
        }

        Launch.classLoader.registerTransformer(gg.essential.asm.compat.DisableVanillaFixAsyncScreenshotsTransformer.class.getName());
        Launch.classLoader.registerTransformer(gg.essential.asm.compat.DisableVanillaFixIntegratedServerShutdownFixTransformer.class.getName());
        //#endif

        if (IntegrationTestsPlugin.ENABLED) {
            Mixins.addConfiguration("mixins.essential.tests.json");
        }
    }
}
