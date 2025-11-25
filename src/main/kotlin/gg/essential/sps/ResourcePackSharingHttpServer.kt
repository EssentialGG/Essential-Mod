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
package gg.essential.sps

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import gg.essential.Essential
import gg.essential.mixins.ext.client.resource.ResourcePackWithPath
import gg.essential.sps.quic.jvm.LOCALHOST
import gg.essential.universal.UMinecraft
import gg.essential.util.Multithreading
import gg.essential.util.ResourceManagerUtil
import gg.essential.util.executor
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.IResourcePack
import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.HttpStatus
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes
import kotlin.io.path.relativeTo

object ResourcePackSharingHttpServer {
    private val spsManager = Essential.getInstance().connectionManager.spsManager

    private var server: HttpServer? = null

    private var packInfo: PackInfo? = null

    val port: Int?
        get() = server?.address?.port

    init {
        UMinecraft.getMinecraft().executor.execute {
            updateResourcePack()
        }

        ResourceManagerUtil.onResourceManagerReload {
            updateResourcePack()
        }
    }

    fun startServer() {
        val server = HttpServer.create(InetSocketAddress(LOCALHOST, 0), 0)
        server.createContext("/") {
            if (spsManager.localSession == null || !spsManager.isShareResourcePack) {
                it.sendResponseHeaders(HttpStatus.SC_FORBIDDEN, 0)
                it.responseBody.close()
                return@createContext
            }

            when (val packInfo = packInfo) {
                null -> {
                    it.sendResponseHeaders(HttpStatus.SC_NOT_FOUND, 0)
                    it.responseBody.close()
                }

                else -> {
                    try {
                        sendFile(packInfo.file, it)
                    } catch (e: Exception) {
                        e.printStackTrace() // Otherwise it will be eaten by the webserver
                        it.responseBody.close()
                    }
                }
            }

        }
        server.executor = Multithreading.pool
        server.start()
        this.server = server
    }

    fun stopServer() {
        server?.stop(0)
        server = null
    }

    private fun sendFile(file: Path, exchange: HttpExchange) {
        exchange.sendResponseHeaders(HttpStatus.SC_OK, file.fileSize())
        file.inputStream().use {
            it.copyTo(exchange.responseBody)
        }
        exchange.responseBody.close()
    }

    private fun getPrimaryResourcePack(): IResourcePack? {
        //#if MC<=11202
        val resourcePackRepository = Minecraft.getMinecraft().resourcePackRepository
        val repositoryEntries = resourcePackRepository.repositoryEntries
        //#else
        //$$ val resourcePackRepository = Minecraft.getInstance().resourcePackList
        //$$ val repositoryEntries = resourcePackRepository.enabledPacks
        //#endif
        if (repositoryEntries.isEmpty()) {
            return null
        }
        return repositoryEntries.last().resourcePack
    }

    fun onShareResourcePackEnable() {
        // Attempt to generate pack info when it is not set
        if (packInfo == null) {
            updateResourcePack()
        }
    }

    private fun updateResourcePack() {
        // If the user is not actively sharing their resource pack, reset the packInfo
        // and cancel computing it to avoid needlessly zipping
        if (spsManager.localSession == null || !spsManager.isShareResourcePack) {
            packInfo?.takeIf { it.isTemp }?.file?.deleteIfExists()
            packInfo = null
            spsManager.updateResourcePack(null)
            return
        }
        recomputePrimaryPack()
        spsManager.updateResourcePack(packInfo)
    }

    private fun recomputePrimaryPack() {
        // Cleanup previous temp zip file if needed
        val packInfo = packInfo
        if (packInfo?.isTemp == true) {
            packInfo.file.deleteIfExists()
        }

        val resourcePack = getPrimaryResourcePack()
        if (resourcePack == null) {
            this.packInfo = null
            return
        }

        if (resourcePack is ResourcePackWithPath) {
            val file = resourcePack.`essential$path`
            if (file == null) {
                this.packInfo = null
                return
            }
            if (file.isDirectory()) {
                // Zip the resource pack to send
                val tempFile = Files.createTempFile("resource-pack", ".zip")
                Essential.logger.info("Zipping host resource pack to $tempFile")

                zipFolder(file, tempFile)
                this.packInfo = PackInfo(tempFile, getChecksum(tempFile), true)
            } else if (file.exists()) {
                this.packInfo = PackInfo(file, getChecksum(file), false)
            } else {
                this.packInfo = null
                Essential.logger.error("Unable to determine primary resource pack: ${resourcePack.packName} ${resourcePack.javaClass.name} $file")
            }

        } else {
            this.packInfo = null
            Essential.logger.error("Unable to determine primary resource pack: ${resourcePack.packName} ${resourcePack.javaClass.name}")
        }
    }

    private fun getChecksum(file: Path): String {
        return DigestUtils.sha1Hex(file.readBytes())
    }

    data class PackInfo(val file: Path, val checksum: String, val isTemp: Boolean)

    /** Adapted from https://stackoverflow.com/questions/51833423/how-to-zip-the-content-of-a-directory-in-java */
    @Throws(java.lang.Exception::class)
    private fun zipFolder(srcFolder: Path, destZipFile: Path) {
        destZipFile.outputStream().use { fileWriter ->
            ZipOutputStream(fileWriter).use { zip ->
                addFolderToZip(
                    srcFolder,
                    srcFolder,
                    zip
                )
            }
        }
    }

    @Throws(java.lang.Exception::class)
    private fun addFileToZip(rootPath: Path, srcFile: Path, zip: ZipOutputStream) {
        if (srcFile.isDirectory()) {
            addFolderToZip(rootPath, srcFile, zip)
        } else {
            val buf = ByteArray(1024)
            var len: Int
            srcFile.inputStream().use { `in` ->
                val name = srcFile.relativeTo(rootPath).toString()
                zip.putNextEntry(ZipEntry(name))
                while (`in`.read(buf).also { len = it } > 0) {
                    zip.write(buf, 0, len)
                }
            }
        }
    }

    @Throws(java.lang.Exception::class)
    private fun addFolderToZip(rootPath: Path, srcFolder: Path, zip: ZipOutputStream) {
        for (fileName in srcFolder.listDirectoryEntries()) {
            addFileToZip(rootPath, fileName, zip)
        }
    }
}