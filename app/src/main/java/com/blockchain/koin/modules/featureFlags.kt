package com.blockchain.koin.modules

import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.featureFlag
import org.koin.dsl.module.applicationContext

val featureFlagsModule = applicationContext {
    factory("ff_notify_coinify_users_to_kyc") {
        get<RemoteConfig>().featureFlag("android_notify_coinify_users_to_kyc")
    }

    factory("ff_sunriver_has_large_backlog") {
        get<RemoteConfig>().featureFlag("sunriver_has_large_backlog")
    }

    factory("ff_pit_linking") {
        get<RemoteConfig>().featureFlag("pit_linking_enabled")
    }

    factory("ff_pit_announcement") {
        get<RemoteConfig>().featureFlag("pit_show_announcement")
    }

    factory("ff_sms_verification") {
        get<RemoteConfig>().featureFlag("android_sms_verification")
    }

    factory("sunriver") {
        get<RemoteConfig>().featureFlag("android_sunriver_airdrop_enabled")
    }
}