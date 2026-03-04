package com.dynamic.sdk.example.Screens.Wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.BaseWallet
import com.dynamic.sdk.example.Components.ActionButton
import com.dynamic.sdk.example.Components.ErrorMessageView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletPasswordScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Recovery state
    var recoveryStateText by remember { mutableStateOf<String?>(null) }

    // Unlock
    var unlockPassword by remember { mutableStateOf("") }

    // Set password
    var setNewPassword by remember { mutableStateOf("") }

    // Update password
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Wallet Password",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // --- Check Recovery State ---
            Text(
                text = "Check Recovery State",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Check if the wallet is locked (encrypted) or ready.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            ActionButton(
                icon = Icons.Default.Search,
                title = if (isLoading) "Checking..." else "Check Recovery State",
                onClick = {
                    if (isLoading) return@ActionButton
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        successMessage = null
                        try {
                            val state = DynamicSDK.getInstance().wallets.waas
                                .getWalletRecoveryState(wallet)
                            recoveryStateText =
                                "State: ${state.walletReadyState}, Password encrypted: ${state.isPasswordEncrypted}"
                        } catch (e: Exception) {
                            errorMessage = "Error: ${e.message}"
                        }
                        isLoading = false
                    }
                }
            )

            recoveryStateText?.let { text ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // --- Unlock Wallet ---
            Text(
                text = "Unlock Wallet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Unlock a password-protected wallet for the current session.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = unlockPassword,
                onValueChange = { unlockPassword = it },
                label = { Text("Enter password to unlock") },
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            ActionButton(
                icon = Icons.Default.LockOpen,
                title = "Unlock Wallet",
                onClick = {
                    if (isLoading || unlockPassword.trim().isEmpty()) return@ActionButton
                    scope.launch {
                        val password = unlockPassword.trim()
                        isLoading = true
                        errorMessage = null
                        successMessage = null
                        try {
                            DynamicSDK.getInstance().wallets.waas
                                .unlockWallet(wallet, password = password)
                            successMessage = "Wallet unlocked"
                            unlockPassword = ""
                        } catch (e: Exception) {
                            errorMessage = "Unlock failed: ${e.message}"
                        }
                        isLoading = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // --- Set Password ---
            Text(
                text = "Set Password",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Set a new password on a wallet that doesn't have one yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = setNewPassword,
                onValueChange = { setNewPassword = it },
                label = { Text("New password") },
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            ActionButton(
                icon = Icons.Default.VpnKey,
                title = "Set Password",
                onClick = {
                    if (isLoading || setNewPassword.trim().isEmpty()) return@ActionButton
                    scope.launch {
                        val newPwd = setNewPassword.trim()
                        isLoading = true
                        errorMessage = null
                        successMessage = null
                        try {
                            DynamicSDK.getInstance().wallets.waas
                                .setPassword(wallet, newPassword = newPwd)
                            successMessage = "Password set"
                            setNewPassword = ""
                        } catch (e: Exception) {
                            errorMessage = "Set password failed: ${e.message}"
                        }
                        isLoading = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            // --- Update Password ---
            Text(
                text = "Update Password",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Change the password for all wallets on this account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
                label = { Text("Current password") },
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New password") },
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            ActionButton(
                icon = Icons.Default.VpnKey,
                title = "Update Password",
                onClick = {
                    if (isLoading || currentPassword.trim().isEmpty() || newPassword.trim().isEmpty()) return@ActionButton
                    scope.launch {
                        val current = currentPassword.trim()
                        val newPwd = newPassword.trim()
                        isLoading = true
                        errorMessage = null
                        successMessage = null
                        try {
                            DynamicSDK.getInstance().wallets.waas
                                .updatePassword(
                                    wallet,
                                    existingPassword = current,
                                    newPassword = newPwd
                                )
                            successMessage = "Password updated"
                            currentPassword = ""
                            newPassword = ""
                        } catch (e: Exception) {
                            errorMessage = "Update password failed: ${e.message}"
                        }
                        isLoading = false
                    }
                }
            )

            // --- Feedback ---
            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                ErrorMessageView(
                    message = error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            successMessage?.let { success ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = success,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
