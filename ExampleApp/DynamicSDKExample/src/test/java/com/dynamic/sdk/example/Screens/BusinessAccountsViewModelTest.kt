package com.dynamic.sdk.example.Screens

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
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BusinessAccountsViewModelTest {
    @Test
    fun `loads and selects the first business account`() {
        val client = FakeBusinessAccountsClient()
        val viewModel = BusinessAccountsViewModel(
            client = client,
            workScope = CoroutineScope(Dispatchers.Unconfined)
        )

        viewModel.loadAccounts()

        assertEquals("business-1", viewModel.state.value.selectedAccountId)
        assertEquals("Existing business", viewModel.state.value.detail?.name)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `creates and selects a business account`() {
        val client = FakeBusinessAccountsClient()
        val viewModel = BusinessAccountsViewModel(
            client = client,
            workScope = CoroutineScope(Dispatchers.Unconfined)
        )

        viewModel.loadAccounts()
        viewModel.createAccount("New business", "external-2")

        assertEquals("New business", client.createdName)
        assertEquals("business-2", viewModel.state.value.selectedAccountId)
        assertEquals("New business", viewModel.state.value.detail?.name)
    }

    @Test
    fun `parses external references`() {
        assertEquals(
            listOf("first", "second", "third"),
            parseBusinessAccountExternalRefs(" first, second ,, third ")
        )
        assertNull(parseBusinessAccountExternalRefs("  "))
    }
}

private class FakeBusinessAccountsClient : BusinessAccountsClient {
    private val accounts = mutableListOf(
        BusinessAccount(
            id = "business-1",
            name = "Existing business",
            projectEnvironmentId = "environment-1"
        )
    )

    var createdName: String? = null

    override suspend fun list(externalRefs: List<String>?): BusinessAccountList {
        return BusinessAccountList(items = accounts)
    }

    override suspend fun get(businessAccountId: String): BusinessAccountDetail {
        val account = accounts.first { it.id == businessAccountId }
        return BusinessAccountDetail(
            id = account.id,
            name = account.name,
            projectEnvironmentId = account.projectEnvironmentId
        )
    }

    override suspend fun create(
        name: String?,
        externalRef: String?,
        metadata: Map<String, JsonElement>?
    ): BusinessAccount {
        createdName = name
        val account = BusinessAccount(
            id = "business-${accounts.size + 1}",
            name = name,
            externalRef = externalRef,
            projectEnvironmentId = "environment-1"
        )
        accounts.add(account)
        return account
    }

    override suspend fun update(
        businessAccountId: String,
        name: String
    ): BusinessAccount {
        error("Not used")
    }

    override suspend fun addWallet(
        businessAccountId: String?,
        walletId: String
    ): BusinessAccountDetail {
        error("Not used")
    }

    override suspend fun removeWallet(
        businessAccountId: String,
        walletId: String
    ): BusinessAccountDetail {
        error("Not used")
    }

    override suspend fun addMember(
        businessAccountId: String,
        userId: String?,
        identifier: String?,
        identifierType: BusinessAccountSignerIdentifierType?,
        role: BusinessAccountMemberRole?
    ): BusinessAccountMember {
        error("Not used")
    }

    override suspend fun removeMember(
        businessAccountId: String,
        userId: String
    ): BusinessAccountMember {
        error("Not used")
    }

    override suspend fun updateMemberRole(
        businessAccountId: String,
        userId: String,
        role: BusinessAccountMemberRole
    ): BusinessAccountMember {
        error("Not used")
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
        error("Not used")
    }

    override suspend fun removeSigner(
        businessAccountId: String,
        walletId: String,
        signerId: String
    ): BusinessAccountSigner {
        error("Not used")
    }

    override suspend fun transferOwnership(
        businessAccountId: String,
        newOwnerUserId: String
    ): BusinessAccountDetail {
        error("Not used")
    }
}
