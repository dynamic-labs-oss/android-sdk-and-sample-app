package com.dynamic.sdk.example.Screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.BaseWallet
import com.dynamic.sdk.android.Models.EmbeddedWalletChain
import com.dynamic.sdk.android.Models.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel : ViewModel() {
    private val sdk = DynamicSDK.getInstance()
    private var walletCreationTimer: Job? = null

    private val _wallets = MutableStateFlow<List<BaseWallet>>(emptyList())
    val wallets: StateFlow<List<BaseWallet>> = _wallets.asStateFlow()

    private val _user = MutableStateFlow<UserProfile?>(null)
    val user: StateFlow<UserProfile?> = _user.asStateFlow()

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    private val _minAuthToken = MutableStateFlow<String?>(null)
    val minAuthToken: StateFlow<String?> = _minAuthToken.asStateFlow()

    private val _isCreatingWallets = MutableStateFlow(false)
    val isCreatingWallets: StateFlow<Boolean> = _isCreatingWallets.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var onNavigateToLogin: (() -> Unit)? = null

    fun startListening(onNavigateToLogin: () -> Unit) {
        this.onNavigateToLogin = onNavigateToLogin

        // Listen for auth changes
        viewModelScope.launch {
            sdk.auth.authenticatedUserChanges.collect { userProfile ->
                _user.value = userProfile
                if (userProfile == null) {
                    onNavigateToLogin()
                } else {
                    checkIfCreatingWallets()
                }
            }
        }

        // Listen for token changes
        viewModelScope.launch {
            sdk.auth.tokenChanges.collect { authToken ->
                _token.value = authToken
            }
        }

        // Listen for minified auth token changes
        viewModelScope.launch {
            sdk.auth.minAuthTokenChanges.collect { minToken ->
                _minAuthToken.value = minToken
            }
        }

        // Listen for wallet changes
        viewModelScope.launch {
            sdk.wallets.userWalletsChanges.collect { walletList ->
                _wallets.value = walletList
                checkIfCreatingWallets()
            }
        }
    }

    fun showUserProfile() {
        sdk.ui.showUserProfile()
    }

    fun logout() {
        viewModelScope.launch {
            try {
                sdk.auth.logout()
            } catch (e: Exception) {
                _errorMessage.value = "Logout failed: ${e.message}"
            }
        }
    }

    fun getWalletByAddress(address: String): BaseWallet? {
        return _wallets.value.find { it.address == address }
    }

    fun createWallet(chain: EmbeddedWalletChain) {
        _errorMessage.value = null
        _isCreatingWallets.value = true
        walletCreationTimer?.cancel()
        walletCreationTimer = null
        viewModelScope.launch {
            try {
                sdk.wallets.embedded.createWallet(chain = chain)
                // Wallets list will update via userWalletsChanges
            } catch (e: Exception) {
                _isCreatingWallets.value = false
                _errorMessage.value = "Create wallet failed: ${e.message}"
            }
        }
    }

    private fun checkIfCreatingWallets() {
        val currentUser = sdk.auth.authenticatedUser
        val currentWallets = sdk.wallets.userWallets

        if (currentUser != null && currentWallets.isEmpty()) {
            _isCreatingWallets.value = true
            walletCreationTimer?.cancel()
            walletCreationTimer = viewModelScope.launch {
                delay(10000) // 10 second timeout
                _isCreatingWallets.value = false
            }
        } else {
            _isCreatingWallets.value = false
            walletCreationTimer?.cancel()
        }
    }
}
