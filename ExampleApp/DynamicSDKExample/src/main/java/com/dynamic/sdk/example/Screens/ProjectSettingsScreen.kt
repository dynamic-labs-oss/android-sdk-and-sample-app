package com.dynamic.sdk.example.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.*
import com.dynamic.sdk.example.Components.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun ProjectSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: ProjectSettingsViewModel = viewModel()
    val settings by viewModel.settings.collectAsState()
    val rawJson by viewModel.rawJson.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Project Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.loadSettings() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ErrorMessageView(message = errorMessage!!)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadSettings() }) {
                        Text("Retry")
                    }
                }
            } else if (settings != null) {
                SettingsContent(settings = settings!!)
            }

            rawJson?.let { json ->
                ValueCard(
                    title = "Raw JSON",
                    value = json,
                    displayValue = json.take(200) + "...",
                    copyValue = json
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SettingsContent(settings: ProjectSettings) {
    // Environment
    SettingsSection(title = "Environment") {
        SettingsRow(label = "Name", value = settings.environmentName?.name ?: "N/A")
    }

    // General
    settings.general?.let { general ->
        SettingsSection(title = "General") {
            SettingsRow(label = "Display Name", value = general.displayName ?: "N/A")
            SettingsRow(label = "Support Email", value = general.supportEmail ?: "N/A")
            SettingsRow(label = "App Logo", value = general.appLogo ?: "N/A")
            SettingsRow(label = "Email Company Name", value = general.emailCompanyName ?: "N/A")
            SettingsRow(label = "Skip Optional KYC", value = general.skipOptionalKYCFieldDuringOnboarding?.toString() ?: "N/A")
        }
    }

    // Chains
    SettingsSection(title = "Chains (${settings.chains.size})") {
        settings.chains.forEach { chain ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chain.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (chain.enabled) "Enabled" else "Disabled",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (chain.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            chain.networks?.forEach { net ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = net.chainName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (net.enabled) "On" else "Off",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (net.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Design
    settings.design?.let { design ->
        SettingsSection(title = "Design") {
            design.modal?.let { modal ->
                SettingsRow(label = "Theme", value = modal.theme ?: "N/A")
                SettingsRow(label = "Primary Color", value = modal.primaryColor ?: "N/A")
                SettingsRow(label = "View", value = modal.view ?: "N/A")
                SettingsRow(label = "Template", value = modal.template ?: "N/A")
                SettingsRow(label = "Radius", value = modal.radius?.let { "%.0f".format(it) } ?: "N/A")
            } ?: Text(
                text = "No modal design settings",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Privacy
    settings.privacy?.let { privacy ->
        SettingsSection(title = "Privacy") {
            SettingsRow(label = "Collect IP", value = privacy.collectIp?.toString() ?: "N/A")
        }
    }

    // SDK Settings
    settings.sdk?.let { sdk ->
        SettingsSection(title = "SDK") {
            SettingsRow(label = "Multi Wallet", value = sdk.multiWallet?.toString() ?: "N/A")
            SettingsRow(label = "Confirm Wallet Transfers", value = sdk.confirmWalletTransfers?.toString() ?: "N/A")
            SettingsRow(label = "Onramp Funding", value = sdk.onrampFunding?.toString() ?: "N/A")
            SettingsRow(label = "Passkey Embedded Wallet", value = sdk.passkeyEmbeddedWalletEnabled?.toString() ?: "N/A")
            SettingsRow(label = "Auto Embedded Wallet", value = sdk.automaticEmbeddedWalletCreation?.toString() ?: "N/A")
            SettingsRow(label = "Prevent Orphaned Accounts", value = sdk.preventOrphanedAccounts?.toString() ?: "N/A")
            SettingsRow(label = "Block Email Subaddresses", value = sdk.blockEmailSubaddresses?.toString() ?: "N/A")
            SettingsRow(label = "Show Fiat", value = sdk.showFiat?.toString() ?: "N/A")

            sdk.emailSignIn?.let { emailSignIn ->
                SettingsRow(label = "Email Sign-In Provider", value = emailSignIn.signInProvider?.name ?: "N/A")
            }

            sdk.walletConnect?.let { walletConnect ->
                SettingsRow(label = "WalletConnect v2", value = walletConnect.v2Enabled?.toString() ?: "N/A")
            }

            sdk.embeddedWallets?.let { ew ->
                SettingsRow(label = "Auto Create EW", value = ew.automaticEmbeddedWalletCreation?.toString() ?: "N/A")
                SettingsRow(label = "Email Recovery", value = ew.emailRecoveryEnabled?.toString() ?: "N/A")
                SettingsRow(label = "Tx Simulation", value = ew.transactionSimulation?.toString() ?: "N/A")
                SettingsRow(label = "Default Wallet Version", value = ew.defaultWalletVersion?.name ?: "N/A")
            }
        }
    }

    // WaaS
    settings.sdk?.waas?.let { waas ->
        SettingsSection(title = "WaaS") {
            SettingsRow(label = "Passcode Required", value = waas.passcodeRequired.toString())
            SettingsRow(label = "Backup Options", value = waas.backupOptions.joinToString(", ") { it.name })
            SettingsRow(label = "Export Disabled", value = waas.exportDisabled?.toString() ?: "N/A")
            waas.delegatedAccess?.let { da ->
                SettingsRow(label = "Delegated Access", value = da.enabled?.toString() ?: "N/A")
                SettingsRow(label = "Requires Delegation", value = da.requiresDelegation?.toString() ?: "N/A")
            }
        }
    }

    // Security
    settings.security?.let { security ->
        SettingsSection(title = "Security") {
            security.jwtDuration?.let { jwt ->
                SettingsRow(label = "JWT Duration", value = "${jwt.amount} ${jwt.unit.name}")
            }
            SettingsRow(label = "Environment Locked", value = security.environmentLocked?.toString() ?: "N/A")

            security.mfa?.let { mfa ->
                SettingsRow(label = "MFA Enabled", value = mfa.enabled?.toString() ?: "N/A")
                SettingsRow(label = "MFA Required", value = mfa.required?.toString() ?: "N/A")
            }

            security.externalAuth?.let { ext ->
                SettingsRow(label = "External Auth", value = ext.enabled?.toString() ?: "N/A")
                SettingsRow(label = "JWKS URL", value = ext.jwksUrl ?: "N/A")
            }
        }
    }

    // Providers
    settings.providers?.let { providers ->
        if (providers.isNotEmpty()) {
            SettingsSection(title = "Providers (${providers.size})") {
                providers.forEach { provider ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = provider.provider.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        provider.id?.let { id ->
                            Text(
                                text = id.take(8) + "...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // KYC Fields
    if (settings.kyc.isNotEmpty()) {
        SettingsSection(title = "KYC Fields (${settings.kyc.size})") {
            settings.kyc.forEach { field ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = field.label ?: field.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (field.required) {
                            Text(
                                text = "Required",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = if (field.enabled) "On" else "Off",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (field.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    // Networks
    settings.networks?.let { networks ->
        if (networks.isNotEmpty()) {
            SettingsSection(title = "Networks (${networks.size})") {
                networks.forEach { netConfig ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = netConfig.chainName ?: "Unknown Chain",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        netConfig.networks?.forEach { net ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = net.name,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "ID: ${net.chainId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (net.isTestnet == true) {
                                        Text(
                                            text = "Testnet",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                                    RoundedCornerShape(3.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Helper Composables

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )
    }
}

// MARK: - ViewModel

class ProjectSettingsViewModel : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val _settings = MutableStateFlow<ProjectSettings?>(null)
    val settings: StateFlow<ProjectSettings?> = _settings.asStateFlow()

    private val _rawJson = MutableStateFlow<String?>(null)
    val rawJson: StateFlow<String?> = _rawJson.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadSettings() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val result = sdk.auth.getProjectSettings()
                _settings.value = result

                if (result != null) {
                    _rawJson.value = json.encodeToString(result)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load project settings: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
