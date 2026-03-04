package com.dynamic.sdk.example.App

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.BaseWallet
import com.dynamic.sdk.example.Screens.*
import com.dynamic.sdk.example.Screens.Wallet.*

@Composable
fun AppRootView() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Splash.route
    ) {
        composable(Route.Splash.route) {
            SplashScreen(
                onNavigateToLogin = { navController.navigate(Route.Login.route) },
                onNavigateToHome = { navController.navigate(Route.Home.route) }
            )
        }
        composable(Route.Login.route) {
            LoginScreen(
                onNavigateToHome = { navController.navigate(Route.Home.route) }
            )
        }
        composable(Route.Home.route) {
            HomeScreen(
                onNavigateToLogin = {
                    navController.navigate(Route.Login.route) {
                        popUpTo(Route.Home.route) { inclusive = true }
                    }
                },
                onNavigateToWalletDetails = { address ->
                    navController.navigate(Route.WalletDetails.createRoute(address))
                },
                onNavigateToMfaDevices = {
                    navController.navigate(Route.MfaDevices.route)
                },
                onNavigateToPasskeys = {
                    navController.navigate(Route.Passkeys.route)
                },
                onNavigateToProjectSettings = {
                    navController.navigate(Route.ProjectSettings.route)
                },
                onNavigateToCreatePasswordWallet = {
                    navController.navigate(Route.CreatePasswordWallet.route)
                }
            )
        }

        // MFA Devices Screen
        composable(Route.MfaDevices.route) {
            MfaDevicesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddDevice = {
                    navController.navigate(Route.MfaAddDevice.route)
                },
                onNavigateToRecoveryCodes = {
                    navController.navigate(Route.MfaRecoveryCodes.route)
                }
            )
        }

        // MFA Add Device Screen
        composable(Route.MfaAddDevice.route) {
            MfaAddDeviceScreen(
                onNavigateBack = { navController.popBackStack() },
                onFinished = {
                    navController.popBackStack()
                }
            )
        }

        // MFA Recovery Codes Screen
        composable(Route.MfaRecoveryCodes.route) {
            MfaRecoveryCodesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Passkeys Screen
        composable(Route.Passkeys.route) {
            PasskeysScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Project Settings Screen
        composable(Route.ProjectSettings.route) {
            ProjectSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Create Password Wallet Screen
        composable(Route.CreatePasswordWallet.route) {
            CreatePasswordWalletScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Delegation Screen
        composable(Route.Delegation.route) {
            DelegationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Wallet Details Screen
        composable(
            route = Route.WalletDetails.route,
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: return@composable
            val wallet = remember(address) { findWalletByAddress(address) }

            wallet?.let { w ->
                WalletDetailsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    wallet = w,
                    onNavigateToSignMessage = {
                        navController.navigate(Route.SignMessage.createRoute(address))
                    },
                    onNavigateToSwitchNetwork = {
                        navController.navigate(Route.SwitchNetwork.createRoute(address))
                    },
                    onNavigateToCustomBalance = {
                        navController.navigate(Route.CustomBalance.createRoute(address))
                    },
                    onNavigateToEvmSignTransaction = {
                        navController.navigate(Route.EvmSignTransaction.createRoute(address))
                    },
                    onNavigateToEvmSendTransaction = {
                        navController.navigate(Route.EvmSendTransaction.createRoute(address))
                    },
                    onNavigateToEvmSignTypedData = {
                        navController.navigate(Route.EvmSignTypedData.createRoute(address))
                    },
                    onNavigateToEvmWriteContract = {
                        navController.navigate(Route.EvmWriteContract.createRoute(address))
                    },
                    onNavigateToEvmSendErc20 = {
                        navController.navigate(Route.EvmSendErc20.createRoute(address))
                    },
                    onNavigateToSolanaSendTransaction = {
                        navController.navigate(Route.SolanaSendTransaction.createRoute(address))
                    },
                    onNavigateToSolanaSendToken = {
                        navController.navigate(Route.SolanaSendToken.createRoute(address))
                    },
                    onNavigateToWalletPassword = {
                        navController.navigate(Route.WalletPassword.createRoute(address))
                    }
                )
            }
        }

        // Sign Message Screen
        composable(
            route = Route.SignMessage.route,
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: return@composable
            val wallet = remember(address) { findWalletByAddress(address) }

            wallet?.let { w ->
                SignMessageScreen(
                    onNavigateBack = { navController.popBackStack() },
                    wallet = w
                )
            }
        }

        // Switch Network Screen
        composable(
            route = Route.SwitchNetwork.route,
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: return@composable
            val wallet = remember(address) { findWalletByAddress(address) }

            wallet?.let { w ->
                SwitchNetworkScreen(
                    onNavigateBack = { navController.popBackStack() },
                    wallet = w,
                    onDismiss = { navController.popBackStack() }
                )
            }
        }

        // Custom Balance Screen
        composable(
            route = Route.CustomBalance.route,
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: return@composable
            val wallet = remember(address) { findWalletByAddress(address) }

            wallet?.let { w ->
                CustomBalanceScreen(
                    onNavigateBack = { navController.popBackStack() },
                    wallet = w
                )
            }
        }

        // EVM Sign Transaction Screen
        composable(
            route = Route.EvmSignTransaction.route,
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: return@composable
            val wallet = remember(address) { findWalletByAddress(address) }

            wallet?.let { w ->
                EvmSignTransactionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    wallet = w
                )
            }
        }

        // EVM Send Transaction Screen
        composable(
            route = Route.EvmSendTransaction.route,
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: return@composable
            val wallet = remember(address) { findWalletByAddress(address) }

            wallet?.let { w ->
                EvmSendTransactionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    wallet = w
                )
            }
        }

        // EVM Sign Typed Data Screen
        composable(
            route = Route.EvmSignTypedData.route,
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: return@composable
            val wallet = remember(address) { findWalletByAddress(address) }

            wallet?.let { w ->
                EvmSignTypedDataScreen(
                    onNavigateBack = { navController.popBackStack() },
                    wallet = w
                )
            }
        }

        // EVM Write Contract Screen
        composable(
            route = Route.EvmWriteContract.route,
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: return@composable
            val wallet = remember(address) { findWalletByAddress(address) }

            wallet?.let { w ->
                EvmWriteContractScreen(
                    onNavigateBack = { navController.popBackStack() },
                    wallet = w
                )
            }
        }

        // EVM Send ERC20 Screen
        composable(
            route = Route.EvmSendErc20.route,
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: return@composable
            val wallet = remember(address) { findWalletByAddress(address) }

            wallet?.let { w ->
                EvmSendErc20Screen(
                    onNavigateBack = { navController.popBackStack() },
                    wallet = w
                )
            }
        }

        // Solana Send Transaction Screen
        composable(
            route = Route.SolanaSendTransaction.route,
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: return@composable
            val wallet = remember(address) { findWalletByAddress(address) }

            wallet?.let { w ->
                SolanaSendTransactionScreen(
                    onNavigateBack = { navController.popBackStack() },
                    wallet = w
                )
            }
        }

        // Wallet Password Screen
        composable(
            route = Route.WalletPassword.route,
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: return@composable
            val wallet = remember(address) { findWalletByAddress(address) }

            wallet?.let { w ->
                WalletPasswordScreen(
                    onNavigateBack = { navController.popBackStack() },
                    wallet = w
                )
            }
        }

        // Solana Send Token Screen
        composable(
            route = Route.SolanaSendToken.route,
            arguments = listOf(navArgument("address") { type = NavType.StringType })
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address") ?: return@composable
            val wallet = remember(address) { findWalletByAddress(address) }

            wallet?.let { w ->
                SolanaSendTokenScreen(
                    onNavigateBack = { navController.popBackStack() },
                    wallet = w
                )
            }
        }
    }
}

private fun findWalletByAddress(address: String): BaseWallet? {
    val sdk = DynamicSDK.getInstance()
    return sdk.wallets.userWallets.find { it.address == address }
}
