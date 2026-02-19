package com.dynamic.sdk.example.Screens.Wallet

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.solanaweb3.*
import kotlinx.serialization.json.jsonPrimitive
import org.sol4k.PublicKey
import java.math.BigDecimal

// Luxe palette
private val Surface = Color(0xFFF7F8FA)
private val Card = Color(0xFFFFFFFF)
private val Border = Color(0xFFE8EBF0)
private val Label = Color(0xFF8A8F9C)
private val TextPrimary = Color(0xFF1A1D26)
private val Accent = Color(0xFF10B981)
private val Error = Color(0xFFEF4444)
private val SolPurple = Color(0xFF9945FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolanaSendTokenScreen(
    onNavigateBack: () -> Unit,
    wallet: BaseWallet
) {
    val viewModel: SolanaSendTokenViewModel = viewModel(
        factory = SolanaSendTokenViewModelFactory(wallet)
    )
    val tokens by viewModel.tokens.collectAsState()
    val selectedToken by viewModel.selectedToken.collectAsState()
    val amount by viewModel.amount.collectAsState()
    val recipientAddress by viewModel.recipientAddress.collectAsState()
    val isLoadingTokens by viewModel.isLoadingTokens.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val transactionSignature by viewModel.transactionSignature.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = Surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "SEND TOKEN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.8.sp,
                        color = Label
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Filled.ArrowBackIosNew,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp),
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    scrolledContainerColor = Surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // From card
            FromCard(wallet = wallet)

            Spacer(modifier = Modifier.height(16.dp))

            // Token selector
            TokenSelectorCard(
                tokens = tokens,
                selectedToken = selectedToken,
                isLoading = isLoadingTokens,
                onSelect = { viewModel.selectToken(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Amount input
            if (selectedToken != null) {
                AmountCard(
                    token = selectedToken!!,
                    amount = amount,
                    onAmountChange = { viewModel.updateAmount(it) },
                    onMaxTap = { viewModel.setMaxAmount() }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Recipient
            RecipientCard(
                value = recipientAddress,
                onValueChange = { viewModel.updateRecipientAddress(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error
            errorMessage?.let { err ->
                ErrorBanner(message = err)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Success
            transactionSignature?.let { sig ->
                SuccessBanner(
                    signature = sig,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(sig))
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Send button
            SendButton(
                symbol = selectedToken?.symbol ?: "Token",
                enabled = viewModel.isFormValid() && !isSending,
                isLoading = isSending,
                onClick = {
                    focusManager.clearFocus()
                    viewModel.sendToken()
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FromCard(wallet: BaseWallet) {
    val addr = wallet.address
    val truncated = if (addr.length > 16)
        "${addr.take(8)}...${addr.takeLast(8)}"
    else addr

    LuxeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SolPurple.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("◎", fontSize = 18.sp, color = SolPurple)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                LuxeLabel("FROM")
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    truncated,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun TokenSelectorCard(
    tokens: List<TokenBalance>,
    selectedToken: TokenBalance?,
    isLoading: Boolean,
    onSelect: (TokenBalance) -> Unit
) {
    LuxeCard {
        LuxeLabel("SELECT TOKEN")
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Label
                )
            }
        } else if (tokens.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No tokens with balance found", fontSize = 13.sp, color = Label)
            }
        } else {
            tokens.forEach { token ->
                TokenRow(
                    token = token,
                    isSelected = selectedToken?.contractAddress == token.contractAddress &&
                            selectedToken?.networkId == token.networkId,
                    onClick = { onSelect(token) }
                )
            }
        }
    }
}

@Composable
private fun TokenRow(
    token: TokenBalance,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (isSelected) Accent.copy(alpha = 0.06f) else Color.Transparent,
        label = "tokenBg"
    )
    val borderColor by animateColorAsState(
        if (isSelected) Accent.copy(alpha = 0.3f) else Color.Transparent,
        label = "tokenBorder"
    )
    val balance = token.balanceDecimal ?: token.balance
    val symbol = token.symbol ?: "???"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TokenIcon(symbol)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                symbol,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            token.name?.let { name ->
                Text(name, fontSize = 11.sp, color = Label)
            }
        }
        Text(
            balance,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary
        )
    }
}

@Composable
private fun TokenIcon(symbol: String) {
    val bg = when (symbol.uppercase()) {
        "USDC" -> Color(0xFF2775CA)
        "USDT" -> Color(0xFF26A17B)
        "SOL" -> SolPurple
        else -> Color(0xFF6366F1)
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            symbol.take(2).uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = bg
        )
    }
}

@Composable
private fun AmountCard(
    token: TokenBalance,
    amount: String,
    onAmountChange: (String) -> Unit,
    onMaxTap: () -> Unit
) {
    val balance = token.balanceDecimal ?: token.balance
    val symbol = token.symbol ?: ""

    LuxeCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LuxeLabel("AMOUNT")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Balance: $balance $symbol", fontSize = 11.sp, color = Label)
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Accent.copy(alpha = 0.1f))
                        .clickable { onMaxTap() }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "MAX",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Accent,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            placeholder = {
                Text(
                    "0.00",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Monospace,
                    color = Label.copy(alpha = 0.4f)
                )
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun RecipientCard(
    value: String,
    onValueChange: (String) -> Unit
) {
    LuxeCard {
        LuxeLabel("RECIPIENT")
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text("Solana address", fontSize = 14.sp, color = Label.copy(alpha = 0.5f))
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Error.copy(alpha = 0.06f))
            .border(1.dp, Error.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Error.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            message,
            fontSize = 12.sp,
            color = Error.copy(alpha = 0.9f),
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun SuccessBanner(signature: String, onCopy: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Accent.copy(alpha = 0.06f))
            .border(1.dp, Accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .clickable { onCopy() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.CheckCircleOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Accent.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Transaction Sent",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Accent
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            signature,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Accent.copy(alpha = 0.8f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SendButton(
    symbol: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (enabled) TextPrimary else Border,
        label = "sendBg"
    )
    val textColor by animateColorAsState(
        if (enabled) Color.White else Label,
        label = "sendText"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(
                "Send $symbol",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                letterSpacing = 0.3.sp
            )
        }
    }
}

@Composable
private fun LuxeCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Card)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun LuxeLabel(text: String) {
    Text(
        text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = Label
    )
}

// ViewModel

class SolanaSendTokenViewModel(private val wallet: BaseWallet) : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _tokens = MutableStateFlow<List<TokenBalance>>(emptyList())
    val tokens: StateFlow<List<TokenBalance>> = _tokens.asStateFlow()

    private val _selectedToken = MutableStateFlow<TokenBalance?>(null)
    val selectedToken: StateFlow<TokenBalance?> = _selectedToken.asStateFlow()

    private val _amount = MutableStateFlow("")
    val amount: StateFlow<String> = _amount.asStateFlow()

    private val _recipientAddress = MutableStateFlow("")
    val recipientAddress: StateFlow<String> = _recipientAddress.asStateFlow()

    private val _isLoadingTokens = MutableStateFlow(true)
    val isLoadingTokens: StateFlow<Boolean> = _isLoadingTokens.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _transactionSignature = MutableStateFlow<String?>(null)
    val transactionSignature: StateFlow<String?> = _transactionSignature.asStateFlow()

    init {
        loadTokens()
    }

    private fun loadTokens() {
        viewModelScope.launch {
            _isLoadingTokens.value = true
            _errorMessage.value = null

            try {
                val solanaNetworks = sdk.networks.solana
                val networkIds = solanaNetworks.mapNotNull { network ->
                     network.chainId.jsonPrimitive.content.toInt()
                }

                val response = sdk.wallets.getMultichainBalances(
                    MultichainBalanceRequest(
                        balanceRequests = listOf(
                            BalanceRequestItem(
                                address = wallet.address,
                                chain = ChainEnum.SOL,
                                networkIds = networkIds
                            )
                        ),
                        filterSpamTokens = true
                    )
                )

                _tokens.value = response.balances.filter { b ->
                    b.symbol != null &&
                            b.balance != "0" &&
                            (b.balanceDecimal ?: b.balance).toDoubleOrNull()?.let { it > 0.0 } == true
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load tokens: ${e.message}\n${e.stackTraceToString().take(500)}"
            }

            _isLoadingTokens.value = false
        }
    }

    fun selectToken(token: TokenBalance) {
        _selectedToken.value = token
        _amount.value = ""
        _errorMessage.value = null
        _transactionSignature.value = null
    }

    fun updateAmount(value: String) { _amount.value = value }
    fun updateRecipientAddress(value: String) { _recipientAddress.value = value }

    fun setMaxAmount() {
        _selectedToken.value?.let { token ->
            _amount.value = token.balanceDecimal ?: token.balance
        }
    }

    fun isFormValid(): Boolean {
        val token = _selectedToken.value ?: return false
        if (token.contractAddress.isNullOrEmpty()) return false
        if (_recipientAddress.value.isBlank()) return false
        val amt = _amount.value.replace(",", ".").toDoubleOrNull() ?: return false
        return amt > 0
    }

    fun sendToken() {
        if (!isFormValid()) return

        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null
            _transactionSignature.value = null

            try {
                val token = _selectedToken.value!!
                val decimals = token.decimals ?: 9
                val amountDouble = _amount.value.replace(",", ".").toDouble()
                val rawAmount = BigDecimal(amountDouble.toString())
                    .multiply(BigDecimal.TEN.pow(decimals))
                    .toLong()

                val ownerPubkey = PublicKey(wallet.address)
                val recipientPubkey = PublicKey(_recipientAddress.value.trim())
                val mintPubkey = PublicKey(token.contractAddress!!)

                val sourceAta = TokenProgram.getAssociatedTokenAddress(
                    mint = mintPubkey,
                    owner = ownerPubkey
                )
                val destAta = TokenProgram.getAssociatedTokenAddress(
                    mint = mintPubkey,
                    owner = recipientPubkey
                )

                val instructions = mutableListOf<TransactionInstruction>()

                // Create ATA for recipient (idempotent) if not exists
                val connection = Connection(Cluster.DEVNET)
                val destExists = connection.accountExists(destAta)
                if (!destExists) {
                    instructions.add(
                        TokenProgram.createAssociatedTokenAccountInstruction(
                            payer = ownerPubkey,
                            associatedToken = destAta,
                            owner = recipientPubkey,
                            mint = mintPubkey
                        )
                    )
                }

                instructions.add(
                    TokenProgram.transferChecked(
                        source = sourceAta,
                        mint = mintPubkey,
                        destination = destAta,
                        owner = ownerPubkey,
                        amount = rawAmount,
                        decimals = decimals
                    )
                )

                val blockhash = connection.getLatestBlockhash()
                val transaction = Transaction.v0(
                    payer = ownerPubkey,
                    instructions = instructions,
                    recentBlockhash = blockhash.blockhash
                )

                val base64 = transaction.serializeUnsignedToBase64()
                val signature = sdk.solana.signAndSendTransaction(base64, wallet)
                _transactionSignature.value = signature

            } catch (e: Exception) {
                _errorMessage.value = "${e.message}\n\n${e.stackTraceToString().take(800)}"
            }

            _isSending.value = false
        }
    }
}

class SolanaSendTokenViewModelFactory(private val wallet: BaseWallet) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SolanaSendTokenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SolanaSendTokenViewModel(wallet) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
