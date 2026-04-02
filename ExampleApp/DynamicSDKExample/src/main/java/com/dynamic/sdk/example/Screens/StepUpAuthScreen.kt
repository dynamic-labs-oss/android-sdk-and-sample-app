package com.dynamic.sdk.example.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.example.Components.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepUpAuthScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: StepUpAuthViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val resultMessage by viewModel.resultMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var scopeInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Step-Up Auth",
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
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Scope input
            TextFieldWithLabel(
                label = "Scope",
                placeholder = "Enter scope (e.g. transfer)",
                value = scopeInput,
                onValueChange = { scopeInput = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Check if step-up required
            FilledTonalButton(
                onClick = { viewModel.checkStepUpRequired(scopeInput) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && scopeInput.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Check isStepUpRequired", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prompt step-up auth
            FilledTonalButton(
                onClick = { viewModel.promptStepUpAuth() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Prompt Step-Up Auth", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prompt MFA
            FilledTonalButton(
                onClick = { viewModel.promptMfa() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Prompt MFA", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Prompt Reauthenticate
            FilledTonalButton(
                onClick = { viewModel.promptReauthenticate() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Prompt Reauthenticate", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reset State
            FilledTonalButton(
                onClick = { viewModel.resetState() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Reset State", fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error message
            if (errorMessage != null) {
                ErrorMessageView(
                    message = errorMessage!!,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Result message
            if (resultMessage != null) {
                SuccessMessageView(
                    message = resultMessage!!,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

class StepUpAuthViewModel : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _resultMessage = MutableStateFlow<String?>(null)
    val resultMessage: StateFlow<String?> = _resultMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun checkStepUpRequired(scope: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _resultMessage.value = null
            try {
                val required = sdk.stepUpAuth.isStepUpRequired(scope)
                _resultMessage.value = "Step-up required for \"$scope\": $required"
            } catch (e: Exception) {
                _errorMessage.value = "Failed to check step-up: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun promptStepUpAuth() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _resultMessage.value = null
            try {
                val token = sdk.stepUpAuth.promptStepUpAuth()
                _resultMessage.value = "Step-up auth token: ${token ?: "nil"}"
            } catch (e: Exception) {
                _errorMessage.value = "Step-up auth failed: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun promptMfa() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _resultMessage.value = null
            try {
                val token = sdk.stepUpAuth.promptMfa()
                _resultMessage.value = "MFA token: ${token ?: "nil"}"
            } catch (e: Exception) {
                _errorMessage.value = "MFA prompt failed: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun promptReauthenticate() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _resultMessage.value = null
            try {
                val token = sdk.stepUpAuth.promptReauthenticate()
                _resultMessage.value = "Reauthenticate token: ${token ?: "nil"}"
            } catch (e: Exception) {
                _errorMessage.value = "Reauthenticate failed: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun resetState() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _resultMessage.value = null
            try {
                sdk.stepUpAuth.resetState()
                _resultMessage.value = "Step-up auth state reset successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Reset state failed: ${e.message}"
            }
            _isLoading.value = false
        }
    }
}
