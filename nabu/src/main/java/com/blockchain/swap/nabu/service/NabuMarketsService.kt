package com.blockchain.swap.nabu.service

import com.blockchain.morph.CoinPair
import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.api.trade.FailureReasonJson
import com.blockchain.swap.nabu.api.nabu.NabuMarkets
import com.blockchain.swap.nabu.api.nabu.NabuTransaction
import com.blockchain.swap.nabu.api.trade.PeriodicLimit
import com.blockchain.swap.nabu.api.trade.TradeFailureJson
import com.blockchain.swap.nabu.api.trade.TradeJson
import com.blockchain.swap.nabu.api.trade.TradeRequest
import com.blockchain.swap.nabu.api.Value
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.withMajorValue
import io.reactivex.Completable
import io.reactivex.Single

class NabuMarketsService internal constructor(
    private val nabuMarkets: NabuMarkets,
    private val authenticator: Authenticator
) : TradeLimitService {

    fun getTradingConfig(tradingPair: CoinPair): Single<TradingConfig> {
        return authenticator.authenticate {
            nabuMarkets.getTradingConfig(
                tradingPair.pairCodeUpper,
                it.authHeader
            ).map {
                TradingConfig(
                    minOrderSize = CryptoValue.fromMajor(
                        tradingPair.from,
                        it.minOrderSize
                    )
                )
            }
        }
    }

    override fun getTradesLimits(fiatCurrency: String): Single<FiatTradesLimits> {
        return authenticator.authenticate {
            nabuMarkets.getTradesLimits(
                fiatCurrency,
                it.authHeader
            )
        }.map {
            FiatTradesLimits(
                minOrder = FiatValue.fromMajor(it.currency, it.minOrder),
                maxOrder = FiatValue.fromMajor(it.currency, it.maxOrder),
                maxPossibleOrder = FiatValue.fromMajor(it.currency, it.maxPossibleOrder),
                daily = it.daily.toFiat(it.currency),
                weekly = it.weekly.toFiat(it.currency),
                annual = it.annual.toFiat(it.currency)
            )
        }
    }

    fun executeTrade(
        tradeRequest: TradeRequest
    ): Single<NabuTransaction> {
        return authenticator.authenticate {
            nabuMarkets.executeTrade(tradeRequest, it.authHeader)
        }.map(TradeJson::mapTrade)
    }

    fun putTradeFailureReason(
        tradeRequestId: String,
        txHash: String?,
        message: String?
    ): Completable {
        return authenticator.authenticateCompletable {
            nabuMarkets.putTradeFailureReason(
                tradeRequestId,
                TradeFailureJson(
                    txHash = txHash,
                    failureReason = message?.let(::FailureReasonJson)
                ),
                it.authHeader
            )
        }
    }

    fun getTrades(userFiatCurrency: String): Single<List<NabuTransaction>> {
        return authenticator.authenticate {
            nabuMarkets.getTrades(userFiatCurrency, it.authHeader)
        }.map { list ->
            list.mapNotNull(TradeJson::mapTrade)
        }
    }
}

private fun PeriodicLimit?.toFiat(currencyCode: String) =
    if (this == null) FiatPeriodicLimit(
        null, null, null
    ) else {
        FiatPeriodicLimit(
            limit = limit?.let { FiatValue.fromMajor(currencyCode, it) },
            available = available?.let { FiatValue.fromMajor(currencyCode, it) },
            used = used?.let { FiatValue.fromMajor(currencyCode, it) }
        )
    }

private fun TradeJson.mapTrade(): NabuTransaction? {
    val coinPair = CoinPair.fromPairCodeOrNull(this.pair.replace("-", "_")) ?: return null

    return NabuTransaction(
        id = this.id,
        createdAt = this.createdAt,
        pair = coinPair,
        fee = this.withdrawalFee.toCryptoValue(),
        fiatValue = this.fiatValue.toFiatValue(),
        refundAddress = this.refundAddress,
        depositAddress = this.depositAddress,
        deposit = this.deposit?.toCryptoValue() ?: CryptoValue.zero(coinPair.from),
        withdrawalAddress = this.withdrawalAddress,
        withdrawal = this.withdrawal?.toCryptoValue() ?: CryptoValue.zero(coinPair.to),
        state = this.state,
        hashOut = this.withdrawalTxHash,
        depositTextMemo = this.depositMemo
    )
}

private fun Value.toCryptoValue(): CryptoValue =
    CryptoCurrency.fromSymbolOrThrow(symbol).withMajorValue(value)

private fun Value.toFiatValue(): FiatValue =
    FiatValue.fromMajor(this.symbol, this.value)
