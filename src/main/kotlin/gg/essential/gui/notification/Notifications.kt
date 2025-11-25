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
package gg.essential.gui.notification

import gg.essential.api.gui.NotificationType
import gg.essential.api.gui.NotificationBuilder
import gg.essential.elementa.dsl.childOf
import gg.essential.api.gui.Notifications
import gg.essential.api.gui.Slot
import gg.essential.config.EssentialConfig
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.gui.overlay.Layer
import gg.essential.gui.overlay.LayerPriority
import gg.essential.util.GuiUtil
import gg.essential.util.executor
import net.minecraft.client.Minecraft
import java.util.concurrent.CompletableFuture

object NotificationsImpl : Notifications, NotificationsManager {
    private const val MAXIMUM_NOTIFICATIONS = 3

    private val mc = Minecraft.getMinecraft()

    private var layer: Layer? = null

    /** @see [Layer.rendered] */
    private var rendered = true
        set(value) {
            field = value
            layer?.rendered = value
        }

    private var beforeFirstDraw = true

    /**
     * A queue of already-built [Notification]s that are waiting for enough space on the screen to be displayed.
     */
    private val notifications = mutableListOf<Notification>()

    /**
     * A queue of blocks which (typically) contain yet-to-be built notifications that are waiting for the first draw
     * to complete before either being displayed, or added to the standard [notifications] queue.
     */
    private val blockedNotifications = mutableListOf<() -> Unit>()
        get() = field.also { layer() } // this queue is flushed via an UpdateFunc, so need to make sure one's registered

    private fun layer(): Layer {
        layer?.let { return it }

        val layer = GuiUtil.addLayer(LayerPriority.Notifications).also { layer = it }
        layer.rendered = rendered
        layer.window.addUpdateFunc { _, _ ->
            if (beforeFirstDraw) {
                beforeFirstDraw = false
            }

            if (!blocked && blockedNotifications.isNotEmpty()) {
                flushBlockedNotifications()
            }

            if (layer.window.children.isEmpty() && blockedNotifications.isEmpty()) {
                GuiUtil.removeLayer(layer)
                this.layer = null
            }
        }
        return layer
    }

    override fun push(title: String, message: String) {
        push(title, message, 4f, action = {})
    }

    @Suppress("INAPPLICABLE_JVM_NAME") // https://youtrack.jetbrains.com/issue/KT-31420
    @JvmName("push") // different name for kotlin to avoid overload ambiguity, kotlin can just use push with default args
    override fun pushWithBuilder(title: String, message: String, configure: NotificationBuilder.() -> Unit) =
        push(title, message, configure = configure)

    override fun push(title: String, message: String, duration: Float) {
        push(title, message, duration, action = {})
    }

    @Suppress("INAPPLICABLE_JVM_NAME") // https://youtrack.jetbrains.com/issue/KT-31420
    @JvmName("push") // different name for kotlin to avoid overload ambiguity, kotlin can just use push with default args
    override fun pushWithAction(title: String, message: String, action: () -> Unit) {
        push(title, message, 4f, action)
    }

    @Suppress("INAPPLICABLE_JVM_NAME") // https://youtrack.jetbrains.com/issue/KT-31420
    @JvmName("push") // different name for kotlin to avoid overload ambiguity, kotlin can just use push with default args
    override fun pushWithDurationAndAction(title: String, message: String, duration: Float, action: () -> Unit) {
        push(title, message, duration, action, close = {})
    }

    @Suppress("INAPPLICABLE_JVM_NAME") // https://youtrack.jetbrains.com/issue/KT-31420
    @JvmName("push") // different name for kotlin to avoid overload ambiguity, kotlin can just use push with default args
    override fun pushWithDurationActionAndClose(title: String, message: String, duration: Float, action: () -> Unit, close: () -> Unit) {
        this.push(title, message, duration, action, close) {}
    }

    override fun push(
        title: String, message: String,
        duration: Float,
        action: () -> Unit,
        close: () -> Unit,
        configure: NotificationBuilder.() -> Unit
    ) {
        // `push` is supposed to be thread-safe (or at least that's how we're using it), so we need to make sure we do
        // the actual gui changes on the main thread.
        mc.executor.execute {
            pushSync(title, message, duration, action, close, configure, false)
        }
    }

    override fun pushPersistentToast(
        title: String,
        message: String,
        action: () -> Unit,
        close: () -> Unit,
        configure: NotificationBuilder.() -> Unit
    ) {
        // `push` is supposed to be thread-safe (or at least that's how we're using it), so we need to make sure we do
        // the actual gui changes on the main thread.
        mc.executor.execute {
            pushSync(title, message, 0f, action, close, configure, true)
        }
    }

    private fun pushSync(
        title: String,
        message: String,
        duration: Float,
        action: () -> Unit,
        close: () -> Unit,
        configure: NotificationBuilder.() -> Unit,
        persistent: Boolean,
    ) {
        if (EssentialConfig.disableAllNotifications || EssentialConfig.streamerMode) return

        val builder = NotificationBuilderImpl(title, message, duration, action, close, persistent = persistent)

        // If a notification is built before the game has finished loading on newer versions of Minecraft,
        // and that notification has a component which has a constraint derived from the width of text, the constraint's
        // value won't be correct as fonts have not been loaded yet.
        if (blocked) {
            blockedNotifications.add { pushSync(builder.apply(configure).build()) }
            return
        }

        val notification = builder.apply(configure).build()
        pushSync(notification)
    }

    private fun pushSync(notification: Notification) {
        if (notification.uniqueId != null && hasNotification(notification.uniqueId)) {
            return
        }

        val window = layer().window
        if (window.children.size < MAXIMUM_NOTIFICATIONS) {
            notification childOf window
            notification.animateIn()
        } else {
            notifications.add(notification)
        }
    }

    private fun addFromQueue() {
        val notification = notifications.removeFirstOrNull() ?: return

        notification childOf layer().window
        notification.animateIn()
    }

    private fun flushBlockedNotifications() {
        while (blockedNotifications.isNotEmpty()) {
            blockedNotifications.removeFirstOrNull()?.invoke()
        }
    }

    private val blocked: Boolean
        //#if MC>=11600
        //$$ get() = beforeFirstDraw || mc.loadingGui != null
        //#else
        get() = beforeFirstDraw
    //#endif

    class NotificationBuilderImpl(
        val title: String,
        val message: String,
        override var duration: Float,
        override var onAction: () -> Unit,
        override var onClose: () -> Unit,
        override var timerEnabled: State<Boolean> = BasicState(true),
        override var trimTitle: Boolean = false,
        override var trimMessage: Boolean = false,
        val persistent: Boolean,
        override var uniqueId: Any? = null,
        override var type: NotificationType = NotificationType.GENERAL,
    ) : NotificationBuilder {
        val components = mutableMapOf<Slot, UIComponent>()

        override var elementaVersion = ElementaVersion.V6

        private val dismissFuture = CompletableFuture<Void?>()
        private val instantDismissFuture = CompletableFuture<Void?>()

        override val dismissNotification: () -> Unit = {
            dismissFuture.complete(null)
        }

        override val dismissNotificationInstantly: () -> Unit = {
            instantDismissFuture.complete(null)
        }

        override fun withCustomComponent(slot: Slot, component: UIComponent) = apply {
            components[slot] = component
        }

        fun build() = Notification(
            text = message,
            title = title,
            duration = duration,
            onClick = onAction,
            timerEnabled = timerEnabled,
            onClosed = {
                addFromQueue()
                onClose()
            },
            trimTitle = trimTitle,
            trimMessage = trimMessage,
            dismissNotification = dismissFuture,
            dismissNotificationInstantly = instantDismissFuture,
            persistent = persistent,
            components = components,
            type =  type,
            uniqueId = uniqueId,
        )
    }

    override fun hide() {
        rendered = false
    }

    override fun show() {
        rendered = true
    }

    override fun hasActiveNotifications(): Boolean {
        return layer?.window?.children?.isNotEmpty() ?: false
    }

    override fun removeNotificationById(id: Any) {
        notifications.removeIf { it.uniqueId == id }

        val window = layer?.window ?: return

        window.childrenOfType<Notification>()
            .filter { it.uniqueId == id }
            .forEach { it.dismissInstantly() }

        if (blockedNotifications.isNotEmpty()) {
            blockedNotifications.add { removeNotificationById(id) }
        }
    }

    private fun hasNotification(uniqueId: Any): Boolean {
        return notifications.any { it.uniqueId == uniqueId } || layer?.window?.childrenOfType<Notification>()?.any { it.uniqueId == uniqueId } ?: false
    }
}
