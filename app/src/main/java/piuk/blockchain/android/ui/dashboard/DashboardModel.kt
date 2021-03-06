package piuk.blockchain.android.ui.dashboard

import androidx.annotation.VisibleForTesting
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.percentageDelta
import info.blockchain.balance.sum
import info.blockchain.balance.toFiat
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import piuk.blockchain.android.coincore.BalanceFilter
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.android.ui.base.mvi.MviState
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

class AssetMap(private val map: Map<CryptoCurrency, AssetState>) : Map<CryptoCurrency, AssetState> by map {
    override operator fun get(key: CryptoCurrency): AssetState {
        return map.getOrElse(key) { throw IllegalArgumentException("$key is not a known CryptoCurrency") }
    }

    // TODO: This is horrendously inefficient. Fix it!
    fun copy(): AssetMap {
        val assets = toMutableMap()
        return AssetMap(assets)
    }

    fun copy(patchBalance: CryptoValue): AssetMap {
        val assets = toMutableMap()
        val value = get(patchBalance.currency).copy(cryptoBalance = patchBalance)
        assets[patchBalance.currency] = value
        return AssetMap(assets)
    }

    fun copy(patchAsset: AssetState): AssetMap {
        val assets = toMutableMap()
        assets[patchAsset.currency] = patchAsset
        return AssetMap(assets)
    }

    fun reset(): AssetMap {
        val assets = toMutableMap()
        map.values.forEach { assets[it.currency] = it.reset() }
        return AssetMap(assets)
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
fun mapOfAssets(vararg pairs: Pair<CryptoCurrency, AssetState>) = AssetMap(mapOf(*pairs))

interface DashboardItem

interface BalanceState : DashboardItem {
    val isLoading: Boolean
    val fiatBalance: FiatValue?
    val delta: Pair<FiatValue, Double>?

    operator fun get(currency: CryptoCurrency): AssetState
}

enum class PromoSheet {
    PROMO_STX_CAMPAIGN_INTO,
    PROMO_STX_CAMPAIGN_COMPLETE,
}

data class DashboardState(
    val assets: AssetMap = mapOfAssets(
        CryptoCurrency.BTC to AssetState(CryptoCurrency.BTC),
        CryptoCurrency.BCH to AssetState(CryptoCurrency.BCH),
        CryptoCurrency.ETHER to AssetState(CryptoCurrency.ETHER),
        CryptoCurrency.XLM to AssetState(CryptoCurrency.XLM),
        CryptoCurrency.PAX to AssetState(CryptoCurrency.PAX)
    ),
    val showAssetSheetFor: CryptoCurrency? = null,
    val showPromoSheet: PromoSheet? = null,
    val announcement: AnnouncementCard? = null

) : MviState, BalanceState {

    // If ALL the assets are refreshing, then report true. Else false
    override val isLoading: Boolean by unsafeLazy {
        assets.values.all { it.isLoading }
    }

    override val fiatBalance: FiatValue? by unsafeLazy {
        assets.values
            .filter { !it.isLoading && it.fiatBalance != null }
            .map { it.fiatBalance!! }
            .sum()
    }

    private val fiatBalance24h: FiatValue? by unsafeLazy {
        assets.values
            .filter { !it.isLoading && it.fiatBalance24h != null }
            .map { it.fiatBalance24h!! }
            .sum()
    }

    override val delta: Pair<FiatValue, Double>? by unsafeLazy {
        val current = fiatBalance
        val old = fiatBalance24h

        if (current != null && old != null) {
            Pair(current - old, current.percentageDelta(old))
        } else {
            null
        }
    }

    override operator fun get(currency: CryptoCurrency): AssetState =
        assets[currency]
}

data class AssetState(
    val currency: CryptoCurrency,
    val cryptoBalance: CryptoValue? = null,
    val price: FiatValue? = null,
    val price24h: FiatValue? = null,
    val priceTrend: List<Float> = emptyList(),
    val hasBalanceError: Boolean = false
) : DashboardItem {
    val fiatBalance: FiatValue? by unsafeLazy {
        price?.let { cryptoBalance?.toFiat(it) ?: FiatValue.zero(it.currencyCode) }
    }

    val fiatBalance24h: FiatValue? by unsafeLazy {
        price24h?.let { cryptoBalance?.toFiat(it) ?: FiatValue.zero(it.currencyCode) }
    }

    val priceDelta: Double by unsafeLazy { price.percentageDelta(price24h) }

    val isLoading: Boolean by unsafeLazy {
        cryptoBalance == null || price == null || price24h == null
    }

    fun reset(): AssetState = AssetState(currency)
}

class DashboardModel(
    initialState: DashboardState,
    mainScheduler: Scheduler,
    private val interactor: DashboardInteractor
) : MviModel<DashboardState, DashboardIntent>(initialState, mainScheduler) {

    override fun performAction(intent: DashboardIntent): Disposable? {
        Timber.d("***> performAction: ${intent.javaClass.simpleName}")

        return when (intent) {
            is RefreshAllIntent -> {
                interactor.refreshBalances(this, BalanceFilter.Total)
            }
            is BalanceUpdate -> {
                process(RefreshPrices(intent.cryptoCurrency))
                null
            }
            is RefreshPrices -> interactor.refreshPrices(this, intent.cryptoCurrency)
            is PriceUpdate -> interactor.refreshPriceHistory(this, intent.cryptoCurrency)
            is BalanceUpdateError,
            is PriceHistoryUpdate,
            is ClearAnnouncement,
            is ShowAnnouncement,
            is ShowAssetDetails,
            is ShowPromoSheet,
            is ClearBottomSheet -> null
        }
    }

    override fun onScanLoopError(t: Throwable) { Timber.e("***> Scan loop failed: $t") }
}
