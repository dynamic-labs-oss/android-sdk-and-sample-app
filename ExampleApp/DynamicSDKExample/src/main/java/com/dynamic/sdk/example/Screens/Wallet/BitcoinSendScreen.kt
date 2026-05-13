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
fun BitcoinSendScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: BitcoinSendViewModel = viewModel(
        factory = BitcoinSendViewModelFactory(wallet)
    )
    val recipientAddress by viewModel.recipientAddress.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val txId by viewModel.txId.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Send Bitcoin",
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
                        text = "Wallet",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = wallet.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextFieldWithLabel(
                label = "Recipient Address",
                placeholder = "bc1...",
                value = recipientAddress,
                onValueChange = { viewModel.updateRecipientAddress(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextFieldWithLabel(
                label = "Amount (satoshis)",
                placeholder = "1000",
                value = amount,
                onValueChange = { viewModel.updateAmount(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let { error ->
                ErrorMessageView(message = error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            PrimaryButton(
                title = "Send Bitcoin",
                onClick = { viewModel.sendBitcoin() },
                isLoading = isLoading,
                isDisabled = !viewModel.isFormValid()
            )

            Spacer(modifier = Modifier.height(16.dp))

            txId?.let { id ->
                InfoCard(title = "Transaction ID", content = id)
                Spacer(modifier = Modifier.height(8.dp))
                SuccessMessageView(message = "Bitcoin sent successfully!")
            }
        }
    }
}

class BitcoinSendViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _recipientAddress = MutableStateFlow("")
    val recipientAddress: StateFlow<String> = _recipientAddress.asStateFlow()

    private val _amount = MutableStateFlow("1000")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _txId = MutableStateFlow<String?>(null)
    val txId: StateFlow<String?> = _txId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateRecipientAddress(value: String) { _recipientAddress.value = value }
    fun updateAmount(value: String) { _amount.value = value }

    fun isFormValid(): Boolean {
        return _recipientAddress.value.isNotBlank() &&
                _amount.value.toLongOrNull() != null &&
                (_amount.value.toLongOrNull() ?: 0) > 0
    }

    fun sendBitcoin() {
        val walletId = wallet.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _txId.value = null

            try {
                _txId.value = sdk.bitcoin.sendBitcoin(
                    walletId = walletId,
                    recipientAddress = _recipientAddress.value.trim(),
                    amount = _amount.value.trim()
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to send Bitcoin"
            }

            _isLoading.value = false
        }
    }
}

class BitcoinSendViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BitcoinSendViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BitcoinSendViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
