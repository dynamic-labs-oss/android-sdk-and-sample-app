package com.dynamic.sdk.example.Screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
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
import com.dynamic.sdk.example.ui.theme.ErrorRed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustedDevicesScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: TrustedDevicesViewModel = viewModel()
    val devices by viewModel.devices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val alertTitle by viewModel.alertTitle.collectAsState()
    val alertMessage by viewModel.alertMessage.collectAsState()
    val showConfirmDialog by viewModel.showConfirmDialog.collectAsState()
    val confirmTitle by viewModel.confirmDialogTitle.collectAsState()
    val confirmMessage by viewModel.confirmDialogMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDevices()
    }

    // Alert Dialog
    if (alertTitle != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAlert() },
            title = { Text(alertTitle!!) },
            text = { Text(alertMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAlert() }) {
                    Text("OK")
                }
            }
        )
    }

    // Confirm Dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConfirmDialog() },
            title = { Text(confirmTitle) },
            text = { Text(confirmMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmAction() }) {
                    Text("Remove", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissConfirmDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trusted Devices",
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
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    ErrorMessageView(
                        message = errorMessage!!,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    SimpleButton(
                        icon = Icons.Default.Refresh,
                        title = "Retry",
                        onClick = { viewModel.loadDevices() }
                    )
                }
                devices.isNullOrEmpty() -> {
                    CardContainer {
                        Column {
                            Text(
                                text = "No trusted devices registered",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Trusted devices will appear here once registered",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        devices!!.forEach { device ->
                            TrustedDeviceCard(
                                device = device,
                                onRemove = { viewModel.confirmRemoveDevice(device) }
                            )
                        }
                    }

                    // Remove All button (only show when more than 1 device)
                    if ((devices?.size ?: 0) > 1) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.confirmRemoveAll() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ErrorRed,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove All",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove All Devices", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TrustedDeviceCard(
    device: JsonObject,
    onRemove: () -> Unit
) {
    val displayText = device["displayText"]?.jsonPrimitive?.content ?: "Unknown Device"
    val deviceId = device["id"]?.jsonPrimitive?.content ?: "N/A"
    val deviceType = device["type"]?.jsonPrimitive?.content
    val createdAt = device["createdAt"]?.jsonPrimitive?.content
    val isCurrentDevice = device["isCurrentDevice"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isCurrentDevice) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "This Device",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (deviceType != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val typeLabel = when (deviceType) {
                    "mobile" -> "Mobile"
                    "desktop" -> "Desktop"
                    else -> deviceType
                }
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ID: ${deviceId.take(8)}...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (createdAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Registered: $createdAt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ErrorRed,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Remove", fontWeight = FontWeight.Medium)
            }
        }
    }
}

class TrustedDevicesViewModel : ViewModel() {
    private val sdk = DynamicSDK.getInstance()

    private val _devices = MutableStateFlow<List<JsonObject>?>(null)
    val devices: StateFlow<List<JsonObject>?> = _devices.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _alertTitle = MutableStateFlow<String?>(null)
    val alertTitle: StateFlow<String?> = _alertTitle.asStateFlow()

    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()

    private val _showConfirmDialog = MutableStateFlow(false)
    val showConfirmDialog: StateFlow<Boolean> = _showConfirmDialog.asStateFlow()

    private val _confirmDialogTitle = MutableStateFlow("")
    val confirmDialogTitle: StateFlow<String> = _confirmDialogTitle.asStateFlow()

    private val _confirmDialogMessage = MutableStateFlow("")
    val confirmDialogMessage: StateFlow<String> = _confirmDialogMessage.asStateFlow()

    private var pendingRemoveDevice: JsonObject? = null
    private var pendingRemoveAll: Boolean = false

    fun loadDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _devices.value = sdk.deviceRegistration.getRegisteredDevices()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load trusted devices: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun confirmRemoveDevice(device: JsonObject) {
        pendingRemoveDevice = device
        pendingRemoveAll = false
        val displayText = device["displayText"]?.jsonPrimitive?.content ?: "Unknown Device"
        _confirmDialogTitle.value = "Remove Device"
        _confirmDialogMessage.value = "Are you sure you want to remove \"$displayText\"?"
        _showConfirmDialog.value = true
    }

    fun confirmRemoveAll() {
        pendingRemoveDevice = null
        pendingRemoveAll = true
        _confirmDialogTitle.value = "Remove All Devices"
        _confirmDialogMessage.value = "This will remove all trusted devices. You will need to re-verify on each device."
        _showConfirmDialog.value = true
    }

    fun confirmAction() {
        _showConfirmDialog.value = false
        viewModelScope.launch {
            if (pendingRemoveAll) {
                removeAllDevices()
            } else {
                pendingRemoveDevice?.let { removeDevice(it) }
            }
            pendingRemoveDevice = null
            pendingRemoveAll = false
        }
    }

    private suspend fun removeDevice(device: JsonObject) {
        val deviceId = device["id"]?.jsonPrimitive?.content ?: return
        _isLoading.value = true
        try {
            sdk.deviceRegistration.revokeRegisteredDevice(deviceRegistrationId = deviceId)
            loadDevices()
            _alertTitle.value = "Success"
            _alertMessage.value = "Device removed successfully"
        } catch (e: Exception) {
            _alertTitle.value = "Error"
            _alertMessage.value = "Failed to remove device: ${e.message}"
        }
        _isLoading.value = false
    }

    private suspend fun removeAllDevices() {
        _isLoading.value = true
        try {
            sdk.deviceRegistration.revokeAllRegisteredDevices()
            loadDevices()
            _alertTitle.value = "Success"
            _alertMessage.value = "All devices removed successfully"
        } catch (e: Exception) {
            _alertTitle.value = "Error"
            _alertMessage.value = "Failed to remove all devices: ${e.message}"
        }
        _isLoading.value = false
    }

    fun dismissAlert() {
        _alertTitle.value = null
        _alertMessage.value = null
    }

    fun dismissConfirmDialog() {
        _showConfirmDialog.value = false
        pendingRemoveDevice = null
        pendingRemoveAll = false
    }
}
