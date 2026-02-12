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
package gg.essential.util.resource

import gg.essential.network.connectionmanager.cosmetics.AssetLoader
import net.minecraft.client.resources.IResourcePack
import net.minecraft.util.ResourceLocation
import java.io.InputStream
import java.util.concurrent.CompletableFuture

//#if MC>=12005
//$$ import gg.essential.util.makePackInfo
//#endif

//#if MC>=11903
//$$ import net.minecraft.resource.InputSupplier
//#else
import java.io.FileNotFoundException
//#endif

//#if MC>=11500
//$$ import net.minecraft.resources.ResourcePackType
//$$ import net.minecraft.resources.data.IMetadataSectionSerializer
//$$ import java.util.function.Predicate
//#else
import net.minecraft.client.resources.data.IMetadataSection
import net.minecraft.client.resources.data.MetadataSerializer
import java.awt.image.BufferedImage
//#endif

/**
 * Resource pack which provides [gg.essential.mod.EssentialAsset]s to the vanilla resource pack system.
 *
 * The assets need to be registered with the [AssetLoader] (even if just at [passive][AssetLoader.Priority.Passive]
 * priority), and they need to match one of the declared [pathPatterns].
 *
 * If an asset is not yet loaded by the time it is requested from the resource pack, its priority will be increased to
 * [Blocking][AssetLoader.Priority.Blocking].
 */
class EssentialAssetResourcePack(private val assetLoader: AssetLoader) : IResourcePack {
    companion object {
        // Hex-encoded md5, sha1, or sha256
        private const val CHECKSUM = "([0-9a-f]{32}|[0-9a-f]{40}|[0-9a-f]{64})"
    }

    // Regexes to get the checksum from resource paths
    // Note: These must not be too general, as otherwise third-party mods may get confused when we return random data
    //       for paths they query and expect to get (or even not get) certain data from.
    private val pathPatterns = listOf(
        Regex("sounds/$CHECKSUM.ogg")
    )

    private fun lookup(path: String, priority: AssetLoader.Priority): CompletableFuture<ByteArray>? {
        for (pattern in pathPatterns) {
            val match = pattern.matchEntire(path) ?: continue
            return assetLoader.getKnownAsset(match.groupValues[1], priority)
        }
        return null
    }

    //#if MC>=11903
    //$$ override fun open(type: ResourceType, id: Identifier): InputSupplier<InputStream>? {
    //#elseif MC>=11500
    //$$ override fun getResourceStream(type: ResourcePackType, id: ResourceLocation): InputStream {
    //#else
    override fun getInputStream(id: ResourceLocation): InputStream {
    //#endif
        val path = id.resourcePath
        val future = lookup(path, AssetLoader.Priority.Blocking)
            //#if MC>=11903
            //$$ ?: return null
            //#else
            ?: throw FileNotFoundException(path)
            //#endif

        //#if MC>=11903
        //$$ return InputSupplier {
        //$$     future.join().inputStream()
        //$$ }
        //#else
        return future.join().inputStream()
        //#endif
    }

    //#if MC<11903
    //#if MC>=11500
    //$$ override fun resourceExists(type: ResourcePackType, id: ResourceLocation): Boolean =
    //#else
    override fun resourceExists(id: ResourceLocation): Boolean =
    //#endif
        lookup(id.resourcePath, AssetLoader.Priority.Passive) != null
    //#endif

    //#if MC>=11500
    //$$ override fun <T : Any> getMetadata(serializer: IMetadataSectionSerializer<T>): T? = null
    //#else
    override fun <T : IMetadataSection> getPackMetadata(serializer: MetadataSerializer, name: String): T? = null
    //#endif

    //#if MC>=11500
    //$$ override fun getResourceNamespaces(type: ResourcePackType): Set<String> =
    //#else
    override fun getResourceDomains(): Set<String> =
    //#endif
        setOf("essential")

    override fun getPackName(): String = "Essential Assets"

    //#if MC>=11500
    //$$ override fun close() {}
    //$$
    //#if MC>=11903
    //$$ override fun findResources(type: ResourceType, namespace: String, path: String, consumer: ResourcePack.ResultConsumer) {}
    //#elseif MC>=11900
    //$$ override fun findResources(type: ResourceType, namespace: String, path: String, filter: Predicate<Identifier>): Collection<Identifier> = emptyList()
    //#else
    //$$ override fun getAllResourceLocations(type: ResourcePackType, namespace: String, path: String, depth: Int, filter: Predicate<String>): Collection<ResourceLocation> = emptyList()
    //#endif
    //$$
    //#if MC>=11903
    //$$ override fun openRoot(vararg segments: String): InputSupplier<InputStream>? = null
    //#else
    //$$ override fun getRootResourceStream(path: String): InputStream? = null
    //#endif
    //#else
    override fun getPackImage(): BufferedImage = throw FileNotFoundException()
    //#endif

    //#if MC>=12005
    //$$ val packInfo = makePackInfo("essential_assets")
    //$$
    //$$ override fun getInfo() = packInfo
    //#endif
}
