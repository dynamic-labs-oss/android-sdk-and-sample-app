package com.dynamic.sdk.example.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.EmbeddedWalletChain
import com.dynamic.sdk.example.Components.*

@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToWalletDetails: (String) -> Unit,
    onNavigateToMfaDevices: () -> Unit,
    onNavigateToStepUpAuth: () -> Unit,
    onNavigateToTrustedDevices: () -> Unit,
    onNavigateToPasskeys: () -> Unit,
    onNavigateToProjectSettings: () -> Unit,
    onNavigateToCreatePasswordWallet: () -> Unit = {},
    onNavigateToDelegation: () -> Unit = {},
    onNavigateToWalletConnect: () -> Unit = {}
) {
    val viewModel: HomeScreenViewModel = viewModel()
    val wallets by viewModel.wallets.collectAsState()
    val user by viewModel.user.collectAsState()
    val token by viewModel.token.collectAsState()
    val isCreatingWallets by viewModel.isCreatingWallets.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.startListening(onNavigateToLogin = onNavigateToLogin)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        // 1) Wallets Section
        Text(
            text = "Wallets",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )


        if (isCreatingWallets) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Creating wallets...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else if (wallets.isEmpty()) {
            var selectedChain by remember { mutableStateOf(EmbeddedWalletChain.Evm) }
            var chainDropdownExpanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box {
                    FilledTonalButton(
                        onClick = { chainDropdownExpanded = true }
                    ) {
                        Text(selectedChain.value)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                    DropdownMenu(
                        expanded = chainDropdownExpanded,
                        onDismissRequest = { chainDropdownExpanded = false }
                    ) {
                        EmbeddedWalletChain.entries.forEach { chain ->
                            DropdownMenuItem(
                                text = { Text(chain.value) },
                                onClick = {
                                    selectedChain = chain
                                    chainDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        viewModel.createWallet(selectedChain)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create wallet")
                }
            }
        } else {
            wallets.forEach { wallet ->
                WalletCard(
                    wallet = wallet,
                    onClick = { onNavigateToWalletDetails(wallet.address) }
                )
            }
        }

        // Create Password Wallet button
        Spacer(modifier = Modifier.height(12.dp))
        NavigationButton(
            icon = Icons.Default.Lock,
            title = "Create Password Wallet",
            onClick = onNavigateToCreatePasswordWallet,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2) Open Profile Button
        SimpleButton(
            icon = Icons.Default.Person,
            title = "Open Profile",
            onClick = { viewModel.showUserProfile() },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        // Error message
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            ErrorMessageView(
                message = errorMessage!!,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3) MFA Devices Button
        NavigationButton(
            icon = Icons.Default.Shield,
            title = "MFA Devices",
            onClick = onNavigateToMfaDevices,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Step-Up Auth Button
        NavigationButton(
            icon = Icons.Default.Security,
            title = "Step-Up Auth",
            onClick = onNavigateToStepUpAuth,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Trusted Devices Button
        NavigationButton(
            icon = Icons.Default.PhoneAndroid,
            title = "Trusted Devices",
            onClick = onNavigateToTrustedDevices,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 4) Passkeys Button
        NavigationButton(
            icon = Icons.Default.Key,
            title = "Passkeys",
            onClick = onNavigateToPasskeys,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 5) Project Settings Button
        NavigationButton(
            icon = Icons.Default.Settings,
            title = "Project Settings",
            onClick = onNavigateToProjectSettings,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 6) Delegation Button
        NavigationButton(
            icon = Icons.Default.Share,
            title = "Delegation",
            onClick = onNavigateToDelegation,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 7) WalletConnect Button
        NavigationButton(
            icon = Icons.Default.Link,
            title = "WalletConnect",
            onClick = onNavigateToWalletConnect,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 5) User JSON
        user?.let { userProfile ->
            val userJson = "Email: ${userProfile.email ?: "N/A"}\nUser ID: ${userProfile.userId ?: "N/A"}"
            ValueCard(
                title = "User",
                value = userJson,
                copyValue = userJson
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 6) Token
        token?.let { authToken ->
            ValueCard(
                title = "Token",
                value = authToken,
                displayValue = truncateMiddle(authToken),
                copyValue = authToken
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7) Logout Button (must be last)
        DangerButton(
            icon = Icons.Default.ExitToApp,
            title = "Logout",
            onClick = { viewModel.logout() },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}
