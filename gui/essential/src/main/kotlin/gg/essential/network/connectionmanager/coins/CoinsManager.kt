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
package gg.essential.network.connectionmanager.coins

import gg.essential.config.EssentialConfig
import gg.essential.connectionmanager.common.packet.checkout.ClientCheckoutClaimCoinsPacket
import gg.essential.connectionmanager.common.packet.checkout.ClientCheckoutCoinBundlePacket
import gg.essential.connectionmanager.common.packet.checkout.ClientCheckoutDynamicCoinBundlePacket
import gg.essential.connectionmanager.common.packet.checkout.ClientCheckoutPartnerCodeRequestDataPacket
import gg.essential.connectionmanager.common.packet.checkout.ServerCheckoutClaimCoinsResponsePacket
import gg.essential.connectionmanager.common.packet.checkout.ServerCheckoutPartnerCodeDataPacket
import gg.essential.connectionmanager.common.packet.checkout.ServerCheckoutUrlPacket
import gg.essential.connectionmanager.common.packet.coins.ClientCoinBundleOptionsPacket
import gg.essential.connectionmanager.common.packet.coins.ClientCoinsBalancePacket
import gg.essential.connectionmanager.common.packet.coins.ServerCoinBundleOptionsPacket
import gg.essential.connectionmanager.common.packet.coins.ServerCoinsBalancePacket
import gg.essential.connectionmanager.common.packet.currency.ClientCurrencyOptionsPacket
import gg.essential.connectionmanager.common.packet.currency.ServerCurrencyOptionsPacket
import gg.essential.connectionmanager.common.packet.response.ResponseActionPacket
import gg.essential.gui.elementa.state.v2.*
import gg.essential.gui.elementa.state.v2.combinators.*
import gg.essential.gui.elementa.state.v2.stateBy
import gg.essential.gui.notification.Notifications
import gg.essential.gui.notification.sendCheckoutFailedNotification
import gg.essential.gui.wardrobe.modals.CoinsReceivedModal
import gg.essential.network.CMConnection
import gg.essential.network.connectionmanager.NetworkedManager
import gg.essential.network.cosmetics.toMod
import gg.essential.network.registerPacketHandler
import gg.essential.util.*
import gg.essential.util.GuiEssentialPlatform.Companion.platform
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.URI
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

class CoinsManager(val connectionManager: CMConnection) : NetworkedManager {

    private val referenceHolder = ReferenceHolderImpl()
    private var currentCodeValidationJob: Job? = null

    // Actual data
    private val mutableCoins: MutableState<Int> = mutableStateOf(0)
    private val mutableCoinsSpent: MutableState<Int> = mutableStateOf(0)
    private val mutablePricing: MutableListState<CoinBundle> = mutableListStateOf()
    private val mutableCurrencies: MutableSetState<Currency> = mutableSetState(USD_CURRENCY)
    private val checkedCreatorCodes = mutableStateOf(mapOf<String, String?>())
    private var purchaseRequestInProgress = false
    // A state for an infra provided code that is used, when the user has nothing configured (but shouldn't be saved to config)
    private val creatorCodeNonPersistent = mutableStateOf("")

    val currencyRaw = EssentialConfig.coinsSelectedCurrencyState
    val creatorCodeConfigured = EssentialConfig.coinsPurchaseCreatorCodeState

    // This is in the coins manager because we could receive the purchase modal when we are not in the wardrobe, and it needs to be able to access this
    val areCoinsVisuallyFrozen = mutableStateOf(false)
    val isClaimingCoins = mutableStateOf(false)

    // Derived data
    val creatorCode = stateBy { (creatorCodeConfigured() ?: creatorCodeNonPersistent()).uppercase() }
    val coins: State<Int> = mutableCoins
    val coinsSpent: State<Int> = mutableCoinsSpent
    val pricing: ListState<CoinBundle> = mutablePricing
    val currencies: ListState<Currency> = mutableCurrencies.map { currencies ->
        currencies.sortedBy { it.currencyCode } // We should probably look at this ordering when we add more currencies
    }.toListState()
    val currency = stateBy {
        val currency = Currency.getInstance(currencyRaw())
        currencies().find { it.currencyCode == currency.currencyCode }
    }

    val creatorCodeName = stateBy { checkedCreatorCodes()[creatorCode()] ?: creatorCode().lowercase() }
    val creatorCodeValid = stateBy {
        val codesMap = checkedCreatorCodes()
        val code = creatorCode()
        if (codesMap.containsKey(code)) codesMap[code] != null else null
    }

    init {
        connectionManager.registerPacketHandler<ServerCoinsBalancePacket> { packet ->
            onBalanceUpdate(packet.coins, packet.coinsSpent, packet.topUpAmount)
        }
        connectionManager.registerPacketHandler<ServerCheckoutPartnerCodeDataPacket> { packet ->
            onCreatorCodeData(packet.partnerCode, packet.partnerName, packet.isPersist)
        }

        currency.onSetValue(referenceHolder) { refreshPricing(it) }
        // We only really verify the configured code, the non-persistent one is already infra provided
        creatorCodeConfigured.onSetValue(referenceHolder) { validateCode(it, debounce = true) }
    }

    override fun onConnected() {
        resetState()

        refreshCoins()
        refreshCurrencies()
        refreshPricing()
        validateCode(creatorCode.get(), debounce = false)
    }

    override fun resetState() {
        mutableCoins.set(0)
        mutableCoinsSpent.set(0)
    }

    fun purchaseBundle(bundle: CoinBundle, callback: (URI) -> Unit) {
        // If we are already waiting for a response, prevent spamming
        if (purchaseRequestInProgress) return
        purchaseRequestInProgress = true

        val creatorCode = if (creatorCodeValid.get() == true) creatorCode.get() else null

        val packet = if (bundle.isSpecificAmount) {
            ClientCheckoutDynamicCoinBundlePacket(bundle.numberOfCoins, bundle.currency, creatorCode)
        } else {
            ClientCheckoutCoinBundlePacket(bundle.id, bundle.currency, creatorCode)
        }
        connectionManager.send(packet) { maybeResponse ->
            purchaseRequestInProgress = false
            val responsePacket = maybeResponse.orElse(null)
            if (responsePacket is ServerCheckoutUrlPacket) {
                callback(URI(responsePacket.url))
            } else {
                sendCheckoutFailedNotification()
                // One of the failure reasons is sending an invalid code, so in the rare chance that that happened, we might as well re-validate in the background
                if (creatorCode != null) {
                    validateCode(creatorCode, debounce = false, force = true)
                }
            }
        }

    }

    fun refreshPricing() = refreshPricing(currency.get())

    private fun refreshCoins() {
        connectionManager.connectionScope.launch {
            connectionManager.call(ClientCoinsBalancePacket())
                .exponentialBackoff()
                .await<ServerCoinsBalancePacket>()
            // Updating the balance is done by the packet handler
        }
    }

    private fun refreshCurrencies() {
        connectionManager.connectionScope.launch {
            val response = connectionManager.call(ClientCurrencyOptionsPacket())
                .exponentialBackoff()
                .await<ServerCurrencyOptionsPacket>()
            mutableCurrencies.setAll(response.currencies)
        }
    }

    private fun validateCode(code: String?, debounce: Boolean, force: Boolean = false) {
        if (!connectionManager.isOpen) return // Return if connection is not open (since we use states from config, this can get triggered very early)
        if (code.isNullOrEmpty() || (!force && checkedCreatorCodes.get().contains(code))) return

        currentCodeValidationJob?.cancel()
        if (debounce) {
            currentCodeValidationJob = connectionManager.connectionScope.launch {
                delay(500.milliseconds)
                validateCode(code, false, force)
            }
            return
        }

        connectionManager.send(ClientCheckoutPartnerCodeRequestDataPacket(code)) { maybeResponse ->
            val responsePacket = maybeResponse.orElse(null)
            if (responsePacket is ServerCheckoutPartnerCodeDataPacket)
                return@send // Saving is handled in onCreatorCodeData (via packet handler)
            if (responsePacket is ResponseActionPacket && responsePacket.errorMessage == null) {
                checkedCreatorCodes.set { it + Pair(code, null) }
            } else {
                Notifications.push("Error validating creator code", "An unexpected error has occurred. Try again.")
            }
        }
    }

    private fun refreshPricing(currency: Currency?) {
        if (!connectionManager.isOpen) return // Return if connection is not open (since we use states from config, this can get triggered very early)
        if (currency == null) return
        connectionManager.send(ClientCoinBundleOptionsPacket(currency)) { maybeResponse ->
            val responsePacket = maybeResponse.orElse(null)
            if (responsePacket is ServerCoinBundleOptionsPacket) {
                mutablePricing.setAll(responsePacket.coinBundles.map { it.toMod(currency) })
            }
        }
    }

    private fun onBalanceUpdate(coins: Int, coinsSpent: Int, topUpAmount: Int?) {
        // Split behaviour if the packet was sent as a result of a successful coins purchase
        // If we are currently claiming coins, we might receive a top-up packet before we get the claim confirmation, so we handle all balance updates during claiming as claim top-ups.
        if (topUpAmount != null) {
            // If we aren't in the middle of a claim
            if (!isClaimingCoins.getUntracked()) {
                val manager = platform.createModalManager()
                manager.queueModal(
                    // Make the modal first as it disables coins animations
                    CoinsReceivedModal.fromPurchase(manager, this, topUpAmount)
                        .apply { mutableCoins.set(coins) }
                )
            } else {
                // Otherwise, this is a claim top-up packet, so we finish the claiming process
                isClaimingCoins.set(false)
                mutableCoins.set(coins)
            }
        } else {
            mutableCoins.set(coins)
        }
        mutableCoinsSpent.set(coinsSpent)
    }

    private fun onCreatorCodeData(code: String, name: String, persist: Boolean) {
        checkedCreatorCodes.set { it + Pair(code, name) }
        if (persist) {
            // This write is redundant if the user requested this validation, but since it's the same value anyway it doesn't matter
            // This simplifies the packet, so we don't need another boolean to know if the data was user requested...
            creatorCodeConfigured.set(code)
        } else {
            creatorCodeNonPersistent.set(code)
        }
    }

    fun tryClaimingWelcomeCoins() {
        // We only try to claim if they don't have coins and if they haven't spent any coins yet. (Basically new user)
        if (coins.get() != 0 || coinsSpent.get() != 0) return

        isClaimingCoins.set(true)
        // Freeze coins until we get a response to prevent the balance packet we receive as a response from animating them
        areCoinsVisuallyFrozen.set(true)

        connectionManager.send(ClientCheckoutClaimCoinsPacket("WARDROBE_REFRESH")) { optionalPacket ->
            val packet = optionalPacket.orElse(null)
            if (packet is ServerCheckoutClaimCoinsResponsePacket) {

                // The modal will also unfreeze coins when it's dismissed
                // Alongside this response, infra sends down a top-up balance packet too, it's handler will set isClaimingCoins back to false
                val manager = platform.createModalManager()
                manager.queueModal(CoinsReceivedModal.fromCoinClaim(manager, this, packet.coinsClaimed))

                return@send // Return if successful
            } else if (packet !is ResponseActionPacket || packet.errorMessage == null) {
                // Invalid packet or missing errorMessage means infra error
                LOGGER.error("ClientCheckoutClaimCoinsPacket gave invalid response!")
            }
            areCoinsVisuallyFrozen.set(false) // Unfreeze if unsuccessful
            isClaimingCoins.set(false)
        }
    }

    companion object {
        private  val LOGGER = LoggerFactory.getLogger(CoinsManager::class.java)
        val COIN_FORMAT = DecimalFormat("#,###", DecimalFormatSymbols(Locale.US))
    }

}
