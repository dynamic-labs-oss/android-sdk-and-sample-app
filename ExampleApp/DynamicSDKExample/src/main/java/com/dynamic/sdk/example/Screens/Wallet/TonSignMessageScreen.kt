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
fun TonSignMessageScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: TonSignMessageViewModel = viewModel(
        factory = TonSignMessageViewModelFactory(wallet)
    )
    val message by viewModel.message.collectAsState()
    val signature by viewModel.signature.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "TON Sign Message",
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
                label = "Message",
                placeholder = "Enter message to sign",
                value = message,
                onValueChange = { viewModel.updateMessage(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let { error ->
                ErrorMessageView(message = error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            PrimaryButton(
                title = "Sign Message",
                onClick = { viewModel.signMessage() },
                isLoading = isLoading,
                isDisabled = message.isBlank()
            )

            Spacer(modifier = Modifier.height(16.dp))

            signature?.let { sig ->
                InfoCard(title = "Signature", content = sig)
                Spacer(modifier = Modifier.height(8.dp))
                SuccessMessageView(message = "Message signed successfully!")
            }
        }
    }
}

class TonSignMessageViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _message = MutableStateFlow("Hello World")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _signature = MutableStateFlow<String?>(null)
    val signature: StateFlow<String?> = _signature.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun updateMessage(value: String) { _message.value = value }

    fun signMessage() {
        val walletId = wallet.id ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _signature.value = null

            try {
                _signature.value = sdk.ton.signMessage(
                    walletId = walletId,
                    message = _message.value.trim()
                )
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to sign message"
            }

            _isLoading.value = false
        }
    }
}

class TonSignMessageViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TonSignMessageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TonSignMessageViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
