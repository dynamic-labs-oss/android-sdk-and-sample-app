package com.dynamic.sdk.example.Screens.WalletConnect

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamic.sdk.android.DynamicSDK
import com.dynamic.sdk.android.Models.WcSessionProposal
import com.dynamic.sdk.android.Models.WcSessionRequest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Global listener that handles WalletConnect lifecycle:
 * - Auto-initializes WalletConnect when the user logs in
 * - Shows session proposal dialogs (approve/reject)
 * - Shows session request dialogs (approve/reject signing/transactions)
 * - Shows snackbar when a session is disconnected
 *
 * Place this high in the widget tree, wrapping the main app content.
 */
@Composable
fun WcGlobalListener(content: @Composable () -> Unit) {
    val sdk = DynamicSDK.getInstance()
    val wc = sdk.walletConnect
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var initialized by remember { mutableStateOf(false) }
    var initializing by remember { mutableStateOf(false) }

    // Dialog state
    var showProposal by remember { mutableStateOf<WcSessionProposal?>(null) }
    var showRequest by remember { mutableStateOf<WcSessionRequest?>(null) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen for auth token AND webview initialized state
    val token by sdk.auth.tokenChanges.collectAsState()
    val wcInitialized by wc.initializedChanges.collectAsState()

    // Wait for BOTH: user is logged in AND webview has extended the GlobalWalletExtension
    LaunchedEffect(token, wcInitialized) {
        if (!token.isNullOrEmpty() && wcInitialized && !initialized && !initializing) {
            initializing = true
            try {
                wc.initialize()
                initialized = true
            } catch (e: Exception) {
                android.util.Log.e("WcGlobalListener", "Failed to initialize WC: ${e.message}")
            }
            initializing = false
        } else if (token.isNullOrEmpty()) {
            initialized = false
            initializing = false
        }
    }

    // Listen for WC events using direct collect on SharedFlow
    LaunchedEffect(Unit) {
        launch {
            wc.onSessionProposal.collect { proposal ->
                showProposal = proposal
            }
        }
        launch {
            wc.onSessionRequest.collect { request ->
                showRequest = request
            }
        }
        launch {
            wc.onSessionDelete.collect { topic ->
                snackbarHostState.showSnackbar(
                    "Session disconnected: ${topic?.take(8)}..."
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Session Proposal Dialog
    showProposal?.let { proposal ->
        ProposalDialog(
            proposal = proposal,
            onApprove = {
                showProposal = null
                scope.launch {
                    try {
                        wc.confirmPairing(true)
                        snackbarHostState.showSnackbar("Session approved")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Approval failed: ${e.message}")
                    }
                }
            },
            onReject = {
                showProposal = null
                scope.launch {
                    try {
                        wc.confirmPairing(false)
                    } catch (_: Exception) {}
                }
            }
        )
    }

    // Session Request Dialog
    showRequest?.let { request ->
        RequestDialog(
            request = request,
            onApprove = {
                showRequest = null
                scope.launch {
                    try {
                        wc.respondSessionRequest(request.id, request.topic, true)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Request failed: ${e.message}")
                    }
                }
            },
            onReject = {
                showRequest = null
                scope.launch {
                    try {
                        wc.respondSessionRequest(request.id, request.topic, false)
                    } catch (_: Exception) {}
                }
            }
        )
    }
}

@Composable
private fun ProposalDialog(
    proposal: WcSessionProposal,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* non-dismissible */ },
        title = { Text("Session Proposal") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = proposal.proposer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (proposal.proposer.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = proposal.proposer.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (proposal.proposer.url.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = proposal.proposer.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                proposal.requiredNamespaces?.let { namespaces ->
                    Text(
                        text = "Required chains:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    namespaces.forEach { (key, ns) ->
                        Text(
                            text = "  $key: ${ns.chains.joinToString(", ")}",
                            fontSize = 13.sp
                        )
                    }
                }

                proposal.optionalNamespaces?.takeIf { it.isNotEmpty() }?.let { namespaces ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Optional chains:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    namespaces.forEach { (key, ns) ->
                        Text(
                            text = "  $key: ${ns.chains.joinToString(", ")}",
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onApprove) {
                Text("Approve")
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text("Reject")
            }
        }
    )
}

@Composable
private fun RequestDialog(
    request: WcSessionRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = { /* non-dismissible */ },
        title = { Text("Session Request") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                InfoRow(label = "Method", value = request.method)
                InfoRow(label = "Chain", value = request.chainId)
                InfoRow(label = "Topic", value = "${request.topic.take(12)}...")

                request.params?.let { params ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Params:",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                val json = prettyPrintJson(params)
                                clipboardManager.setText(AnnotatedString(json))
                                Toast.makeText(context, "Params copied", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy params",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = prettyPrintJson(params),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onApprove) {
                Text("Approve")
            }
        },
        dismissButton = {
            TextButton(onClick = onReject) {
                Text("Reject")
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp
        )
    }
}

private fun prettyPrintJson(element: JsonElement): String {
    return try {
        val json = kotlinx.serialization.json.Json {
            prettyPrint = true
        }
        json.encodeToString(JsonElement.serializer(), element)
    } catch (_: Exception) {
        element.toString()
    }
}
