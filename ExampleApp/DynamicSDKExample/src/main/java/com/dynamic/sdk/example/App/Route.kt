package com.dynamic.sdk.example.App

sealed class Route(val route: String) {
    object Splash : Route("splash")
    object Login : Route("login")
    object Home : Route("home")
    object MfaDevices : Route("mfa_devices")
    object MfaAddDevice : Route("mfa_add_device")
    object MfaRecoveryCodes : Route("mfa_recovery_codes")
    object Passkeys : Route("passkeys")
    object Delegation : Route("delegation")
    object ProjectSettings : Route("project_settings")
    object CreatePasswordWallet : Route("create_password_wallet")

    // Wallet routes with address parameter
    object WalletDetails : Route("wallet_details/{address}") {
        fun createRoute(address: String) = "wallet_details/$address"
    }
    object SignMessage : Route("sign_message/{address}") {
        fun createRoute(address: String) = "sign_message/$address"
    }
    object SwitchNetwork : Route("switch_network/{address}") {
        fun createRoute(address: String) = "switch_network/$address"
    }
    object CustomBalance : Route("custom_balance/{address}") {
        fun createRoute(address: String) = "custom_balance/$address"
    }

    // EVM routes
    object EvmSignTransaction : Route("evm_sign_transaction/{address}") {
        fun createRoute(address: String) = "evm_sign_transaction/$address"
    }
    object EvmSendTransaction : Route("evm_send_transaction/{address}") {
        fun createRoute(address: String) = "evm_send_transaction/$address"
    }
    object EvmSignTypedData : Route("evm_sign_typed_data/{address}") {
        fun createRoute(address: String) = "evm_sign_typed_data/$address"
    }
    object EvmWriteContract : Route("evm_write_contract/{address}") {
        fun createRoute(address: String) = "evm_write_contract/$address"
    }
    object EvmSendErc20 : Route("evm_send_erc20/{address}") {
        fun createRoute(address: String) = "evm_send_erc20/$address"
    }

    // Wallet password route
    object WalletPassword : Route("wallet_password/{address}") {
        fun createRoute(address: String) = "wallet_password/$address"
    }

    // Solana routes
    object SolanaSendTransaction : Route("solana_send_transaction/{address}") {
        fun createRoute(address: String) = "solana_send_transaction/$address"
    }
    object SolanaSendToken : Route("solana_send_token/{address}") {
        fun createRoute(address: String) = "solana_send_token/$address"
    }

    // SUI routes
    object SuiSignMessage : Route("sui_sign_message/{address}") {
        fun createRoute(address: String) = "sui_sign_message/$address"
    }
    object SuiSignTransaction : Route("sui_sign_transaction/{address}") {
        fun createRoute(address: String) = "sui_sign_transaction/$address"
    }
    object SuiSendTransaction : Route("sui_send_transaction/{address}") {
        fun createRoute(address: String) = "sui_send_transaction/$address"
    }
}
