package com.dynamic.sdk.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Module.GenericNetwork
import com.dynamic.sdk.android.Module.NativeCurrency
import com.dynamic.sdk.android.UI.DynamicUI
import com.dynamic.sdk.android.core.ClientDebugProps
import com.dynamic.sdk.android.core.ClientProps
import com.dynamic.sdk.android.core.LoggerLevel
import com.dynamic.sdk.example.App.AppRootView
import com.dynamic.sdk.example.ui.theme.DynamicSDKExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val b3SepoliaNetwork = GenericNetwork(
            blockExplorerUrls = listOf(
                "https://sepolia-explorer.b3.fun" // ako nemaš tačan explorer, stavi "" ili ukloni kasnije
            ),
            chainId = 1993,          // Any -> Int (može i "1993")
            chainName = "B3 Sepolia",
            iconUrls = listOf(
                "https://icodrops.com/media/projects/covers/b3-fun_cover_1740116094.webp"
            ),
            lcdUrl = null,           // EVM uglavnom nema lcdUrl
            name = "B3 Sepolia",
            nameService = null,      // ako nema ENS-like registry
            nativeCurrency = NativeCurrency(
                name = "Ether",
                symbol = "ETH",
                decimals = 18
            ),
            networkId = 1993,        // Any -> Int/Long (može i "1993")
            privateCustomerRpcUrls = null,
            rpcUrls = listOf(
                "https://sepolia.b3.fun"
            ),
            vanityName = null
        )

        // Initialize Dynamic SDK
        val props = ClientProps(
            environmentId = "3e219b76-dcf1-40ab-aad6-652c4dfab4cc",
            appLogoUrl = "https://demo.dynamic.xyz/favicon-32x32.png",
            appName = "Dynamic Android Demo",
            redirectUrl = "dynamicandroiddemo://",
            appOrigin = "https://demo.dynamic.xyz",
            apiBaseUrl = "https://app.dynamicauth.com/api/v0",
            logLevel = LoggerLevel.DEBUG,
            evmNetworks = listOf(b3SepoliaNetwork)
        )
        DynamicSDK.initialize(props, applicationContext, this)
        setContent {
            DynamicSDKExampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Main app content
                        AppRootView()

                        // Dynamic SDK WebView overlay (shows when auth/profile is opened)
                        DynamicUI()
                    }
                }
            }
        }
    }
}

