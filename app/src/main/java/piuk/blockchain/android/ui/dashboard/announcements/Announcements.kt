package piuk.blockchain.android.ui.dashboard.announcements

import androidx.annotation.VisibleForTesting
import piuk.blockchain.android.campaign.CampaignType
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import timber.log.Timber

interface AnnouncementHost {
    val disposables: CompositeDisposable

    fun showAnnouncementCard(card: AnnouncementCard)
    fun dismissAnnouncementCard()

    // Actions
    fun startKyc(campaignType: CampaignType)
    fun startSwap(swapTarget: CryptoCurrency = CryptoCurrency.ETHER)
    fun startBuySell()
    fun startPitLinking()
    fun startFundsBackup()
    fun startSetup2Fa()
    fun startVerifyEmail()
    fun startEnableFingerprintLogin()
    fun startIntroTourGuide()
    fun startTransferCrypto()
    fun startBlockstackIntro()
}

abstract class AnnouncementRule(private val dismissRecorder: DismissRecorder) {

    protected val dismissEntry by lazy { dismissRecorder[dismissKey] }

    abstract val dismissKey: String
    abstract val name: String

    abstract fun shouldShow(): Single<Boolean>
    abstract fun show(host: AnnouncementHost)
    fun isDismissed(): Boolean = dismissEntry.isDismissed
}

class AnnouncementList(
    private val mainScheduler: Scheduler,
    private val orderAdapter: AnnouncementConfigAdapter,
    private val availableAnnouncements: List<AnnouncementRule>,
    private val dismissRecorder: DismissRecorder
) {
    fun checkLatest(host: AnnouncementHost, disposables: CompositeDisposable) {
        host.dismissAnnouncementCard()

        disposables += showNextAnnouncement(host)
            .subscribeBy(onError = Timber::e)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun buildAnnouncementList(order: List<String>): List<AnnouncementRule> {
        val r = order.mapNotNull { availableAnnouncements.find(it) }
        return r
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun showNextAnnouncement(host: AnnouncementHost): Maybe<AnnouncementRule> =
        getNextAnnouncement()
            .observeOn(mainScheduler)
            .doOnSuccess { it.show(host) }

    private fun getNextAnnouncement(): Maybe<AnnouncementRule> =
        orderAdapter.announcementConfig
            .doOnSuccess { dismissRecorder.setPeriod(it.interval) }
            .map { buildAnnouncementList(it.order) }
            .flattenAsObservable { it }
            .concatMap { a ->
                Observable.defer {
                    a.shouldShow()
                        .filter { it }
                        .map { a }
                        .toObservable()
                }
            }
            .firstElement()

    internal fun dismissKeys(): List<String> = availableAnnouncements.map { it.dismissKey }

    private fun List<AnnouncementRule>.find(name: String): AnnouncementRule? =
        this.find { it.name == name }
}
