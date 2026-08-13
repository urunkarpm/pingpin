package com.urunkarpm.pingpin.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.urunkarpm.pingpin.service.ScannedWifiNetwork
import com.urunkarpm.pingpin.service.WifiService
import com.urunkarpm.pingpin.ui.theme.EmeraldGreen
import com.urunkarpm.pingpin.ui.theme.ElectricBlue
import kotlinx.coroutines.launch

@Composable
fun WifiSsidPickerField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Office Wi-Fi SSID"
) {
    val context = LocalContext.current
    val wifiService = remember { WifiService(context) }
    var showDialog by remember { mutableStateOf(false) }
    var currentLiveSsid by remember { mutableStateOf<String?>(null) }

    val fieldShape = RoundedCornerShape(14.dp)

    LaunchedEffect(value) {
        currentLiveSsid = wifiService.getWifiSSID()
    }

    val isLiveConnected = !currentLiveSsid.isNullOrEmpty() &&
            currentLiveSsid.equals(value.trim(), ignoreCase = true)

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                shape = fieldShape,
                leadingIcon = {
                    Icon(
                        imageVector = if (isLiveConnected) Icons.Default.Wifi else Icons.Default.WifiFind,
                        contentDescription = null,
                        tint = if (isLiveConnected) EmeraldGreen else MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(onClick = { showDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = "Scan Wi-Fi Networks",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Open Picker",
                            modifier = Modifier.clickable { showDialog = true }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDialog = true },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )
        }

        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isLiveConnected) EmeraldGreen else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isLiveConnected) "Currently Connected to this Network" else "Configured SSID (Not connected)",
                    fontSize = 11.sp,
                    color = if (isLiveConnected) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    if (showDialog) {
        WifiSsidPickerDialog(
            initialSelectedSsid = value,
            wifiService = wifiService,
            onSsidSelected = { selected ->
                onValueChange(selected)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiSsidPickerDialog(
    initialSelectedSsid: String,
    wifiService: WifiService,
    onSsidSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var scannedNetworks by remember { mutableStateOf<List<ScannedWifiNetwork>>(emptyList()) }
    var savedSsids by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentLiveSsid by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var customSsidInput by remember { mutableStateOf("") }

    fun refreshData() {
        isScanning = true
        scope.launch {
            currentLiveSsid = wifiService.getWifiSSID()
            savedSsids = wifiService.getKnownSSIDs(includeCurrentLive = false)
            scannedNetworks = wifiService.getScannedNetworks()
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Wi-Fi SSID Picker",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Select or scan your office network",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { refreshData() }) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Rescan",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                // Search Filter Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter SSIDs or enter custom name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Currently Connected Wi-Fi Card
                if (!currentLiveSsid.isNullOrEmpty() && searchQuery.isEmpty()) {
                    val activeNetwork = currentLiveSsid!!
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSsidSelected(activeNetwork) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = activeNetwork,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldGreen.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = EmeraldGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Currently connected live Wi-Fi network",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Button(
                                onClick = { onSsidSelected(activeNetwork) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Select", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (currentLiveSsid.isNullOrEmpty() && searchQuery.isEmpty()) {
                    val hasLocPerm = remember(wifiService) { wifiService.hasLocationPermission() }
                    val isLocEnabled = remember(wifiService) { wifiService.isLocationServicesEnabled() }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when {
                                    !hasLocPerm -> "Android requires Location permission to detect Wi-Fi network name (SSID)."
                                    !isLocEnabled -> "Android requires Location (GPS) turned ON in phone Quick Settings to reveal Wi-Fi SSID."
                                    else -> "Connect to your office Wi-Fi or select from scanned/saved networks below."
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Filtered & Grouped Content List
                val filteredScanned = scannedNetworks.filter {
                    it.ssid.contains(searchQuery, ignoreCase = true)
                }

                val filteredSaved = savedSsids.filter {
                    it.contains(searchQuery, ignoreCase = true) &&
                            scannedNetworks.none { scanned -> scanned.ssid.equals(it, ignoreCase = true) }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Nearby Scanned Networks
                    if (filteredScanned.isNotEmpty()) {
                        item {
                            Text(
                                text = "NEARBY SCANNED NETWORKS (${filteredScanned.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(filteredScanned, key = { "scan_${it.ssid}" }) { net ->
                            WifiNetworkRow(
                                network = net,
                                isSelected = initialSelectedSsid.equals(net.ssid, ignoreCase = true),
                                onSelect = { onSsidSelected(net.ssid) }
                            )
                        }
                    }

                    // 2. Saved / History Networks
                    if (filteredSaved.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "SAVED HISTORY NETWORKS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(filteredSaved, key = { "saved_$it" }) { savedSsid ->
                            SavedSsidRow(
                                ssid = savedSsid,
                                isSelected = initialSelectedSsid.equals(savedSsid, ignoreCase = true),
                                onSelect = { onSsidSelected(savedSsid) },
                                onDelete = {
                                    scope.launch {
                                        wifiService.removeKnownSSID(savedSsid)
                                        savedSsids = savedSsids.filter { it != savedSsid }
                                    }
                                }
                            )
                        }
                    }

                    if (filteredScanned.isEmpty() && filteredSaved.isEmpty() && !isScanning) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.WifiOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "No matching networks found for '$searchQuery'" else "No Wi-Fi networks detected nearby",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Custom SSID Manual Entry
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customSsidInput,
                        onValueChange = { customSsidInput = it },
                        placeholder = { Text("Enter hidden SSID manually...") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val trimmed = customSsidInput.trim()
                            if (trimmed.isNotEmpty()) {
                                scope.launch {
                                    wifiService.addKnownSSID(trimmed)
                                    onSsidSelected(trimmed)
                                }
                            } else {
                                Toast.makeText(context, "Please enter an SSID", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        enabled = customSsidInput.trim().isNotEmpty()
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiNetworkRow(
    network: ScannedWifiNetwork,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val signalIcon = when {
        network.rssi >= -60 -> Icons.Default.Wifi
        network.rssi >= -75 -> Icons.Default.Wifi1Bar
        else -> Icons.Default.WifiLock
    }

    val signalColor = when {
        network.rssi >= -60 -> EmeraldGreen
        network.rssi >= -75 -> ElectricBlue
        else -> Color.Gray
    }

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = signalIcon,
                    contentDescription = null,
                    tint = signalColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = network.ssid,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = network.securityLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        if (network.is5GHz) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = ElectricBlue.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "5 GHz",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricBlue,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        if (network.isConnected) {
                            Text(
                                text = "• Connected",
                                fontSize = 10.sp,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SavedSsidRow(
    ssid: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = ssid,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Saved SSID",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
