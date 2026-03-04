package com.dynamic.sdk.example.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.DelegatedAccessState
import com.dynamic.sdk.android.Models.DelegationStatus
import com.dynamic.sdk.android.Models.DelegationWalletIdentifier
import com.dynamic.sdk.android.Models.WalletDelegatedStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelegationScreen(onNavigateBack: () -> Unit) {
    val viewModel: DelegationViewModel = viewModel()
    val delegationState by viewModel.delegationState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wallet Delegation") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Feedback Message
                item {
                    feedbackMessage?.let { message ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    message,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                // State Info Card
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Delegation State",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            InfoRow("Delegation Enabled", delegationState?.delegatedAccessEnabled?.toString() ?: "N/A")
                            InfoRow("Delegation Required", delegationState?.requiresDelegation?.toString() ?: "N/A")
                            InfoRow("Wallets Count", "${delegationState?.walletsDelegatedStatus?.size ?: 0}")
                        }
                    }
                }

                // Actions Section
                item {
                    Text(
                        "Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = "Open Delegation Modal",
                            icon = Icons.Default.OpenInNew,
                            onClick = { viewModel.initDelegationProcess() }
                        )
                        ActionButton(
                            text = "Check Should Prompt",
                            icon = Icons.Default.QuestionMark,
                            onClick = { viewModel.checkShouldPrompt() }
                        )
                        ActionButton(
                            text = "Get Wallets Status",
                            icon = Icons.Default.Refresh,
                            onClick = { viewModel.getWalletsStatus() }
                        )
                        ActionButton(
                            text = "Delegate All Wallets",
                            icon = Icons.Default.Shield,
                            onClick = { viewModel.delegateAllWallets() }
                        )
                        ActionButton(
                            text = "Dismiss All Prompts",
                            icon = Icons.Default.Clear,
                            onClick = { viewModel.dismissAllPrompts() }
                        )
                        ActionButton(
                            text = "Clear Session State",
                            icon = Icons.Default.DeleteOutline,
                            onClick = { viewModel.clearSessionState() }
                        )
                    }
                }

                // Wallets List
                delegationState?.walletsDelegatedStatus?.let { wallets ->
                    if (wallets.isNotEmpty()) {
                        item {
                            Text(
                                "Wallets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(wallets) { wallet ->
                            WalletDelegationCard(
                                wallet = wallet,
                                onRevoke = { viewModel.revokeWallet(wallet) },
                                onDeny = { viewModel.denyWallet(wallet) },
                                onDismiss = { viewModel.dismissPrompt(wallet.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun WalletDelegationCard(
    wallet: WalletDelegatedStatus,
    onRevoke: () -> Unit,
    onDeny: () -> Unit,
    onDismiss: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        getStatusIcon(wallet.status),
                        contentDescription = null,
                        tint = getStatusColor(wallet.status)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${wallet.address.take(10)}...${wallet.address.takeLast(8)}",
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = getStatusColor(wallet.status).copy(alpha = 0.2f)
                ) {
                    Text(
                        wallet.status.value.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = getStatusColor(wallet.status),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Chain: ${wallet.chain}", style = MaterialTheme.typography.bodySmall)
            Text("ID: ${wallet.id}", style = MaterialTheme.typography.bodySmall)
            wallet.isDismissedThisSession?.let {
                Text("Dismissed: $it", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (wallet.status) {
                    DelegationStatus.DELEGATED -> {
                        OutlinedButton(
                            onClick = onRevoke,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Revoke")
                        }
                    }
                    DelegationStatus.PENDING -> {
                        OutlinedButton(
                            onClick = onDeny,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Deny")
                        }
                        FilledTonalButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.RemoveCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dismiss")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

private fun getStatusIcon(status: DelegationStatus): androidx.compose.ui.graphics.vector.ImageVector {
    return when (status) {
        DelegationStatus.DELEGATED -> Icons.Default.CheckCircle
        DelegationStatus.DENIED -> Icons.Default.Cancel
        DelegationStatus.PENDING -> Icons.Default.Pending
    }
}

private fun getStatusColor(status: DelegationStatus): Color {
    return when (status) {
        DelegationStatus.DELEGATED -> Color(0xFF4CAF50)
        DelegationStatus.DENIED -> Color(0xFFF44336)
        DelegationStatus.PENDING -> Color(0xFFFF9800)
    }
}

class DelegationViewModel : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _delegationState = MutableStateFlow<DelegatedAccessState?>(null)
    val delegationState: StateFlow<DelegatedAccessState?> = _delegationState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    init {
        loadDelegationState()
        observeDelegationChanges()
    }

    private fun loadDelegationState() {
        _delegationState.value = sdk.wallets.delegatedAccessState
    }

    private fun observeDelegationChanges() {
        viewModelScope.launch {
            sdk.wallets.delegatedAccessChanges.collect { state ->
                _delegationState.value = state
            }
        }
    }

    fun initDelegationProcess() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                sdk.wallets.waas.delegation.initDelegationProcess()
                _feedbackMessage.value = "Delegation modal opened"
            } catch (e: Exception) {
                _feedbackMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun checkShouldPrompt() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val shouldPrompt = sdk.wallets.waas.delegation.shouldPromptWalletDelegation()
                _feedbackMessage.value = "Should prompt: $shouldPrompt"
            } catch (e: Exception) {
                _feedbackMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getWalletsStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val statuses = sdk.wallets.waas.delegation.getWalletsDelegatedStatus()
                _feedbackMessage.value = "Found ${statuses.size} wallets"
                loadDelegationState()
            } catch (e: Exception) {
                _feedbackMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun delegateAllWallets() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                sdk.wallets.waas.delegation.delegateKeyShares()
                _feedbackMessage.value = "Delegation started for all wallets"
            } catch (e: Exception) {
                _feedbackMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun revokeWallet(wallet: WalletDelegatedStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                sdk.wallets.waas.delegation.revokeDelegation(
                    wallets = listOf(
                        DelegationWalletIdentifier(wallet.chain, wallet.address)
                    )
                )
                _feedbackMessage.value = "Revoked delegation for ${wallet.address}"
                loadDelegationState()
            } catch (e: Exception) {
                _feedbackMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun denyWallet(wallet: WalletDelegatedStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                sdk.wallets.waas.delegation.denyWalletDelegation(wallet.id)
                _feedbackMessage.value = "Denied delegation for ${wallet.address}"
                loadDelegationState()
            } catch (e: Exception) {
                _feedbackMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissPrompt(walletId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                sdk.wallets.waas.delegation.dismissDelegationPrompt(walletId)
                _feedbackMessage.value = "Dismissed prompt for wallet"
            } catch (e: Exception) {
                _feedbackMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dismissAllPrompts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                sdk.wallets.waas.delegation.dismissDelegationPrompt()
                _feedbackMessage.value = "Dismissed all prompts"
            } catch (e: Exception) {
                _feedbackMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSessionState() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                sdk.wallets.waas.delegation.clearDelegationSessionState()
                _feedbackMessage.value = "Session state cleared"
            } catch (e: Exception) {
                _feedbackMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
