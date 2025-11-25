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
package gg.essential.mixins.transformers.client;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import gg.essential.Essential;
import gg.essential.event.client.ReAuthEvent;
import gg.essential.event.gui.GuiOpenedEvent;
import gg.essential.mixins.ext.server.integrated.IntegratedServerExt;
import gg.essential.sps.McIntegratedServerManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Session;
import gg.essential.event.gui.GuiOpenEvent;
import gg.essential.mixins.impl.client.MinecraftExt;
import gg.essential.mixins.impl.client.MinecraftHook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Objects;

import static gg.essential.universal.UMinecraft.isCallingFromMinecraftThread;
import static gg.essential.util.HelpersKt.toUSession;

//#if MC>=12109
//$$ import com.llamalad7.mixinextras.sugar.Local;
//$$ import net.minecraft.util.ApiServices;
//$$ import org.spongepowered.asm.mixin.Unique;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

//#if MC>=12002
//$$ import com.mojang.authlib.yggdrasil.ProfileResult;
//$$ import net.minecraft.util.Util;
//$$ import java.util.concurrent.CompletableFuture;
//#endif

//#if MC>=11400
//#else
import gg.essential.event.gui.MouseScrollEvent;
import org.lwjgl.input.Mouse;
//#endif

//#if MC>=11700
//$$ import net.minecraft.client.network.SocialInteractionsManager;
//#if MC>=11903
//$$ import net.minecraft.client.util.ProfileKeysImpl;
//#endif
//#if MC>=11900
//$$ import net.minecraft.client.util.ProfileKeys;
//#endif
//#if MC>=11800
//$$ import com.mojang.authlib.minecraft.UserApiService;
//#else
//$$ import com.mojang.authlib.minecraft.SocialInteractionsService;
//$$ import com.mojang.authlib.minecraft.OfflineSocialInteractions;
//#endif
//#endif

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements MinecraftExt {

    private final MinecraftHook minecraftHook = new MinecraftHook((Minecraft) (Object) this);

    private GuiOpenEvent guiOpenEvent;

    @Shadow @Mutable @Final private Session session;

    //#if MC>=12109
    //$$ @Shadow @Final private ApiServices apiServices;
    //#endif

    //#if MC>=12002
    //$$ @Shadow @Mutable @Final private CompletableFuture<ProfileResult> gameProfileFuture;
    //#if MC<12109
    //$$ @Shadow @Final private MinecraftSessionService sessionService;
    //#endif
    //#else
    @Shadow public abstract PropertyMap getProfileProperties();
    //#endif

    //#if MC>=11700
    //$$ @Shadow @Mutable @Final private SocialInteractionsManager socialInteractionsManager;
    //#if MC>=11900
    //#if MC>=12109
    //$$ @Unique
    //$$ private YggdrasilAuthenticationService authenticationService;
    //$$ @Inject(method = "createUserApiService", at = @At("HEAD"))
    //$$ private void captureAuthService(CallbackInfoReturnable<?> ci, @Local(argsOnly = true) YggdrasilAuthenticationService authenticationService) {
    //$$     this.authenticationService = authenticationService;
    //$$ }
    //#else
    //$$ @Shadow @Mutable @Final private YggdrasilAuthenticationService authenticationService;
    //#endif
    //$$
    //$$ @Shadow @Mutable @Final private ProfileKeys profileKeys;
    //$$
    //$$ @Shadow @Final public File runDirectory;
    //#else
    //$$ @Shadow public abstract MinecraftSessionService getSessionService();
    //#endif
    //#if MC>=11800
    //$$ @Shadow @Mutable @Final private UserApiService userApiService;
    //#else
    //$$ @Shadow @Mutable @Final private SocialInteractionsService socialInteractionsService;
    //#endif
    //#endif

    /**
     * Invoked once the game is launching
     */
    //#if MC < 11400
    @Inject(method = "init", at = @At("HEAD"))
    //#elseif FABRIC
    //$$ @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;dataFixer:Lcom/mojang/datafixers/DataFixer;", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    //#else
    //$$ @Inject(method = "<init>", at = @At(value = "ESSENTIAL:CONSTANT_IN_INIT", args = "stringValue=Backend library: {}"))
    //#endif
    private void preinit(CallbackInfo ci) {
        minecraftHook.preinit();
    }

    /**
     * Invoked once the game has finished launching
     */
    //#if MC < 11400
    @Inject(method = "init", at = @At(value = "INVOKE",
            //#if MC<=10809
            //$$ target = "Lnet/minecraft/client/particle/EffectRenderer;<init>(Lnet/minecraft/world/World;Lnet/minecraft/client/renderer/texture/TextureManager;)V"
            //#else
            target = "Lnet/minecraft/client/particle/ParticleManager;<init>(Lnet/minecraft/world/World;Lnet/minecraft/client/renderer/texture/TextureManager;)V"
            //#endif
    ))
    //#else
    //$$ @Inject(method = "<init>", at = @At(
        //#if MC<11400 || FABRIC
        //$$ value = "INVOKE", shift = At.Shift.AFTER,
        //#else
        //$$ value = "ESSENTIAL:AFTER_INVOKE_IN_INIT",
        //#endif
    //$$     target = "Lnet/minecraft/client/Minecraft;updateWindowSize()V"
    //$$ ))
    //#endif
    private void init(CallbackInfo ci) {
        minecraftHook.startGame();
    }

    //#if MC < 11400
    @Inject(method = "init", at = @At("RETURN"))
    //#else
    //$$ @Inject(method = "<init>", at = @At("RETURN"))
    //#endif
    private void postInit(CallbackInfo ci) {
        minecraftHook.postInit();
    }

    /**
     * Invoked every tick (every 50milliseconds)
     */
    @Inject(method = "runTick", at = @At("RETURN"))
    private void runTick(CallbackInfo ci) {
        minecraftHook.runTick();
    }

    //#if MC>=11400
    //$$ // Note: This is targeted after the cancelTasks call, such that any tasks we schedule from the event
    //$$ //       won't immediately be cancelled (which could otherwise result in the game locking up, e.g. when
    //$$ //       scheduling a resource pack reload which will then never finish).
    //#if MC>=12005
    //$$ @Inject(method = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;cancelTasks()V", shift = At.Shift.AFTER))
    //#else
    //$$ @Inject(method = "unloadWorld(Lnet/minecraft/client/gui/screen/Screen;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;dropTasks()V", shift = At.Shift.AFTER))
    //#endif
    //$$ private void unloadWorld(CallbackInfo ci) {
    //$$     minecraftHook.disconnect();
    //$$ }
    //#else
    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void loadWorld(WorldClient worldClient, String message, CallbackInfo ci) {
        if (worldClient == null) {
            minecraftHook.disconnect();
        }
    }
    //#endif

    @Inject(method = "shutdown", at = @At("HEAD"))
    private void shutdown(CallbackInfo ci) {
        minecraftHook.shutdown();
    }

    @Inject(method = "displayGuiScreen", at = @At("HEAD"))
    private void checkThreadSafety(CallbackInfo ci) {
        if (!isCallingFromMinecraftThread()) {
            Essential.logger.error("Detected call to `openScreen` on thread {}. " +
                "This method is NOT thread safe and MUST NOT be called from any thread except the main client thread! " +
                "Please report this to the mod responsible as per the following stacktrace:",
                Thread.currentThread(),
                new Throwable());
        }
    }

    @ModifyVariable(method = "displayGuiScreen", at = @At("HEAD"))
    public GuiScreen displayGuiScreen(GuiScreen screen) {
        guiOpenEvent = minecraftHook.displayGuiScreen(screen);
        return guiOpenEvent.getGui();
    }

    @Inject(method = "displayGuiScreen", at = @At("HEAD"), cancellable = true)
    public void displayGuiScreen(GuiScreen screen, CallbackInfo info) {
        if (guiOpenEvent != null && guiOpenEvent.isCancelled()) info.cancel();
    }

    @Inject(method = "displayGuiScreen", at = @At("TAIL"))
    public void essential$fireGuiOpenedEvent(GuiScreen screen, CallbackInfo info) {
        if (screen == null) {
            return;
        }

        Essential.EVENT_BUS.post(new GuiOpenedEvent(screen));
    }

    @Override
    public void setSession(Session session) {
        Session oldSession = this.session;

        this.session = session;

        //#if MC>=12002
        //#if MC>=12109
        //$$ MinecraftSessionService sessionService = this.apiServices.sessionService();
        //#else
        //$$ MinecraftSessionService sessionService = this.sessionService;
        //#endif
        //$$ this.gameProfileFuture = CompletableFuture.supplyAsync(() ->
        //$$     sessionService.fetchProfile(session.getUuidOrNull(), true), Util.getIoWorkerExecutor());
        //#else
        if (!Objects.equals(oldSession.getProfile().getId(), session.getProfile().getId())) {
            this.getProfileProperties().clear();
        }
        //#endif

        //#if MC>=11700
        //$$
        //#if MC>=11900
        //$$ YggdrasilAuthenticationService authenticationService = this.authenticationService;
        //#else
        //$$ YggdrasilAuthenticationService authenticationService = ((YggdrasilMinecraftSessionService) this.getSessionService()).getAuthenticationService();
        //#endif
        //#if MC>=12004
        //$$ this.userApiService = authenticationService.createUserApiService(session.getAccessToken());
        //#else
        //$$ try {
        //#if MC>=11800
        //$$     this.userApiService = authenticationService.createUserApiService(session.getAccessToken());
        //#else
        //$$     this.socialInteractionsService = authenticationService.createSocialInteractionsService(session.getAccessToken());
        //#endif
        //$$ } catch (AuthenticationException e) {
        //$$     Essential.logger.error("Failed to verify authentication", e);
        //#if MC>=11800
        //$$     this.userApiService = UserApiService.OFFLINE;
        //#else
        //$$     this.socialInteractionsService = new OfflineSocialInteractions();
        //#endif
        //$$ }
        //#endif
        //#if MC>=11800
        //$$ this.socialInteractionsManager = new SocialInteractionsManager((MinecraftClient) (Object) this, this.userApiService);
        //#else
        //$$ this.socialInteractionsManager = new SocialInteractionsManager((MinecraftClient) (Object) this, this.socialInteractionsService);
        //#endif
        //#if MC>=12002
        //$$ this.profileKeys = ProfileKeys.create(this.userApiService, session, this.runDirectory.toPath());
        //#elseif MC>=11903
        //$$ this.profileKeys = new ProfileKeysImpl(this.userApiService, session.getProfile().getId(), this.runDirectory.toPath());
        //#elseif MC>=11900
        //$$ this.profileKeys = new ProfileKeys(this.userApiService, session.getProfile().getId(), this.runDirectory.toPath());
        //#endif
        //$$
        //#endif

        Essential.EVENT_BUS.post(new ReAuthEvent(toUSession(session)));
    }

    //#if MC>=11400
    //$$ // Already handled in MixinMouse
    //#else
    @Shadow public GuiScreen currentScreen;

    @ModifyConstant(
        //#if MC>=11200
        method = "runTickMouse",
        //#else
        //$$ method = "runTick",
        //#endif
        constant = @Constant(longValue = 200),
        slice = @Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;systemTime:J", opcode = Opcodes.GETFIELD),
            to = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I")
        )
    )
    private long mouseScroll(long ret) {
        if (this.currentScreen == null && Mouse.getEventDWheel() != 0) {
            MouseScrollEvent event = new MouseScrollEvent(Mouse.getEventDWheel(), null);
            Essential.EVENT_BUS.post(event);
            if (event.isCancelled()) {
                ret = -1; // this effectively skips the if which would otherwise process the event
            }
        }
        return ret;
    }
    //#endif

    @Shadow @Nullable private IntegratedServer integratedServer;

    //#if MC>=11900
    //$$ private static final String LAUNCH_INTEGRATED_SERVER = "startIntegratedServer";
    //#elseif MC>=11802
    //$$ private static final String LAUNCH_INTEGRATED_SERVER = "startIntegratedServer(Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Function;ZLnet/minecraft/client/MinecraftClient$WorldLoadAction;)V";
    //#elseif MC>=11600
    //$$ private static final String LAUNCH_INTEGRATED_SERVER = "loadWorld(Ljava/lang/String;Lnet/minecraft/util/registry/DynamicRegistries$Impl;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/Minecraft$WorldSelectionType;)V";
    //#else
    private static final String LAUNCH_INTEGRATED_SERVER = "launchIntegratedServer";
    //#endif
    //#if MC>=12005
    //$$ private static final String STOP_INTEGRATED_SERVER = "disconnect(Lnet/minecraft/client/gui/screen/Screen;Z)V";
    //#elseif MC>=11600
    //$$ private static final String STOP_INTEGRATED_SERVER = "unloadWorld(Lnet/minecraft/client/gui/screen/Screen;)V";
    //#else
    private static final String STOP_INTEGRATED_SERVER = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V";
    //#endif

    @Inject(method = {
        LAUNCH_INTEGRATED_SERVER,
        //$$ // Forge patches on these versions add `boolean creating` to end of arguments
        //#if FORGE && MC>=11600 && MC<11900
        //#if MC>=11802
        //$$ "doLoadLevel(Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/Function;ZLnet/minecraft/client/Minecraft$ExperimentalDialogType;Z)V",
        //#elseif MC>=11700
        //$$ "doLoadLevel(Ljava/lang/String;Lnet/minecraft/core/RegistryAccess$RegistryHolder;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/Minecraft$ExperimentalDialogType;Z)V",
        //#else
        //$$ "loadWorld(Ljava/lang/String;Lnet/minecraft/util/registry/DynamicRegistries$Impl;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function4;ZLnet/minecraft/client/Minecraft$WorldSelectionType;Z)V",
        //#endif
        //#endif
    }, at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;integratedServer:Lnet/minecraft/server/integrated/IntegratedServer;", shift = At.Shift.AFTER))
    private void setIntegratedServerManager(CallbackInfo ci) {
        IntegratedServerExt ext = (IntegratedServerExt) this.integratedServer;
        assert ext != null;
        Essential.getInstance().getIntegratedServerManager().set(ext.getEssential$manager());
    }

    @Inject(method = STOP_INTEGRATED_SERVER, at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;integratedServer:Lnet/minecraft/server/integrated/IntegratedServer;", shift = At.Shift.AFTER))
    private void unsetIntegratedServerManager(CallbackInfo ci) {
        Essential.getInstance().getIntegratedServerManager().set((McIntegratedServerManager) null);
    }
}
