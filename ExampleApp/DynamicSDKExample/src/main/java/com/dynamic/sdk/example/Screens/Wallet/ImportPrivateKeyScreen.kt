package com.dynamic.sdk.example.Screens.Wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.ThresholdSignatureScheme
import com.dynamic.sdk.android.Models.WaasChain
import com.dynamic.sdk.example.Components.ActionButton
import com.dynamic.sdk.example.Components.ErrorMessageView
import kotlinx.coroutines.launch

/** ed25519 chains that support raw 32-byte signing-scalar import. */
private val ed25519Chains = setOf(WaasChain.SVM, WaasChain.SUI, WaasChain.TON)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPrivateKeyScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sdk = remember { DynamicSDK.getInstance() }

    var selectedChain by remember { mutableStateOf(WaasChain.EVM) }
    var chainMenuOpen by remember { mutableStateOf(false) }

    var privateKey by remember { mutableStateOf("") }

    var selectedScheme by remember { mutableStateOf<ThresholdSignatureScheme?>(null) }
    var schemeMenuOpen by remember { mutableStateOf(false) }

    var publicAddressCheck by remember { mutableStateOf("") }
    var addressType by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRawScalarImport by remember { mutableStateOf(false) }

    var isImporting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Import Private Key",
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
                text = "Import a raw private key into a new Dynamic WaaS wallet on the chosen chain. " +
                    "The webview-controller resolves the WaaS connector by chain — no pre-existing wallet required.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Chain picker
            ExposedDropdownMenuBox(
                expanded = chainMenuOpen,
                onExpandedChange = { chainMenuOpen = !chainMenuOpen }
            ) {
                OutlinedTextField(
                    value = selectedChain.value,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isImporting,
                    label = { Text("Chain") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = chainMenuOpen)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = chainMenuOpen,
                    onDismissRequest = { chainMenuOpen = false }
                ) {
                    WaasChain.values().forEach { chain ->
                        DropdownMenuItem(
                            text = { Text(chain.value) },
                            onClick = {
                                selectedChain = chain
                                chainMenuOpen = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = privateKey,
                onValueChange = { privateKey = it },
                label = { Text("Private key") },
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isImporting,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Threshold signature scheme picker
            ExposedDropdownMenuBox(
                expanded = schemeMenuOpen,
                onExpandedChange = { schemeMenuOpen = !schemeMenuOpen }
            ) {
                OutlinedTextField(
                    value = selectedScheme?.value ?: "Connector default",
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isImporting,
                    label = { Text("Threshold signature scheme") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = schemeMenuOpen)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = schemeMenuOpen,
                    onDismissRequest = { schemeMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Connector default") },
                        onClick = {
                            selectedScheme = null
                            schemeMenuOpen = false
                        }
                    )
                    ThresholdSignatureScheme.values().forEach { scheme ->
                        DropdownMenuItem(
                            text = { Text(scheme.value) },
                            onClick = {
                                selectedScheme = scheme
                                schemeMenuOpen = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = publicAddressCheck,
                onValueChange = { publicAddressCheck = it },
                label = { Text("Public address check (optional)") },
                enabled = !isImporting,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (selectedChain == WaasChain.BTC) {
                OutlinedTextField(
                    value = addressType,
                    onValueChange = { addressType = it },
                    label = { Text("Address type (BTC) — e.g. segwit") },
                    enabled = !isImporting,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (optional)") },
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isImporting,
                modifier = Modifier.fillMaxWidth()
            )

            // Raw scalar toggle — ed25519 chains only (SVM, SUI, TON)
            if (selectedChain in ed25519Chains) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Raw scalar import",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Treat the key as a raw 32-byte ed25519 signing scalar (hex) from an external MPC system.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = isRawScalarImport,
                        onCheckedChange = { isRawScalarImport = it },
                        enabled = !isImporting
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            ActionButton(
                icon = Icons.Default.Download,
                title = if (isImporting) "Importing..." else "Import Private Key",
                onClick = {
                    val key = privateKey.trim()
                    if (isImporting || key.isEmpty()) return@ActionButton
                    val check = publicAddressCheck.trim()
                    val addr = addressType.trim()
                    val pwd = password.trim()

                    scope.launch {
                        isImporting = true
                        errorMessage = null
                        successMessage = null
                        try {
                            sdk.wallets.waas.importPrivateKey(
                                chain = selectedChain,
                                privateKey = key,
                                thresholdSignatureScheme = selectedScheme,
                                publicAddressCheck = check.ifEmpty { null },
                                addressType = addr.ifEmpty { null },
                                password = pwd.ifEmpty { null },
                                isRawScalarImport = if (selectedChain in ed25519Chains) isRawScalarImport else null
                            )
                            successMessage = "Private key imported"
                            privateKey = ""
                            publicAddressCheck = ""
                            addressType = ""
                            password = ""
                        } catch (e: Exception) {
                            errorMessage = "Import failed: ${e.message}"
                        }
                        isImporting = false
                    }
                }
            )

            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                ErrorMessageView(message = error, modifier = Modifier.fillMaxWidth())
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
