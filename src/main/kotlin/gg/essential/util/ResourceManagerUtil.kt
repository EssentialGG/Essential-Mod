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
package gg.essential.util

import gg.essential.universal.UMinecraft
import net.minecraft.client.resources.IResource
import net.minecraft.client.resources.IResourceManager
import net.minecraft.client.resources.IResourceManagerReloadListener
import net.minecraft.util.ResourceLocation
import java.io.FileNotFoundException
import java.util.concurrent.CopyOnWriteArrayList

//#if MC>=11600
//$$ import java.util.concurrent.CompletableFuture
//$$ import net.minecraft.resources.IFutureReloadListener.IStage
//$$ import net.minecraft.profiler.IProfiler
//$$ import java.util.concurrent.Executor
//#endif

typealias Listener = () -> Unit

object ResourceManagerUtil : IResourceManagerReloadListener {
    private val listeners = CopyOnWriteArrayList<Listener>()

    fun onResourceManagerReload(listener: Listener) {
        listeners.add(listener)
    }

    fun getResource(location: ResourceLocation): IResource? {
        return try {
            UMinecraft.getMinecraft().resourceManager.getResource(location)
            //#if MC>=11900
            //$$    .orElse(null)
            //#endif
        } catch (e: FileNotFoundException) {
            null
        }
    }

    /**
     * We want to clear our caches when the resource manager is reloaded in-case a resource pack is controlling
     * the texture.
     */
    //#if MC>=12109
    //$$ override fun reload(
    //$$     store: ResourceReloader.Store,
    //$$     backgroundExecutor: Executor,
    //$$     stage: Synchronizer,
    //$$     gameExecutor: Executor,
    //$$ ): CompletableFuture<Void> {
    //#else
    //#if MC<=11202
    override fun onResourceManagerReload(ignored: IResourceManager) {
        //#else
        //$$ override fun reload(
        //$$      stage: IStage,
        //$$      resourceManager: IResourceManager?,
        //#if MC<12102
        //$$      preparationsProfiler: IProfiler?,
        //$$      reloadProfiler: IProfiler?,
        //#endif
        //$$      backgroundExecutor: Executor?,
        //$$      gameExecutor: Executor?,
        //$$  ): CompletableFuture<Void?> {
        //#endif
    //#endif
        listeners.forEach(Listener::invoke)
        //#if MC>11202
        //$$ return stage.markCompleteAwaitingOthers(null)
        //#endif
    }
}