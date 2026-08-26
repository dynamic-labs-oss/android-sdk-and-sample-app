package com.dynamic.sdk.example.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.AddBusinessAccountSignerResponse
import com.dynamic.sdk.android.Models.BusinessAccount
import com.dynamic.sdk.android.Models.BusinessAccountDetail
import com.dynamic.sdk.android.Models.BusinessAccountList
import com.dynamic.sdk.android.Models.BusinessAccountMember
import com.dynamic.sdk.android.Models.BusinessAccountMemberRole
import com.dynamic.sdk.android.Models.BusinessAccountSigner
import com.dynamic.sdk.android.Models.BusinessAccountSignerIdentifierType
import com.dynamic.sdk.android.Models.BusinessAccountSignerType
import com.dynamic.sdk.android.Models.TargetIdentity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

interface BusinessAccountsClient {
    suspend fun list(externalRefs: List<String>? = null): BusinessAccountList

    suspend fun get(businessAccountId: String): BusinessAccountDetail

    suspend fun create(
        name: String? = null,
        externalRef: String? = null,
        metadata: Map<String, JsonElement>? = null
    ): BusinessAccount

    suspend fun update(businessAccountId: String, name: String): BusinessAccount

    suspend fun addWallet(
        businessAccountId: String? = null,
        walletId: String
    ): BusinessAccountDetail

    suspend fun removeWallet(
        businessAccountId: String,
        walletId: String
    ): BusinessAccountDetail

    suspend fun addMember(
        businessAccountId: String,
        userId: String? = null,
        identifier: String? = null,
        identifierType: BusinessAccountSignerIdentifierType? = null,
        role: BusinessAccountMemberRole? = null
    ): BusinessAccountMember

    suspend fun removeMember(
        businessAccountId: String,
        userId: String
    ): BusinessAccountMember

    suspend fun updateMemberRole(
        businessAccountId: String,
        userId: String,
        role: BusinessAccountMemberRole
    ): BusinessAccountMember

    suspend fun addSigner(
        accountAddress: String,
        chainName: String,
        targetSignerIdentity: TargetIdentity,
        businessAccountId: String? = null,
        walletId: String? = null,
        signerType: BusinessAccountSignerType? = null,
        password: String? = null
    ): AddBusinessAccountSignerResponse

    suspend fun removeSigner(
        businessAccountId: String,
        walletId: String,
        signerId: String
    ): BusinessAccountSigner

    suspend fun transferOwnership(
        businessAccountId: String,
        newOwnerUserId: String
    ): BusinessAccountDetail
}

class DynamicBusinessAccountsClient : BusinessAccountsClient {
    private val module = DynamicSDK.getInstance().businessAccounts

    override suspend fun list(externalRefs: List<String>?): BusinessAccountList {
        return module.list(externalRefs)
    }

    override suspend fun get(businessAccountId: String): BusinessAccountDetail {
        return module.get(businessAccountId)
    }

    override suspend fun create(
        name: String?,
        externalRef: String?,
        metadata: Map<String, JsonElement>?
    ): BusinessAccount {
        return module.create(name, externalRef, metadata)
    }

    override suspend fun update(
        businessAccountId: String,
        name: String
    ): BusinessAccount {
        return module.update(businessAccountId, name)
    }

    override suspend fun addWallet(
        businessAccountId: String?,
        walletId: String
    ): BusinessAccountDetail {
        return module.addWallet(businessAccountId, walletId)
    }

    override suspend fun removeWallet(
        businessAccountId: String,
        walletId: String
    ): BusinessAccountDetail {
        return module.removeWallet(businessAccountId, walletId)
    }

    override suspend fun addMember(
        businessAccountId: String,
        userId: String?,
        identifier: String?,
        identifierType: BusinessAccountSignerIdentifierType?,
        role: BusinessAccountMemberRole?
    ): BusinessAccountMember {
        return module.addMember(
            businessAccountId,
            userId,
            identifier,
            identifierType,
            role
        )
    }

    override suspend fun removeMember(
        businessAccountId: String,
        userId: String
    ): BusinessAccountMember {
        return module.removeMember(businessAccountId, userId)
    }

    override suspend fun updateMemberRole(
        businessAccountId: String,
        userId: String,
        role: BusinessAccountMemberRole
    ): BusinessAccountMember {
        return module.updateMemberRole(businessAccountId, userId, role)
    }

    override suspend fun addSigner(
        accountAddress: String,
        chainName: String,
        targetSignerIdentity: TargetIdentity,
        businessAccountId: String?,
        walletId: String?,
        signerType: BusinessAccountSignerType?,
        password: String?
    ): AddBusinessAccountSignerResponse {
        return module.addSigner(
            accountAddress,
            chainName,
            targetSignerIdentity,
            businessAccountId,
            walletId,
            signerType,
            password
        )
    }

    override suspend fun removeSigner(
        businessAccountId: String,
        walletId: String,
        signerId: String
    ): BusinessAccountSigner {
        return module.removeSigner(businessAccountId, walletId, signerId)
    }

    override suspend fun transferOwnership(
        businessAccountId: String,
        newOwnerUserId: String
    ): BusinessAccountDetail {
        return module.transferOwnership(businessAccountId, newOwnerUserId)
    }
}

data class BusinessAccountsUiState(
    val accounts: List<BusinessAccount> = emptyList(),
    val selectedAccountId: String? = null,
    val detail: BusinessAccountDetail? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BusinessAccountsViewModel(
    private val client: BusinessAccountsClient = DynamicBusinessAccountsClient(),
    private val workScope: CoroutineScope? = null
) : ViewModel() {
    private val mutableState = MutableStateFlow(BusinessAccountsUiState())
    val state: StateFlow<BusinessAccountsUiState> = mutableState.asStateFlow()

    fun loadAccounts(externalRefs: List<String>? = null) {
        launchAction {
            val result = client.list(externalRefs)
            val accounts = result.items.orEmpty()
            var selectedAccountId = mutableState.value.selectedAccountId
            val selectedAccountExists = accounts.any { it.id == selectedAccountId }
            if (!selectedAccountExists) {
                selectedAccountId = accounts.firstOrNull()?.id
            }

            val detail = selectedAccountId?.let { client.get(it) }
            mutableState.value = mutableState.value.copy(
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                detail = detail
            )
        }
    }

    fun selectAccount(accountId: String) {
        launchAction {
            val detail = client.get(accountId)
            mutableState.value = mutableState.value.copy(
                selectedAccountId = accountId,
                detail = detail
            )
        }
    }

    fun createAccount(name: String, externalRef: String?) {
        launchAction("Business account created") {
            val account = client.create(name = name, externalRef = externalRef)
            reloadAccounts(account.id)
        }
    }

    fun renameAccount(name: String) {
        withSelectedAccount("Business account renamed") { accountId ->
            client.update(accountId, name)
            reloadAccounts(accountId)
        }
    }

    fun addWallet(walletId: String) {
        withSelectedAccount("Wallet linked") { accountId ->
            val detail = client.addWallet(accountId, walletId)
            mutableState.value = mutableState.value.copy(detail = detail)
        }
    }

    fun removeWallet(walletId: String) {
        withSelectedAccount("Wallet removed") { accountId ->
            val detail = client.removeWallet(accountId, walletId)
            mutableState.value = mutableState.value.copy(detail = detail)
        }
    }

    fun addMember(
        userId: String?,
        email: String?,
        role: BusinessAccountMemberRole
    ) {
        withSelectedAccount("Member added") { accountId ->
            val identifierType = if (email == null) {
                null
            } else {
                BusinessAccountSignerIdentifierType.email
            }
            client.addMember(accountId, userId, email, identifierType, role)
            refreshDetail(accountId)
        }
    }

    fun removeMember(userId: String) {
        withSelectedAccount("Member removed") { accountId ->
            client.removeMember(accountId, userId)
            refreshDetail(accountId)
        }
    }

    fun updateMemberRole(userId: String, role: BusinessAccountMemberRole) {
        withSelectedAccount("Member role updated") { accountId ->
            client.updateMemberRole(accountId, userId, role)
            refreshDetail(accountId)
        }
    }

    fun addSigner(
        accountAddress: String,
        chainName: String,
        walletId: String,
        userId: String?,
        email: String?,
        signerType: BusinessAccountSignerType,
        password: String?
    ) {
        withSelectedAccount("Signer added") { accountId ->
            val identifierType = if (email == null) {
                null
            } else {
                BusinessAccountSignerIdentifierType.email
            }
            val targetIdentity = TargetIdentity(
                identifier = email,
                identifierType = identifierType,
                userId = userId
            )
            client.addSigner(
                accountAddress,
                chainName,
                targetIdentity,
                accountId,
                walletId,
                signerType,
                password
            )
            refreshDetail(accountId)
        }
    }

    fun removeSigner(walletId: String, signerId: String) {
        withSelectedAccount("Signer removed") { accountId ->
            client.removeSigner(accountId, walletId, signerId)
            refreshDetail(accountId)
        }
    }

    fun transferOwnership(newOwnerUserId: String) {
        withSelectedAccount("Ownership transferred") { accountId ->
            val detail = client.transferOwnership(accountId, newOwnerUserId)
            mutableState.value = mutableState.value.copy(detail = detail)
        }
    }

    fun clearMessages() {
        mutableState.value = mutableState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    private fun withSelectedAccount(
        successMessage: String,
        action: suspend (String) -> Unit
    ) {
        val accountId = mutableState.value.selectedAccountId ?: return
        launchAction(successMessage) {
            action(accountId)
        }
    }

    private fun launchAction(
        successMessage: String? = null,
        action: suspend () -> Unit
    ) {
        val scope = workScope ?: viewModelScope
        scope.launch {
            mutableState.value = mutableState.value.copy(
                isLoading = true,
                errorMessage = null,
                successMessage = null
            )
            try {
                action()
                mutableState.value = mutableState.value.copy(
                    successMessage = successMessage
                )
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(
                    errorMessage = error.message ?: error.toString()
                )
            } finally {
                mutableState.value = mutableState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun reloadAccounts(selectedAccountId: String) {
        val accounts = client.list().items.orEmpty()
        val detail = client.get(selectedAccountId)
        mutableState.value = mutableState.value.copy(
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            detail = detail
        )
    }

    private suspend fun refreshDetail(accountId: String) {
        val detail = client.get(accountId)
        mutableState.value = mutableState.value.copy(detail = detail)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessAccountsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BusinessAccountsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var createName by remember { mutableStateOf("") }
    var externalRef by remember { mutableStateOf("") }
    var filterRefs by remember { mutableStateOf("") }
    var rename by remember { mutableStateOf("") }
    var walletId by remember { mutableStateOf("") }
    var memberUserId by remember { mutableStateOf("") }
    var memberEmail by remember { mutableStateOf("") }
    var memberRole by remember { mutableStateOf(BusinessAccountMemberRole.viewer) }
    var signerAddress by remember { mutableStateOf("") }
    var signerChain by remember { mutableStateOf("") }
    var signerWalletId by remember { mutableStateOf("") }
    var signerUserId by remember { mutableStateOf("") }
    var signerEmail by remember { mutableStateOf("") }
    var signerPassword by remember { mutableStateOf("") }
    var signerType by remember { mutableStateOf(BusinessAccountSignerType.endUser) }
    var newOwnerUserId by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf<Confirmation?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAccounts()
    }

    confirmation?.let { pending ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            title = { Text(pending.title) },
            text = { Text(pending.message) },
            confirmButton = {
                Button(
                    onClick = {
                        pending.onConfirm()
                        confirmation = null
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Accounts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadAccounts() },
                        enabled = !state.isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            state.errorMessage?.let { message ->
                MessageCard(message, isError = true)
            }
            state.successMessage?.let { message ->
                MessageCard(message, isError = false)
            }

            BusinessAccountSection("Create and filter accounts") {
                BusinessAccountField("Name", createName) { createName = it }
                BusinessAccountField("External reference", externalRef) {
                    externalRef = it
                }
                Button(
                    onClick = {
                        viewModel.createAccount(
                            createName.trim(),
                            externalRef.trim().ifEmpty { null }
                        )
                    },
                    enabled = createName.isNotBlank() && !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create account")
                }
                BusinessAccountField(
                    "External references",
                    filterRefs,
                    "Comma-separated references"
                ) {
                    filterRefs = it
                }
                OutlinedButton(
                    onClick = {
                        viewModel.loadAccounts(parseBusinessAccountExternalRefs(filterRefs))
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply filter")
                }
            }

            BusinessAccountSection("Accounts") {
                AccountDropdown(
                    accounts = state.accounts,
                    selectedAccountId = state.selectedAccountId,
                    enabled = !state.isLoading,
                    onSelected = viewModel::selectAccount
                )
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            state.detail?.let { detail ->
                BusinessAccountSection(detail.name ?: "Unnamed account") {
                    Text("ID: ${detail.id}")
                    detail.externalRef?.let { Text("External ref: $it") }
                    BusinessAccountField("New name", rename) { rename = it }
                    OutlinedButton(
                        onClick = { viewModel.renameAccount(rename.trim()) },
                        enabled = rename.isNotBlank() && !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Rename account")
                    }
                }

                BusinessAccountSection("Wallets") {
                    val wallets = detail.wallets.orEmpty()
                    if (wallets.isEmpty()) {
                        Text("No wallets linked")
                    }
                    wallets.forEach { wallet ->
                        ItemRow(
                            title = wallet.chain,
                            subtitle = wallet.id,
                            onDelete = {
                                confirmation = Confirmation(
                                    "Remove wallet",
                                    "Remove wallet ${wallet.id}?"
                                ) {
                                    viewModel.removeWallet(wallet.id)
                                }
                            }
                        )
                    }
                    BusinessAccountField("Wallet ID", walletId) { walletId = it }
                    OutlinedButton(
                        onClick = { viewModel.addWallet(walletId.trim()) },
                        enabled = walletId.isNotBlank() && !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Link wallet")
                    }
                }

                BusinessAccountSection("Members") {
                    if (detail.members.isEmpty()) {
                        Text("No members")
                    }
                    detail.members.forEach { member ->
                        MemberRow(
                            member = member,
                            enabled = !state.isLoading,
                            onRoleSelected = { role ->
                                viewModel.updateMemberRole(member.userId, role)
                            },
                            onDelete = {
                                confirmation = Confirmation(
                                    "Remove member",
                                    "Remove member ${member.userId}?"
                                ) {
                                    viewModel.removeMember(member.userId)
                                }
                            }
                        )
                    }
                    BusinessAccountField("User ID", memberUserId) {
                        memberUserId = it
                    }
                    BusinessAccountField("Email", memberEmail) {
                        memberEmail = it
                    }
                    EnumDropdown(
                        label = "Role",
                        selected = memberRole,
                        values = BusinessAccountMemberRole.entries,
                        onSelected = { memberRole = it }
                    )
                    OutlinedButton(
                        onClick = {
                            viewModel.addMember(
                                memberUserId.trim().ifEmpty { null },
                                memberEmail.trim().ifEmpty { null },
                                memberRole
                            )
                        },
                        enabled = (memberUserId.isNotBlank() || memberEmail.isNotBlank()) &&
                            !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add member")
                    }
                }

                BusinessAccountSection("Signers") {
                    if (detail.signers.isEmpty()) {
                        Text("No signers")
                    }
                    detail.signers.forEach { signer ->
                        ItemRow(
                            title = signer.userId ?: signer.id,
                            subtitle = "${signer.type} · ${signer.walletId}",
                            onDelete = {
                                confirmation = Confirmation(
                                    "Remove signer",
                                    "Remove signer ${signer.id}?"
                                ) {
                                    viewModel.removeSigner(signer.walletId, signer.id)
                                }
                            }
                        )
                    }
                    BusinessAccountField("Account address", signerAddress) {
                        signerAddress = it
                    }
                    BusinessAccountField("Chain name", signerChain) {
                        signerChain = it
                    }
                    BusinessAccountField("Wallet ID", signerWalletId) {
                        signerWalletId = it
                    }
                    BusinessAccountField("Signer user ID", signerUserId) {
                        signerUserId = it
                    }
                    BusinessAccountField("Signer email", signerEmail) {
                        signerEmail = it
                    }
                    BusinessAccountField(
                        label = "Password",
                        value = signerPassword,
                        password = true,
                        onValueChange = { signerPassword = it }
                    )
                    EnumDropdown(
                        label = "Signer type",
                        selected = signerType,
                        values = BusinessAccountSignerType.entries,
                        onSelected = { signerType = it }
                    )
                    val hasSignerIdentity =
                        signerUserId.isNotBlank() || signerEmail.isNotBlank()
                    val hasSignerWallet =
                        signerAddress.isNotBlank() &&
                            signerChain.isNotBlank() &&
                            signerWalletId.isNotBlank()
                    OutlinedButton(
                        onClick = {
                            viewModel.addSigner(
                                signerAddress.trim(),
                                signerChain.trim(),
                                signerWalletId.trim(),
                                signerUserId.trim().ifEmpty { null },
                                signerEmail.trim().ifEmpty { null },
                                signerType,
                                signerPassword.ifBlank { null }
                            )
                        },
                        enabled = hasSignerIdentity && hasSignerWallet && !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add signer")
                    }
                }

                BusinessAccountSection("Transfer ownership") {
                    BusinessAccountField("New owner user ID", newOwnerUserId) {
                        newOwnerUserId = it
                    }
                    Button(
                        onClick = {
                            confirmation = Confirmation(
                                "Transfer ownership",
                                "Transfer this account to ${newOwnerUserId.trim()}?"
                            ) {
                                viewModel.transferOwnership(newOwnerUserId.trim())
                            }
                        },
                        enabled = newOwnerUserId.isNotBlank() && !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Transfer ownership")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BusinessAccountSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun BusinessAccountField(
    label: String,
    value: String,
    placeholder: String = "",
    password: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        visualTransformation = if (password) {
            PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AccountDropdown(
    accounts: List<BusinessAccount>,
    selectedAccountId: String?,
    enabled: Boolean,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAccount = accounts.firstOrNull { it.id == selectedAccountId }
    if (accounts.isEmpty()) {
        Text("No business accounts")
        return
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedAccount?.name ?: selectedAccount?.id ?: "Select account")
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.name ?: account.id) },
                    onClick = {
                        expanded = false
                        onSelected(account.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun <T : Enum<T>> EnumDropdown(
    label: String,
    selected: T,
    values: List<T>,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: ${selected.name}")
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            values.forEach { value ->
                DropdownMenuItem(
                    text = { Text(value.name) },
                    onClick = {
                        expanded = false
                        onSelected(value)
                    }
                )
            }
        }
    }
}

@Composable
private fun ItemRow(
    title: String,
    subtitle: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Remove")
        }
    }
}

@Composable
private fun MemberRow(
    member: BusinessAccountMember,
    enabled: Boolean,
    onRoleSelected: (BusinessAccountMemberRole) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(member.userId, fontWeight = FontWeight.Medium)
            EnumDropdown(
                label = "Role",
                selected = member.role,
                values = BusinessAccountMemberRole.entries,
                onSelected = onRoleSelected
            )
        }
        IconButton(onClick = onDelete, enabled = enabled) {
            Icon(Icons.Default.Delete, contentDescription = "Remove member")
        }
    }
}

@Composable
private fun MessageCard(message: String, isError: Boolean) {
    val color = if (isError) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Text(message, modifier = Modifier.padding(16.dp))
    }
}

data class Confirmation(
    val title: String,
    val message: String,
    val onConfirm: () -> Unit
)

fun parseBusinessAccountExternalRefs(value: String): List<String>? {
    val references = value
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (references.isEmpty()) {
        return null
    }
    return references
}
