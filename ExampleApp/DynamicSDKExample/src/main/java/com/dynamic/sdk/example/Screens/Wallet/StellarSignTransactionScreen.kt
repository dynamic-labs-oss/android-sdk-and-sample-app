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
fun StellarSignTransactionScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: StellarSignTransactionViewModel = viewModel(
        factory = StellarSignTransactionViewModelFactory(wallet)
    )
    val transactionXdr by viewModel.transactionXdr.collectAsState()
    val signedXdr by viewModel.signedXdr.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Stellar Sign Transaction",
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
                label = "Transaction XDR",
                placeholder = "AAAA...",
                value = transactionXdr,
                onValueChange = { viewModel.updateTransactionXdr(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let { error ->
                ErrorMessageView(message = error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            PrimaryButton(
                title = "Sign Transaction",
                onClick = { viewModel.signTransaction() },
                isLoading = isLoading,
                isDisabled = transactionXdr.isBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

            signedXdr?.let { signed ->
                InfoCard(title = "Signed Transaction XDR", content = signed)
                Spacer(modifier = Modifier.height(8.dp))
                SuccessMessageView(message = "Transaction signed successfully!")
            }
        }
    }
}

class StellarSignTransactionViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _transactionXdr = MutableStateFlow("")
    val transactionXdr: StateFlow<String> = _transactionXdr.asStateFlow()

    private val _signedXdr = MutableStateFlow<String?>(null)
    val signedXdr: StateFlow<String?> = _signedXdr.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateTransactionXdr(value: String) { _transactionXdr.value = value }

    fun signTransaction() {
        val walletId = wallet.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _signedXdr.value = null

            try {
                _signedXdr.value = sdk.stellar.signTransaction(
                    walletId = walletId,
                    transactionXdr = _transactionXdr.value.trim()
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to sign transaction"
            }

            _isLoading.value = false
        }
    }
}

class StellarSignTransactionViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StellarSignTransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StellarSignTransactionViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
