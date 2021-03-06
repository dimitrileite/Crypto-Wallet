package com.blockchain.swap.nabu.datamanagers

import com.blockchain.swap.nabu.Authenticator
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.models.tokenresponse.NabuSessionTokenResponse
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

internal class NabuAuthenticator(
    private val nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager
) : Authenticator {

    override fun <T> authenticateSingle(singleFunction: (Single<NabuSessionTokenResponse>) -> Single<T>): Single<T> =
        nabuToken.fetchNabuToken()
            .map { nabuDataManager.currentToken(it) }
            .flatMap { singleFunction(it) }

    override fun <T> authenticate(singleFunction: (NabuSessionTokenResponse) -> Single<T>): Single<T> =
        nabuToken.fetchNabuToken()
            .flatMap { tokenResponse ->
                nabuDataManager.authenticate(tokenResponse, singleFunction)
                    .subscribeOn(Schedulers.io())
            }

    override fun invalidateToken() {
        nabuDataManager.invalidateToken()
    }
}
