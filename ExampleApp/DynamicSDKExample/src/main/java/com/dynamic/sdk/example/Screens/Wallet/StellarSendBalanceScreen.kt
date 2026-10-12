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
fun StellarSendBalanceScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: StellarSendBalanceViewModel = viewModel(
        factory = StellarSendBalanceViewModelFactory(wallet)
    )
    val recipientAddress by viewModel.recipientAddress.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val tokenAddress by viewModel.tokenAddress.collectAsState()
    val hash by viewModel.hash.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Stellar Send Balance",
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
                        text = "Send XLM to a recipient address. Provide an asset in CODE:ISSUER form to send a non-native asset.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextFieldWithLabel(
                label = "Recipient Address",
                placeholder = "G...",
                value = recipientAddress,
                onValueChange = { viewModel.updateRecipientAddress(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextFieldWithLabel(
                label = "Amount",
                placeholder = "0.01",
                value = amount,
                onValueChange = { viewModel.updateAmount(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextFieldWithLabel(
                label = "Asset (optional, CODE:ISSUER)",
                placeholder = "USDC:GA5Z...",
                value = tokenAddress,
                onValueChange = { viewModel.updateTokenAddress(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let { error ->
                ErrorMessageView(message = error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            PrimaryButton(
                title = "Send",
                onClick = { viewModel.sendBalance() },
                isLoading = isLoading,
                isDisabled = recipientAddress.isBlank() || amount.isBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

            hash?.let { txHash ->
                InfoCard(title = "Hash", content = txHash)
                Spacer(modifier = Modifier.height(8.dp))
                SuccessMessageView(message = "Balance sent successfully!")
            }
        }
    }
}

class StellarSendBalanceViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _recipientAddress = MutableStateFlow("")
    val recipientAddress: StateFlow<String> = _recipientAddress.asStateFlow()

    private val _amount = MutableStateFlow("0.01")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _tokenAddress = MutableStateFlow("")
    val tokenAddress: StateFlow<String> = _tokenAddress.asStateFlow()

    private val _hash = MutableStateFlow<String?>(null)
    val hash: StateFlow<String?> = _hash.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateRecipientAddress(value: String) { _recipientAddress.value = value }
    fun updateAmount(value: String) { _amount.value = value }
    fun updateTokenAddress(value: String) { _tokenAddress.value = value }

    fun sendBalance() {
        val walletId = wallet.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _hash.value = null

            val trimmedToken = _tokenAddress.value.trim()

            try {
                _hash.value = sdk.stellar.sendBalance(
                    walletId = walletId,
                    toAddress = _recipientAddress.value.trim(),
                    amount = _amount.value.trim(),
                    tokenAddress = trimmedToken.ifEmpty { null }
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to send balance"
            }

            _isLoading.value = false
        }
    }
}

class StellarSendBalanceViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StellarSendBalanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StellarSendBalanceViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
