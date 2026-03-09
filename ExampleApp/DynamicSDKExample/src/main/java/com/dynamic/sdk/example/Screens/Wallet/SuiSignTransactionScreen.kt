package com.dynamic.sdk.example.Screens.Wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
fun SuiSignTransactionScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: SuiSignTransactionViewModel = viewModel(
        factory = SuiSignTransactionViewModelFactory(wallet)
    )
    val recipientAddress by viewModel.recipientAddress.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val rawTransaction by viewModel.rawTransaction.collectAsState()
    val useRawTransaction by viewModel.useRawTransaction.collectAsState()
    val signature by viewModel.signature.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SUI Sign Transaction",
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
            // Raw Transaction Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Raw Transaction",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = useRawTransaction,
                    onCheckedChange = { viewModel.toggleRawTransaction(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (useRawTransaction) {
                // Raw Transaction Input
                TextFieldWithLabel(
                    label = "Transaction (base64)",
                    placeholder = "Base64 encoded transaction",
                    value = rawTransaction,
                    onValueChange = { viewModel.updateRawTransaction(it) }
                )
            } else {
                // Recipient Address
                TextFieldWithLabel(
                    label = "Recipient Address",
                    placeholder = "0x...",
                    value = recipientAddress,
                    onValueChange = { viewModel.updateRecipientAddress(it) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Amount
                TextFieldWithLabel(
                    label = "Amount (SUI)",
                    placeholder = "0.001",
                    value = amount,
                    onValueChange = { viewModel.updateAmount(it) },
                    keyboardType = KeyboardType.Decimal
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message
            errorMessage?.let { error ->
                ErrorMessageView(message = error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Sign Button
            PrimaryButton(
                title = "Sign Transaction",
                onClick = { viewModel.signTransaction() },
                isLoading = isLoading,
                isDisabled = !viewModel.isFormValid()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Signature Result
            signature?.let { sig ->
                InfoCard(
                    title = "Transaction Signature",
                    content = sig
                )
                Spacer(modifier = Modifier.height(8.dp))
                SuccessMessageView(message = "Transaction signed successfully!")
            }
        }
    }
}

class SuiSignTransactionViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _recipientAddress = MutableStateFlow("")
    val recipientAddress: StateFlow<String> = _recipientAddress.asStateFlow()

    private val _amount = MutableStateFlow("0.001")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _rawTransaction = MutableStateFlow("")
    val rawTransaction: StateFlow<String> = _rawTransaction.asStateFlow()

    private val _useRawTransaction = MutableStateFlow(false)
    val useRawTransaction: StateFlow<Boolean> = _useRawTransaction.asStateFlow()

    private val _signature = MutableStateFlow<String?>(null)
    val signature: StateFlow<String?> = _signature.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateRecipientAddress(value: String) { _recipientAddress.value = value }
    fun updateAmount(value: String) { _amount.value = value }
    fun updateRawTransaction(value: String) { _rawTransaction.value = value }
    fun toggleRawTransaction(value: Boolean) {
        _useRawTransaction.value = value
        _signature.value = null
        _errorMessage.value = null
    }

    fun isFormValid(): Boolean {
        return if (_useRawTransaction.value) {
            _rawTransaction.value.isNotBlank()
        } else {
            _recipientAddress.value.isNotBlank() &&
                    _amount.value.isNotBlank() &&
                    _amount.value.toDoubleOrNull() != null &&
                    _amount.value.toDoubleOrNull()!! > 0
        }
    }

    fun signTransaction() {
        val walletId = wallet.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _signature.value = null

            try {
                val result = if (_useRawTransaction.value) {
                    sdk.sui.signTransaction(
                        walletId = walletId,
                        transaction = _rawTransaction.value.trim()
                    )
                } else {
                    sdk.sui.signTransferTransaction(
                        walletId = walletId,
                        to = _recipientAddress.value.trim(),
                        value = _amount.value.trim()
                    )
                }
                _signature.value = result
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to sign transaction"
            }

            _isLoading.value = false
        }
    }
}

class SuiSignTransactionViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SuiSignTransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SuiSignTransactionViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
