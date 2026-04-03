package com.dynamic.sdk.example.Screens.Wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.BaseWallet
import com.dynamic.sdk.android.Models.DelegationStatus
import com.dynamic.sdk.android.Models.DelegationWalletIdentifier
import com.dynamic.sdk.android.Models.WalletDelegatedStatus
import com.dynamic.sdk.android.Models.ChainEnum
import com.dynamic.sdk.example.Components.ActionButton
import com.dynamic.sdk.example.Components.ErrorMessageView
import com.dynamic.sdk.example.Components.SecondaryButton
import com.dynamic.sdk.example.ui.theme.WarningOrange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletDetailsScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet,
    onNavigateToSignMessage: () -> Unit,
    onNavigateToSwitchNetwork: () -> Unit,
    onNavigateToCustomBalance: () -> Unit,
    onNavigateToEvmSignTransaction: () -> Unit,
    onNavigateToEvmSendTransaction: () -> Unit,
    onNavigateToEvmSignTypedData: () -> Unit,
    onNavigateToEvmWriteContract: () -> Unit,
    onNavigateToEvmSendErc20: () -> Unit,
    onNavigateToSolanaSendTransaction: () -> Unit,
    onNavigateToSolanaSendToken: () -> Unit,
    onNavigateToWalletPassword: () -> Unit = {},
    onNavigateToSuiSignMessage: () -> Unit = {},
    onNavigateToSuiSignTransaction: () -> Unit = {},
    onNavigateToSuiSendTransaction: () -> Unit = {}
) {
    val viewModel: WalletDetailsViewModel = viewModel(
        factory = WalletDetailsViewModelFactory(wallet)
    )
    val balance by viewModel.balance.collectAsState()
    val network by viewModel.network.collectAsState()
    val isLoadingBalance by viewModel.isLoadingBalance.collectAsState()
    val isLoadingNetwork by viewModel.isLoadingNetwork.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val feedbackLabel by viewModel.feedbackLabel.collectAsState()
    val delegationStatus by viewModel.delegationStatus.collectAsState()
    val isDelegationLoading by viewModel.isDelegationLoading.collectAsState()

    val context = LocalContext.current
    var showCopiedSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Wallet Details",
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
                .padding(vertical = 16.dp)
        ) {
        // Wallet Detail Card
        WalletDetailCard(
            wallet = wallet,
            balance = balance,
            network = network,
            isLoadingBalance = isLoadingBalance,
            isLoadingNetwork = isLoadingNetwork
        )

        // Delegation Section
        delegationStatus?.let { status ->
            Spacer(modifier = Modifier.height(16.dp))
            DelegationSection(
                status = status,
                isLoading = isDelegationLoading,
                onEnable = { viewModel.enableDelegation() },
                onRevoke = { viewModel.revokeDelegation() }
            )
        }

        // Feedback message
        feedbackLabel?.let { feedback ->
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(
                        color = WarningOrange.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = WarningOrange
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = feedback,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarningOrange
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Copy Address Button
        SecondaryButton(
            icon = Icons.Default.ContentCopy,
            title = "Copy Address",
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("address", wallet.address))
                showCopiedSnackbar = true
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Copy Wallet Details Button
        SecondaryButton(
            icon = Icons.Default.ContentCopy,
            title = "Copy Wallet Details",
            onClick = {
                val details = buildString {
                    wallet.id?.takeIf { it.isNotEmpty() }?.let { appendLine("ID: $it") }
                    appendLine("Address: ${wallet.address}")
                    appendLine("Chain: ${wallet.chain}")
                    wallet.publicKey?.takeIf { it.isNotEmpty() }?.let { appendLine("Public Key: $it") }
                    wallet.walletName?.takeIf { it.isNotEmpty() }?.let { appendLine("Wallet Name: $it") }
                    wallet.walletProvider?.takeIf { it.isNotEmpty() }?.let { appendLine("Wallet Provider: $it") }
                }.trimEnd()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("wallet_details", details))
                showCopiedSnackbar = true
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sign Message Button
        SecondaryButton(
            icon = Icons.Default.Edit,
            title = "Sign Message",
            onClick = onNavigateToSignMessage,
            showChevron = true,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Error message
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(12.dp))
            ErrorMessageView(
                message = error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Set as Primary Button (if wallet has ID)
        if (wallet.id != null) {
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryButton(
                icon = Icons.Default.Star,
                title = "Set as Primary Wallet",
                onClick = { viewModel.setPrimary() },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Reveal Private Key Button (for embedded wallets)
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryButton(
                icon = Icons.Default.VpnKey,
                title = "Reveal Private Key",
                onClick = { viewModel.revealPrivateKey() },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Switch Network Button
        SecondaryButton(
            icon = Icons.Default.SwapHoriz,
            title = "Switch Network",
            onClick = onNavigateToSwitchNetwork,
            showChevron = true,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Custom Token Balances Button
        SecondaryButton(
            icon = Icons.Default.BarChart,
            title = "Custom Token Balances",
            onClick = onNavigateToCustomBalance,
            showChevron = true,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Wallet Password Button
        SecondaryButton(
            icon = Icons.Default.Lock,
            title = "Wallet Password",
            onClick = onNavigateToWalletPassword,
            showChevron = true,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Chain-specific actions
        if (wallet.chain.uppercase() == "EVM") {
            EVMActionsView(
                onSignTransaction = onNavigateToEvmSignTransaction,
                onSignTypedData = onNavigateToEvmSignTypedData,
                onSendTransaction = onNavigateToEvmSendTransaction,
                onSendErc20 = onNavigateToEvmSendErc20,
                onWriteContract = onNavigateToEvmWriteContract
            )
        }

        if (wallet.chain.uppercase() == "SOL" || wallet.chain.uppercase() == "SOLANA") {
            SolanaActionsView(
                onSendTransaction = onNavigateToSolanaSendTransaction,
                onSendToken = onNavigateToSolanaSendToken
            )
        }

        if (wallet.chain.uppercase() == "SUI") {
            SuiActionsView(
                onSignMessage = onNavigateToSuiSignMessage,
                onSignTransaction = onNavigateToSuiSignTransaction,
                onSendTransaction = onNavigateToSuiSendTransaction
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showCopiedSnackbar) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showCopiedSnackbar = false
        }
    }
}

@Composable
fun WalletDetailCard(
    wallet: BaseWallet,
    balance: String?,
    network: String?,
    isLoadingBalance: Boolean,
    isLoadingNetwork: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    wallet.walletName?.let { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = wallet.chain.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Wallet ID
            wallet.id?.let { walletId ->
                if (walletId.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Wallet ID",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = walletId,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                    Divider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } ?: run {
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(
                text = "Address",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = wallet.address,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )

            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outline
            )

            Text(
                text = "Current Network",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            when {
                !network.isNullOrEmpty() -> {
                    Text(
                        text = network,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                isLoadingNetwork -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Loading...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outline
            )

            Text(
                text = "Balance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            when {
                !balance.isNullOrEmpty() -> {
                    Text(
                        text = balance,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                isLoadingBalance -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Loading...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EVMActionsView(
    onSignTransaction: () -> Unit,
    onSignTypedData: () -> Unit,
    onSendTransaction: () -> Unit,
    onSendErc20: () -> Unit,
    onWriteContract: () -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = "EVM Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Sign Transaction
        ActionButton(
            icon = Icons.Default.Create,
            title = "Sign Transaction",
            onClick = onSignTransaction,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Sign Typed Data
        ActionButton(
            icon = Icons.Default.Description,
            title = "Sign Typed Data",
            onClick = onSignTypedData,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Send Transaction
        ActionButton(
            icon = Icons.Default.Send,
            title = "Send Transaction",
            onClick = onSendTransaction,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Send ERC20
        ActionButton(
            icon = Icons.Default.SwapHoriz,
            title = "Send ERC20",
            onClick = onSendErc20,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Write Contract
        ActionButton(
            icon = Icons.Default.Code,
            title = "Write Contract",
            onClick = onWriteContract,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SolanaActionsView(
    onSendTransaction: () -> Unit,
    onSendToken: () -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = "Solana Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Send SOL
        ActionButton(
            icon = Icons.Default.Send,
            title = "Send SOL",
            onClick = onSendTransaction,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Send SPL Token
        ActionButton(
            icon = Icons.Default.SwapHoriz,
            title = "Send SPL Token",
            onClick = onSendToken,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SuiActionsView(
    onSignMessage: () -> Unit,
    onSignTransaction: () -> Unit,
    onSendTransaction: () -> Unit
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = "SUI Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // Sign Message
        ActionButton(
            icon = Icons.Default.Edit,
            title = "Sign Message",
            onClick = onSignMessage,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Sign Transaction
        ActionButton(
            icon = Icons.Default.Create,
            title = "Sign Transaction",
            onClick = onSignTransaction,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Send Transaction
        ActionButton(
            icon = Icons.Default.Send,
            title = "Send Transaction",
            onClick = onSendTransaction,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun DelegationSection(
    status: WalletDelegatedStatus,
    isLoading: Boolean,
    onEnable: () -> Unit,
    onRevoke: () -> Unit
) {
    val statusColor = when (status.status) {
        DelegationStatus.DELEGATED -> MaterialTheme.colorScheme.primary
        DelegationStatus.DENIED -> MaterialTheme.colorScheme.error
        DelegationStatus.PENDING -> WarningOrange
    }

    val statusText = when (status.status) {
        DelegationStatus.DELEGATED -> "DELEGATED"
        DelegationStatus.DENIED -> "DENIED"
        DelegationStatus.PENDING -> "PENDING"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delegated Access",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                when (status.status) {
                    DelegationStatus.DELEGATED -> {
                        OutlinedButton(
                            onClick = onRevoke,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RemoveCircleOutline,
                                contentDescription = "Revoke",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Revoke Delegated Access")
                        }
                    }
                    DelegationStatus.PENDING -> {
                        OutlinedButton(
                            onClick = onEnable,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = "Enable",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable Delegated Access")
                        }
                    }
                    DelegationStatus.DENIED -> {
                        // No action for denied wallets
                    }
                }
            }
        }
    }
}

class WalletDetailsViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _balance = MutableStateFlow<String?>(null)
    val balance: StateFlow<String?> = _balance.asStateFlow()

    private val _network = MutableStateFlow<String?>(null)
    val network: StateFlow<String?> = _network.asStateFlow()

    private val _isLoadingBalance = MutableStateFlow(false)
    val isLoadingBalance: StateFlow<Boolean> = _isLoadingBalance.asStateFlow()

    private val _isLoadingNetwork = MutableStateFlow(false)
    val isLoadingNetwork: StateFlow<Boolean> = _isLoadingNetwork.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _feedbackLabel = MutableStateFlow<String?>(null)
    val feedbackLabel: StateFlow<String?> = _feedbackLabel.asStateFlow()

    private val _delegationStatus = MutableStateFlow<WalletDelegatedStatus?>(null)
    val delegationStatus: StateFlow<WalletDelegatedStatus?> = _delegationStatus.asStateFlow()

    private val _isDelegationLoading = MutableStateFlow(false)
    val isDelegationLoading: StateFlow<Boolean> = _isDelegationLoading.asStateFlow()

    init {
        // Observe delegation changes
        viewModelScope.launch {
            sdk.wallets.delegatedAccessChanges.collect {
                updateDelegationStatusFromLocal()
            }
        }
    }

    private fun updateDelegationStatusFromLocal() {
        wallet.id?.let { id ->
            val existingStatus = sdk.wallets.getDelegationStatusForWallet(id)
            if (existingStatus != null) {
                _delegationStatus.value = existingStatus
            }
        }
    }

    private fun fetchDelegationStatus() {
        viewModelScope.launch {
            try {
                val statuses = sdk.wallets.waas.delegation.getWalletsDelegatedStatus()
                wallet.id?.let { id ->
                    val status = statuses.find { it.id == id }
                    if (status != null) {
                        _delegationStatus.value = status
                    } else if (statuses.isNotEmpty()) {
                        // Delegation is enabled but this wallet has no status yet — show as PENDING
                        _delegationStatus.value = WalletDelegatedStatus(
                            id = id,
                            address = wallet.address,
                            chain = wallet.chain,
                            status = DelegationStatus.PENDING
                        )
                    }
                }
            } catch (_: Exception) {
                // Delegation not available for this wallet/project
            }
        }
    }

    fun refresh() {
        loadBalance()
        loadNetwork()
        updateDelegationStatusFromLocal()
        fetchDelegationStatus()
    }

    private fun loadBalance() {
        viewModelScope.launch {
            _isLoadingBalance.value = true
            try {
                val balanceResult = sdk.wallets.getBalance(wallet)
                _balance.value = balanceResult
            } catch (e: Exception) {
                _balance.value = null
            }
            _isLoadingBalance.value = false
        }
    }

    private fun loadNetwork() {
        viewModelScope.launch {
            _isLoadingNetwork.value = true
            try {
                val networkResult = sdk.wallets.getNetwork(wallet)
                val jsonValue = networkResult.value
                _network.value = if (jsonValue is JsonPrimitive) {
                    jsonValue.intOrNull?.toString() ?: jsonValue.contentOrNull ?: jsonValue.toString()
                } else {
                    jsonValue.toString()
                }
            } catch (e: Exception) {
                _network.value = null
            }
            _isLoadingNetwork.value = false
        }
    }

    fun setPrimary() {
        viewModelScope.launch {
            try {
                wallet.id?.let { id ->
                    sdk.wallets.setPrimary(id)
                    _feedbackLabel.value = "Wallet set as primary"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to set as primary: ${e.message}"
            }
        }
    }

    fun revealPrivateKey() {
        viewModelScope.launch {
            try {
                sdk.ui.revealEmbeddedWalletPrivateKey()
                _feedbackLabel.value = "Reveal private key request sent"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to reveal private key: ${e.message}"
            }
        }
    }

    fun enableDelegation() {
        viewModelScope.launch {
            _isDelegationLoading.value = true
            try {
                val chainEnum = if (wallet.chain.uppercase() == "EVM") ChainEnum.EVM else ChainEnum.SOL
                sdk.wallets.waas.delegation.delegateKeyShares(
                    wallets = listOf(
                        DelegationWalletIdentifier(
                            chainName = chainEnum.toString(),
                            accountAddress = wallet.address
                        )
                    )
                )
                _feedbackLabel.value = "Delegated access enabled successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to enable delegation: ${e.message}"
            }
            _isDelegationLoading.value = false
        }
    }

    fun revokeDelegation() {
        viewModelScope.launch {
            _isDelegationLoading.value = true
            try {
                val chainEnum = if (wallet.chain.uppercase() == "EVM") ChainEnum.EVM else ChainEnum.SOL
                sdk.wallets.waas.delegation.revokeDelegation(
                    wallets = listOf(
                        DelegationWalletIdentifier(
                            chainName = chainEnum.toString(),
                            accountAddress = wallet.address
                        )
                    )
                )
                _feedbackLabel.value = "Delegated access revoked successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to revoke delegation: ${e.message}"
            }
            _isDelegationLoading.value = false
        }
    }
}

class WalletDetailsViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WalletDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WalletDetailsViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
