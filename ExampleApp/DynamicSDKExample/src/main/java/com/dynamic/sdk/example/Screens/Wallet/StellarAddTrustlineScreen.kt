package com.dynamic.sdk.example.Screens.Wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.BaseWallet
import com.dynamic.sdk.example.Components.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StellarAddTrustlineScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: StellarAddTrustlineViewModel = viewModel(
        factory = StellarAddTrustlineViewModelFactory(wallet)
    )
    val assetCode by viewModel.assetCode.collectAsState()
    val assetIssuer by viewModel.assetIssuer.collectAsState()
    val limit by viewModel.limit.collectAsState()
    val hash by viewModel.hash.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Stellar Add Trustline",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Add a trustline so this account can hold a non-native Stellar asset.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextFieldWithLabel(
                label = "Asset Code",
                placeholder = "USDC",
                value = assetCode,
                onValueChange = { viewModel.updateAssetCode(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextFieldWithLabel(
                label = "Asset Issuer",
                placeholder = "G...",
                value = assetIssuer,
                onValueChange = { viewModel.updateAssetIssuer(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextFieldWithLabel(
                label = "Limit (optional)",
                placeholder = "1000000",
                value = limit,
                onValueChange = { viewModel.updateLimit(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let { error ->
                ErrorMessageView(message = error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            PrimaryButton(
                title = "Add Trustline",
                onClick = { viewModel.addTrustline() },
                isLoading = isLoading,
                isDisabled = assetCode.isBlank() || assetIssuer.isBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

            hash?.let { txHash ->
                InfoCard(title = "Hash", content = txHash)
                Spacer(modifier = Modifier.height(8.dp))
                SuccessMessageView(message = "Trustline added successfully!")
            }
        }
    }
}

class StellarAddTrustlineViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _assetCode = MutableStateFlow("")
    val assetCode: StateFlow<String> = _assetCode.asStateFlow()

    private val _assetIssuer = MutableStateFlow("")
    val assetIssuer: StateFlow<String> = _assetIssuer.asStateFlow()

    private val _limit = MutableStateFlow("")
    val limit: StateFlow<String> = _limit.asStateFlow()

    private val _hash = MutableStateFlow<String?>(null)
    val hash: StateFlow<String?> = _hash.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateAssetCode(value: String) { _assetCode.value = value }
    fun updateAssetIssuer(value: String) { _assetIssuer.value = value }
    fun updateLimit(value: String) { _limit.value = value }

    fun addTrustline() {
        val walletId = wallet.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _hash.value = null

            val trimmedLimit = _limit.value.trim()

            try {
                _hash.value = sdk.stellar.addTrustline(
                    walletId = walletId,
                    assetCode = _assetCode.value.trim(),
                    assetIssuer = _assetIssuer.value.trim(),
                    limit = trimmedLimit.ifEmpty { null }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to add trustline"
            }

            _isLoading.value = false
        }
    }
}

class StellarAddTrustlineViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StellarAddTrustlineViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StellarAddTrustlineViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
