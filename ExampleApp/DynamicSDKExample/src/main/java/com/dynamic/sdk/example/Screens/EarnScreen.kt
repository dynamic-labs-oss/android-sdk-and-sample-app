package com.dynamic.sdk.example.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamic.sdk.android.Chains.Solana.SolanaSponsorshipMode
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.BaseWallet
import com.dynamic.sdk.android.Models.EarnChain
import com.dynamic.sdk.android.Models.EarnVaultDetails
import com.dynamic.sdk.android.Models.EarnVaultPosition
import com.dynamic.sdk.android.Models.EarnVaultPositionPerformance
import com.dynamic.sdk.android.Models.EarnVaultReward
import com.dynamic.sdk.android.Models.EarnVaultSummary
import com.dynamic.sdk.example.Components.ErrorMessageView
import com.dynamic.sdk.example.Components.PrimaryButton
import com.dynamic.sdk.example.Components.ValueCard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class EarnScreenViewModel : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _wallets = MutableStateFlow<List<BaseWallet>>(emptyList())
    val wallets: StateFlow<List<BaseWallet>> = _wallets.asStateFlow()

    private var refreshJob: Job? = null

    private val _vaults = MutableStateFlow<List<EarnVaultSummary>>(emptyList())
    val vaults: StateFlow<List<EarnVaultSummary>> = _vaults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _details = MutableStateFlow<EarnVaultDetails?>(null)
    val details: StateFlow<EarnVaultDetails?> = _details.asStateFlow()

    private val _position = MutableStateFlow<EarnVaultPosition?>(null)
    val position: StateFlow<EarnVaultPosition?> = _position.asStateFlow()

    private val _performance = MutableStateFlow<EarnVaultPositionPerformance?>(null)
    val performance: StateFlow<EarnVaultPositionPerformance?> = _performance.asStateFlow()

    private val _rewards = MutableStateFlow<List<EarnVaultReward>>(emptyList())
    val rewards: StateFlow<List<EarnVaultReward>> = _rewards.asStateFlow()

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult.asStateFlow()

    init {
        viewModelScope.launch {
            sdk.wallets.userWalletsChanges.collect { _wallets.value = it }
        }
        loadVaults()
    }

    fun loadVaults() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _vaults.value = sdk.earn.getVaults()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load vaults"
            }
            _isLoading.value = false
        }
    }

    fun loadVault(vaultId: String, ownerAddress: String) {
        refreshJob?.cancel()
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _details.value = null
            _position.value = null
            _performance.value = null
            _rewards.value = emptyList()
            _actionResult.value = null
            try {
                _details.value = sdk.earn.getVaultDetails(vaultId)
                if (ownerAddress.isNotBlank()) {
                    runCatching { _position.value = sdk.earn.getVaultPosition(vaultId, ownerAddress) }
                    runCatching { _performance.value = sdk.earn.getVaultPerformance(vaultId, ownerAddress) }
                    runCatching { _rewards.value = sdk.earn.getVaultRewards(vaultId, ownerAddress) }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load vault"
            }
            _isLoading.value = false
        }
    }

    fun runAction(
        vaultId: String,
        action: String,
        amount: String,
        ownerAddress: String
    ) {
        val wallet = _wallets.value.find {
            it.address.equals(ownerAddress, ignoreCase = true)
        }

        refreshJob?.cancel()
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _actionResult.value = null
            var sent = false
            try {
                val response = when (action) {
                    "deposit" -> sdk.earn.deposit(vaultId, ownerAddress = ownerAddress, amount = amount)
                    "withdraw" -> sdk.earn.withdraw(vaultId, ownerAddress = ownerAddress, amount = amount)
                    else -> sdk.earn.claim(vaultId, ownerAddress)
                }

                val payload = response.signingPayload
                if (payload == null) {
                    _actionResult.value = "No signing payload returned (nothing to do)"
                } else if (wallet == null) {
                    _actionResult.value =
                        "Payload received. Connect a wallet with address $ownerAddress to execute it."
                } else {
                    // Solana transactions are sent unsponsored (the wallet pays the fee):
                    // sponsorship failed with sponsorship_http_400 on the demo environment.
                    val hash = sdk.earn.execute(
                        payload,
                        wallet,
                        sponsorshipMode = SolanaSponsorshipMode.OFF
                    )
                    _actionResult.value = "Transaction sent: $hash"
                    sent = true
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Action failed"
            }
            _isLoading.value = false

            if (sent) {
                refreshJob = viewModelScope.launch { refreshPositionUntilChanged(vaultId, ownerAddress) }
            }
        }
    }

    private suspend fun refreshPosition(vaultId: String, ownerAddress: String) {
        val position = runCatching { sdk.earn.getVaultPosition(vaultId, ownerAddress) }.getOrNull()
        val performance = runCatching { sdk.earn.getVaultPerformance(vaultId, ownerAddress) }.getOrNull()
        val rewards = runCatching { sdk.earn.getVaultRewards(vaultId, ownerAddress) }.getOrNull()

        // Another vault may have been opened while these requests were in flight.
        if (_details.value?.vaultId != vaultId) return
        position?.let { _position.value = it }
        performance?.let { _performance.value = it }
        rewards?.let { _rewards.value = it }
    }

    /**
     * `execute` returns once the transaction is submitted, not mined, so the
     * position only changes after confirmation. Polls briefly for it.
     */
    private suspend fun refreshPositionUntilChanged(vaultId: String, ownerAddress: String) {
        val before = _position.value?.assets
        repeat(POSITION_REFRESH_ATTEMPTS) {
            delay(POSITION_REFRESH_INTERVAL_MS)
            refreshPosition(vaultId, ownerAddress)
            if (_position.value?.assets != before) return
        }
    }

    private companion object {
        const val POSITION_REFRESH_ATTEMPTS = 8
        const val POSITION_REFRESH_INTERVAL_MS = 3_000L
    }
}

private fun formatPercent(value: Double?): String =
    value?.let { String.format(Locale.US, "%.2f%%", it * 100) } ?: "—"

/**
 * Converts a raw on-chain token amount (e.g. `"1000685"`) to a grouped decimal
 * string using the token's [decimals] (`"1.000685"` for 6). Falls back to the
 * raw value when it isn't a plain integer.
 */
private fun formatUnits(raw: String, decimals: Int): String {
    val amount = raw.toBigDecimalOrNull() ?: return raw
    val pattern = if (decimals > 0) "#,##0.${"#".repeat(decimals)}" else "#,##0"
    return DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
        .format(amount.movePointLeft(decimals))
}

private fun formatUsd(value: Double?): String =
    value?.let { String.format(Locale.US, "$%,.0f", it) } ?: "—"

/** Wallets that can act as owner for a vault on [chain]; all wallets when no vault is selected. */
private fun List<BaseWallet>.compatibleWith(chain: EarnChain?): List<BaseWallet> =
    if (chain == null) this else filter { it.chain.equals(chain.value, ignoreCase = true) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerAddressDropdown(
    wallets: List<BaseWallet>,
    selectedAddress: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedAddress,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Owner address (for position & actions)") },
            placeholder = { Text("No compatible wallet") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            wallets.forEach { wallet ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                "${wallet.walletName ?: wallet.walletProvider ?: "Wallet"}  •  ${wallet.chain}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(wallet.address, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = {
                        onSelect(wallet.address)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarnScreen(onNavigateBack: () -> Unit) {
    val viewModel = remember { EarnScreenViewModel() }

    val vaults by viewModel.vaults.collectAsState()
    val details by viewModel.details.collectAsState()
    val position by viewModel.position.collectAsState()
    val performance by viewModel.performance.collectAsState()
    val rewards by viewModel.rewards.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val actionResult by viewModel.actionResult.collectAsState()

    var selectedVaultId by remember { mutableStateOf<String?>(null) }
    var ownerAddress by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    var vaultQuery by remember { mutableStateOf("") }
    var chainFilter by remember { mutableStateOf<EarnChain?>(null) }
    val chainOptions = vaults.mapNotNull { it.chainName }.distinct()
    val filteredVaults = vaults.filter { vault ->
        (chainFilter == null || vault.chainName == chainFilter) &&
            (vaultQuery.isBlank() || listOfNotNull(
                vault.curator,
                vault.assetSymbol,
                vault.provider?.name,
                vault.chainName?.value,
                vault.chainId.toString()
            ).any { it.contains(vaultQuery.trim(), ignoreCase = true) })
    }

    val selectedChain = vaults.find { it.vaultId == selectedVaultId }?.chainName
    val ownerOptions = wallets.compatibleWith(selectedChain)

    LaunchedEffect(ownerOptions) {
        if (ownerOptions.none { it.address.equals(ownerAddress, ignoreCase = true) }) {
            ownerAddress = ownerOptions.firstOrNull()?.address ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Earn Vaults") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OwnerAddressDropdown(
                    wallets = ownerOptions,
                    selectedAddress = ownerAddress,
                    onSelect = { ownerAddress = it }
                )
            }

            errorMessage?.let { message ->
                item { ErrorMessageView(message = message) }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            details?.let { vault ->
                item {
                    ValueCard(
                        title = "${vault.curator} ${vault.assetSymbol}",
                        value = buildString {
                            append("APY ${formatPercent(vault.netApy)}  •  TVL ${formatUsd(vault.tvlUsd)}\n")
                            append("Chain ${vault.chainId}  •  ${vault.provider?.name ?: "—"}  •  ${vault.assetDecimals} decimals\n")
                            append(vault.contractAddress)
                        }
                    )
                }

                position?.let { pos ->
                    item {
                        ValueCard(
                            title = "Position",
                            value = "Shares ${pos.shares} (raw)\n" +
                                "Assets ${formatUnits(pos.assets, vault.assetDecimals)} ${vault.assetSymbol}"
                        )
                    }
                }

                performance?.let { perf ->
                    item {
                        ValueCard(
                            title = "Performance",
                            value = "Net earnings ${formatUnits(perf.netEarningsAssets, vault.assetDecimals)} ${vault.assetSymbol}\n" +
                                "Return on capital ${formatPercent(perf.returnOnCapital)}"
                        )
                    }
                }

                if (rewards.isNotEmpty()) {
                    item {
                        ValueCard(
                            title = "Rewards",
                            value = rewards.joinToString("\n") {
                                "${formatUnits(it.accrued ?: "0", it.tokenDecimals)} ${it.tokenSymbol ?: it.tokenAddress ?: "token"}"
                            }
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (${vault.assetSymbol})") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(
                            title = "Deposit",
                            onClick = {
                                selectedVaultId?.let { viewModel.runAction(it, "deposit", amount, ownerAddress) }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryButton(
                            title = "Withdraw",
                            onClick = {
                                selectedVaultId?.let { viewModel.runAction(it, "withdraw", amount, ownerAddress) }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryButton(
                            title = "Claim",
                            onClick = {
                                selectedVaultId?.let { viewModel.runAction(it, "claim", amount, ownerAddress) }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                actionResult?.let { result ->
                    item { ValueCard(title = "Result", value = result) }
                }
            }

            if (vaults.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = vaultQuery,
                            onValueChange = { vaultQuery = it },
                            label = { Text("Filter vaults (curator, asset, provider, chain)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = chainFilter == null,
                                    onClick = { chainFilter = null },
                                    label = { Text("All chains") }
                                )
                            }
                            items(chainOptions) { chain ->
                                FilterChip(
                                    selected = chainFilter == chain,
                                    onClick = { chainFilter = if (chainFilter == chain) null else chain },
                                    label = { Text(chain.value) }
                                )
                            }
                        }
                        Text(
                            "${filteredVaults.size} of ${vaults.size} vaults",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            items(filteredVaults) { vault ->
                Card(
                    onClick = {
                        val options = wallets.compatibleWith(vault.chainName)
                        val owner = options.firstOrNull { it.address.equals(ownerAddress, ignoreCase = true) }?.address
                            ?: options.firstOrNull()?.address
                            ?: ""
                        selectedVaultId = vault.vaultId
                        ownerAddress = owner
                        viewModel.loadVault(vault.vaultId, owner)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "${vault.curator} ${vault.assetSymbol}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "APY ${formatPercent(vault.netApy)}  •  TVL ${formatUsd(vault.tvlUsd)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Chain ${vault.chainId}  •  ${vault.provider?.name ?: "—"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
