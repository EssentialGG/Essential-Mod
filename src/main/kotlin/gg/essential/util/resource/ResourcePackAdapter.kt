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

import gg.essential.util.identifier
import net.minecraft.client.resources.IResourcePack
import net.minecraft.util.ResourceLocation
import java.io.InputStream

//#if MC>=12005
//$$ import gg.essential.util.makePackInfo
//#endif

//#if MC>=11903
//$$ import net.minecraft.resource.InputSupplier
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

open class ResourcePackAdapter(val parent: IResourcePack) : IResourcePack {

    open fun mapToParent(path: String): FileMapper? = null

    private fun mapToParentPath(path: String): String =
        mapToParent(path)?.parentPath ?: path

    //#if MC>=11903
    //$$ override fun open(type: ResourceType, id: Identifier): InputSupplier<InputStream>? {
    //#elseif MC>=11500
    //$$ override fun getResourceStream(type: ResourcePackType, id: ResourceLocation): InputStream {
    //#else
    override fun getInputStream(id: ResourceLocation): InputStream {
    //#endif
        val mapper = mapToParent(id.resourcePath)
        val parentId = mapper?.let { identifier(id.resourceDomain, it.parentPath) } ?: id
        //#if MC>=11500
        //$$ val stream = parent.getResourceStream(type, parentId)
        //#else
        val stream = parent.getInputStream(parentId)
        //#endif
        //#if MC>=11903
        //$$ return if (stream == null || mapper == null) {
        //$$     stream
        //$$ } else {
        //$$     InputSupplier {
        //$$         mapper.map(stream.get())
        //$$     }
        //$$ }
        //#else
        return mapper?.map(stream) ?: stream
        //#endif
    }

    //#if MC<11903
    //#if MC>=11500
    //$$ override fun resourceExists(type: ResourcePackType, id: ResourceLocation): Boolean =
    //$$     parent.resourceExists(type, ResourceLocation(id.namespace, mapToParentPath(id.path)))
    //#else
    override fun resourceExists(id: ResourceLocation): Boolean =
        parent.resourceExists(ResourceLocation(id.resourceDomain, mapToParentPath(id.resourcePath)))
    //#endif
    //#endif

    //#if MC>=11500
    //$$ override fun <T : Any> getMetadata(serializer: IMetadataSectionSerializer<T>): T? = parent.getMetadata(serializer)
    //#else
    override fun <T : IMetadataSection> getPackMetadata(serializer: MetadataSerializer, name: String): T? =
        parent.getPackMetadata(serializer, mapToParentPath(name))
    //#endif

    //#if MC>=11500
    //$$ override fun getResourceNamespaces(type: ResourcePackType): Set<String> = parent.getResourceNamespaces(type)
    //#else
    override fun getResourceDomains(): Set<String> = parent.resourceDomains
    //#endif

    override fun getPackName(): String = parent.packName

    //#if MC>=11500
    //$$ override fun close() = parent.close()
    //$$
    //#if MC>=11903
    //$$ override fun findResources(type: ResourceType, namespace: String, path: String, consumer: ResourcePack.ResultConsumer) =
    //$$     parent.findResources(type, namespace, path, consumer)
    //#elseif MC>=11900
    //$$ override fun findResources(type: ResourceType, namespace: String, path: String, filter: Predicate<Identifier>): Collection<Identifier> =
    //$$     parent.findResources(type, namespace, path, filter)
    //#else
    //$$ override fun getAllResourceLocations(type: ResourcePackType, namespace: String, path: String, depth: Int, filter: Predicate<String>): Collection<ResourceLocation> =
    //$$     parent.getAllResourceLocations(type, namespace, path, depth, filter)
    //#endif
    //$$
    //#if MC>=11903
    //$$ override fun openRoot(vararg segments: String): InputSupplier<InputStream>? = parent.openRoot(*segments)
    //#else
    //$$ override fun getRootResourceStream(path: String): InputStream? = parent.getRootResourceStream(path)
    //#endif
    //#else
    override fun getPackImage(): BufferedImage = parent.packImage
    //#endif

    interface FileMapper {
        val parentPath: String
        fun map(stream: InputStream): InputStream
    }

    class PathChange(override val parentPath: String) : FileMapper {
        override fun map(stream: InputStream): InputStream = stream
    }

    //#if MC>=12005
    //$$ val packInfo = makePackInfo("essential_adapter")
    //$$
    //$$ override fun getInfo() = packInfo
    //#endif
}
