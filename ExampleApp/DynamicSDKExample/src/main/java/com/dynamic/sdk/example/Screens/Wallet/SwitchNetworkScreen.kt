package com.dynamic.sdk.example.Screens.Wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import com.dynamic.sdk.android.Models.BaseWallet
import com.dynamic.sdk.android.Models.Network
import com.dynamic.sdk.android.Module.GenericNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

data class NetworkItem(
    val name: String,
    val chainId: Int?,
    val networkId: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchNetworkScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet,
    onDismiss: () -> Unit
) {
    val viewModel: SwitchNetworkViewModel = viewModel(
        factory = SwitchNetworkViewModelFactory(wallet)
    )
    val networks by viewModel.networks.collectAsState()
    val currentNetworkId by viewModel.currentNetworkId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSuccessAlertPresented by viewModel.isSuccessAlertPresented.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    if (isSuccessAlertPresented) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissSuccess()
                onDismiss()
            },
            title = { Text("Success") },
            text = { Text("Network switched successfully") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissSuccess()
                    onDismiss()
                }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Switch Network",
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
        ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.load() }) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(networks) { network ->
                        val isActive = viewModel.isActive(network)
                        NetworkListItem(
                            network = network,
                            subtitle = viewModel.subtitle(network),
                            isActive = isActive,
                            onClick = {
                                if (!isActive) {
                                    viewModel.select(network)
                                }
                            }
                        )
                        Divider()
                    }
                }
            }
        }
        }
    }
}

@Composable
fun NetworkListItem(
    network: NetworkItem,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isActive, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = network.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isActive) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = Color.Green.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "Active",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Green,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Active",
                tint = Color.Green
            )
        }
    }
}

class SwitchNetworkViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _networks = MutableStateFlow<List<NetworkItem>>(emptyList())
    val networks: StateFlow<List<NetworkItem>> = _networks.asStateFlow()

    private val _currentNetworkId = MutableStateFlow<String?>(null)
    val currentNetworkId: StateFlow<String?> = _currentNetworkId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSuccessAlertPresented = MutableStateFlow(false)
    val isSuccessAlertPresented: StateFlow<Boolean> = _isSuccessAlertPresented.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Get current network
                try {
                    val currentNetwork = sdk.wallets.getNetwork(wallet)
                    val jsonValue = currentNetwork.value
                    _currentNetworkId.value = if (jsonValue is JsonPrimitive) {
                        jsonValue.intOrNull?.toString() ?: jsonValue.contentOrNull ?: jsonValue.toString()
                    } else {
                        jsonValue.toString()
                    }
                } catch (e: Exception) {
                    // Ignore
                }

                // Get available networks from SDK (environment-configured networks)
                val networkList = mutableListOf<NetworkItem>()

                if (wallet.chain.uppercase() == "EVM") {
                    // Use networks from SDK instead of hardcoded list
                    networkList.addAll(sdk.networks.evm.map { network ->
                        val chainId = when (val v = network.chainId) {
                            is Int -> v
                            is String -> v.toIntOrNull()
                            else -> null
                        }
                        NetworkItem(network.name, chainId, null)
                    })
                } else if (wallet.chain.uppercase() == "SOL") {
                    // Use networks from SDK instead of hardcoded list
                    networkList.addAll(sdk.networks.solana.map { network ->
                        val networkId = when (val v = network.networkId) {
                            is String -> v
                            is Int, is Long -> v.toString()
                            else -> v.toString()
                        }
                        NetworkItem(network.name, null, networkId)
                    })
                }

                _networks.value = networkList
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load networks: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    fun subtitle(network: NetworkItem): String {
        return when {
            network.chainId != null -> "Chain ID: ${network.chainId}"
            network.networkId != null -> "Network: ${network.networkId}"
            else -> ""
        }
    }

    fun isActive(network: NetworkItem): Boolean {
        val current = _currentNetworkId.value ?: return false
        return when {
            network.chainId != null -> current == network.chainId.toString()
            network.networkId != null -> current == network.networkId
            else -> false
        }
    }

    fun select(network: NetworkItem) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val networkValue = when {
                    network.chainId != null -> Network.evm(network.chainId)
                    network.networkId != null -> Network.solana(network.networkId)
                    else -> throw Exception("Invalid network")
                }

                sdk.wallets.switchNetwork(wallet, networkValue)
                _currentNetworkId.value = when {
                    network.chainId != null -> network.chainId.toString()
                    network.networkId != null -> network.networkId
                    else -> null
                }
                _isSuccessAlertPresented.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to switch network: ${e.message}"
            }

            _isLoading.value = false
        }
    }

    fun dismissSuccess() {
        _isSuccessAlertPresented.value = false
    }
}

class SwitchNetworkViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SwitchNetworkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SwitchNetworkViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
