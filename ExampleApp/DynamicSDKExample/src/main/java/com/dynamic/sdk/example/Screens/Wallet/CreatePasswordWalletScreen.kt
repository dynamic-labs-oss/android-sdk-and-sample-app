package com.dynamic.sdk.example.Screens.Wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.EmbeddedWalletChain
import com.dynamic.sdk.example.Components.ActionButton
import com.dynamic.sdk.example.Components.ErrorMessageView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePasswordWalletScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedChain by remember { mutableStateOf(EmbeddedWalletChain.Evm) }
    var password by remember { mutableStateOf("") }
    var chainMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Create Password Wallet",
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
            Text(
                text = "Create an embedded wallet protected by a password. Choose the chain and optionally enter a password.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Chain selector
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chain",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box {
                    Button(
                        onClick = { chainMenuExpanded = true },
                        enabled = !isCreating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(selectedChain.value)
                    }
                    DropdownMenu(
                        expanded = chainMenuExpanded,
                        onDismissRequest = { chainMenuExpanded = false }
                    ) {
                        EmbeddedWalletChain.entries.forEach { chain ->
                            DropdownMenuItem(
                                text = { Text(chain.value) },
                                onClick = {
                                    selectedChain = chain
                                    chainMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (optional)") },
                placeholder = { Text("Leave empty for no password") },
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isCreating,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            ActionButton(
                icon = Icons.Default.Add,
                title = if (isCreating) "Creating..." else "Create wallet",
                onClick = {
                    if (isCreating) return@ActionButton
                    scope.launch {
                        isCreating = true
                        errorMessage = null
                        val pwd = password.trim()
                        try {
                            DynamicSDK.getInstance().wallets.embedded
                                .createWallet(
                                    chain = selectedChain,
                                    password = pwd.ifEmpty { null }
                                )
                            onNavigateBack()
                        } catch (e: Exception) {
                            errorMessage = "Create wallet failed: ${e.message}"
                        }
                        isCreating = false
                    }
                }
            )

            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                ErrorMessageView(
                    message = error,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
