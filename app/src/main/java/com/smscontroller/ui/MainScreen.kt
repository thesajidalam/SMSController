package com.smscontroller.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smscontroller.ui.theme.Success
import com.smscontroller.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val serviceEnabled by viewModel.serviceEnabled.collectAsStateWithLifecycle()
    val lockEnabled by viewModel.lockEnabled.collectAsStateWithLifecycle()
    val beepEnabled by viewModel.beepEnabled.collectAsStateWithLifecycle()
    val gpsEnabled by viewModel.gpsEnabled.collectAsStateWithLifecycle()
    val hiddenFromLauncher by viewModel.hiddenFromLauncher.collectAsStateWithLifecycle()
    val authorizedNumbers by viewModel.authorizedNumbers.collectAsStateWithLifecycle()
    val deviceAdminActive by viewModel.deviceAdminActive.collectAsStateWithLifecycle()
    val batteryOptimized by viewModel.batteryOptimized.collectAsStateWithLifecycle()
    val beepDuration by viewModel.beepDuration.collectAsStateWithLifecycle()
    val beepRingtoneUri by viewModel.beepRingtoneUri.collectAsStateWithLifecycle()
    val ownerNumber by viewModel.ownerNumber.collectAsStateWithLifecycle()
    val batteryEnabled by viewModel.batteryEnabled.collectAsStateWithLifecycle()
    val photoEnabled by viewModel.photoEnabled.collectAsStateWithLifecycle()
    val wipeEnabled by viewModel.wipeEnabled.collectAsStateWithLifecycle()
    val flashEnabled by viewModel.flashEnabled.collectAsStateWithLifecycle()
    val callmeEnabled by viewModel.callmeEnabled.collectAsStateWithLifecycle()
    val wifiEnabled by viewModel.wifiEnabled.collectAsStateWithLifecycle()
    val dataEnabled by viewModel.dataEnabled.collectAsStateWithLifecycle()
    val recordEnabled by viewModel.recordEnabled.collectAsStateWithLifecycle()
    val screenshotEnabled by viewModel.screenshotEnabled.collectAsStateWithLifecycle()
    val screenCaptureReady by viewModel.screenCaptureReady.collectAsStateWithLifecycle()
    val commandCount by viewModel.commandCount.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }
    var showNumbersDialog by remember { mutableStateOf(false) }
    var showBeepSettings by remember { mutableStateOf(false) }
    var showOwnerDialog by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var tempNumbers by remember { mutableStateOf(authorizedNumbers.toMutableList()) }
    var tempOwner by remember { mutableStateOf(ownerNumber) }
    var tempBeepDuration by remember { mutableIntStateOf(beepDuration) }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            try {
                val cursor = context.contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val phoneIdx = c.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (phoneIdx >= 0) {
                            val number = c.getString(phoneIdx)
                            if (number != null && tempNumbers.size < 5) {
                                tempNumbers = (tempNumbers + number).toMutableList()
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.setBeepRingtoneUri(uri.toString())
            }
        }
    }

    val screenCaptureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.initScreenCapture(context, result.resultCode, result.data)
        }
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val mpm = context.getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
            screenCaptureLauncher.launch(mpm.createScreenCaptureIntent())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshCommandCount()
        viewModel.checkDeviceAdmin(context)
        viewModel.checkBatteryOptimization(context)
        viewModel.checkScreenCapture(context)

        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.RECEIVE_SMS); perms.add(Manifest.permission.READ_SMS); perms.add(Manifest.permission.SEND_SMS)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION); perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (perms.isNotEmpty()) smsPermissionLauncher.launch(perms.toTypedArray())
    }

    if (showAbout) {
        AboutScreen(onBack = { showAbout = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SMS Controller", style = MaterialTheme.typography.titleLarge)
                        Text("Remote control via SMS", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Authorized numbers") },
                                onClick = { showMenu = false; tempNumbers = authorizedNumbers.toMutableList(); showNumbersDialog = true },
                                leadingIcon = { Icon(Icons.Default.Contacts, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Beep settings") },
                                onClick = { showMenu = false; tempBeepDuration = beepDuration; showBeepSettings = true },
                                leadingIcon = { Icon(Icons.Default.MusicNote, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Set owner number") },
                                onClick = { showMenu = false; tempOwner = ownerNumber; showOwnerDialog = true },
                                leadingIcon = { Icon(Icons.Default.Phone, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Battery exemption") },
                                onClick = { showMenu = false; viewModel.requestBatteryOptimization(context) },
                                leadingIcon = { Icon(Icons.Default.Power, null) }
                            )
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)))
                            DropdownMenuItem(
                                text = { Text("About") },
                                onClick = { showMenu = false; showAbout = true },
                                leadingIcon = { Icon(Icons.Default.Info, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item { StatusBanner(serviceEnabled, deviceAdminActive, batteryOptimized, commandCount) }

            item { ServiceToggleCard(serviceEnabled) { viewModel.toggleService(context) } }

            item { SectionHeader("Commands") }

            item { CommandToggleCard(Icons.Default.Lock, "Lock", "Lock screen immediately via SMS", lockEnabled, { viewModel.toggleLock() }, serviceEnabled, requiresAdmin = true, adminActive = deviceAdminActive, onEnableAdmin = { viewModel.requestDeviceAdmin(context) }) }
            item { CommandToggleCard(Icons.Default.VolumeUp, "Beep", "Loud sound & vibration for ${beepDuration}s (disables silent)", beepEnabled, { viewModel.toggleBeep() }, serviceEnabled) }
            item { CommandToggleCard(Icons.Default.GpsFixed, "GPS", "Get live location replied via SMS", gpsEnabled, { viewModel.toggleGps() }, serviceEnabled) }

            item { Spacer(Modifier.height(4.dp)); SectionHeader("Privacy") }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = if (hiddenFromLauncher) Success else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Hide from Launcher", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(if (hiddenFromLauncher) "App is hidden" else "App is visible", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = hiddenFromLauncher, onCheckedChange = { viewModel.toggleHidden(context) }, colors = SwitchDefaults.colors(checkedTrackColor = Success))
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)); SectionHeader("Advanced Features") }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showAdvanced = !showAdvanced }.animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Tap to ${if (showAdvanced) "hide" else "show"} advanced SMS features", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (showAdvanced) {
                            Spacer(Modifier.height(4.dp))
                            Text("These features require additional permissions. Enable only what you need.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (showAdvanced) {
                item { FeatureToggleCard(Icons.Default.BatteryFull, "BATTERY", "Reply with battery % and charging status", batteryEnabled, { viewModel.toggleBattery() }, serviceEnabled) }
                item { FeatureToggleCard(Icons.Default.CameraAlt, "PHOTO", "Capture photo and save to device (needs camera permission)", photoEnabled, { viewModel.togglePhoto() }, serviceEnabled) }
                item { FeatureToggleCard(Icons.Default.DeleteForever, "WIPE", "Factory reset device (needs device admin)", wipeEnabled, { viewModel.toggleWipe() }, serviceEnabled, requiresAdmin = true, adminActive = deviceAdminActive, onEnableAdmin = { viewModel.requestDeviceAdmin(context) }) }
                item { FeatureToggleCard(Icons.Default.FlashOn, "FLASH", "Toggle flashlight on/off", flashEnabled, { viewModel.toggleFlash() }, serviceEnabled) }
                item { FeatureToggleCard(Icons.Default.Phone, "CALLME", "Call owner's number (needs phone permission)", callmeEnabled, { viewModel.toggleCallme() }, serviceEnabled) }
                item { FeatureToggleCard(Icons.Default.Wifi, "WIFI", "Turn on WiFi", wifiEnabled, { viewModel.toggleWifi() }, serviceEnabled) }
                item { FeatureToggleCard(Icons.Default.DataUsage, "DATA", "Turn on mobile data", dataEnabled, { viewModel.toggleData() }, serviceEnabled) }
                item { FeatureToggleCard(Icons.Default.Radio, "RECORD", "Start/stop audio recording (needs mic permission)", recordEnabled, { viewModel.toggleRecord() }, serviceEnabled) }
                item {
                    FeatureToggleCard(
                        Icons.Default.ScreenShare, "SCREENSHOT", "Capture screen and save locally", screenshotEnabled,
                        { viewModel.toggleScreenshot() }, serviceEnabled,
                        extraAction = if (!screenCaptureReady && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                    val mpm = context.getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
                                    screenCaptureLauncher.launch(mpm.createScreenCaptureIntent())
                                } else {
                                    mediaProjectionLauncher.launch(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION)
                                }
                            }
                        } else null,
                        extraActionLabel = if (!screenCaptureReady) "Grant screen capture" else null
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)); SectionHeader("Commands Reference") }

            item { CommandReferenceCard() }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "SMSController v1.1.0 by @thesajidalam",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showNumbersDialog) {
        MultiNumberDialog(
            numbers = tempNumbers,
            onAdd = {
                if (tempNumbers.size < 5) {
                    tempNumbers = (tempNumbers + "").toMutableList()
                }
            },
            onPickContact = { contactPickerLauncher.launch(null) },
            onNumberChange = { index, value ->
                val list = tempNumbers.toMutableList()
                if (index < list.size) { list[index] = value; tempNumbers = list }
            },
            onRemove = { index ->
                val list = tempNumbers.toMutableList()
                if (index < list.size) { list.removeAt(index); tempNumbers = list }
            },
            onConfirm = { viewModel.setAuthorizedNumbers(tempNumbers.filter { it.isNotBlank() }); showNumbersDialog = false },
            onDismiss = { showNumbersDialog = false }
        )
    }

    if (showBeepSettings) {
        BeepSettingsDialog(
            currentDuration = tempBeepDuration,
            currentRingtoneUri = beepRingtoneUri,
            onDurationChange = { tempBeepDuration = it },
            onPickRingtone = {
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, if (beepRingtoneUri.isNotBlank()) Uri.parse(beepRingtoneUri) else null)
                }
                ringtonePickerLauncher.launch(intent)
            },
            onConfirm = {
                viewModel.setBeepDuration(tempBeepDuration)
                showBeepSettings = false
            },
            onDismiss = { showBeepSettings = false }
        )
    }

    if (showOwnerDialog) {
        OwnerNumberDialog(
            currentNumber = tempOwner,
            onNumberChange = { tempOwner = it },
            onConfirm = { viewModel.setOwnerNumber(tempOwner); showOwnerDialog = false },
            onDismiss = { showOwnerDialog = false }
        )
    }
}

@Composable
private fun StatusBanner(isActive: Boolean, deviceAdminActive: Boolean, batteryOptimized: Boolean, commandCount: Int = 0) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isActive) Success.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isActive) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (isActive) Success else MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (isActive) "Service Running" else "Service Stopped", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (isActive) {
                        Text("$commandCount commands executed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!deviceAdminActive && isActive) Text("Device admin not enabled (Lock/Wipe unavailable)", style = MaterialTheme.typography.bodySmall, color = Warning)
                    if (batteryOptimized && isActive) Text("Battery optimization may kill service", style = MaterialTheme.typography.bodySmall, color = Warning)
                }
            }
        }
    }
}

@Composable
private fun ServiceToggleCard(enabled: Boolean, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(if (enabled) Icons.Default.BluetoothConnected else Icons.Default.PowerOff, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Service", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (enabled) "Listening for SMS commands" else "Tap to start listening", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = enabled, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
        }
    }
}

@Composable
private fun CommandToggleCard(
    icon: ImageVector, title: String, description: String, enabled: Boolean, onToggle: () -> Unit,
    serviceEnabled: Boolean, requiresAdmin: Boolean = false, adminActive: Boolean = false,
    onEnableAdmin: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Switch(checked = enabled, onCheckedChange = { onToggle() }, enabled = serviceEnabled, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
            }
            if (requiresAdmin && enabled && !adminActive) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { onEnableAdmin?.invoke() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Shield, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enable Device Admin")
                }
            }
        }
    }
}

@Composable
private fun FeatureToggleCard(
    icon: ImageVector, title: String, description: String, enabled: Boolean, onToggle: () -> Unit,
    serviceEnabled: Boolean, requiresAdmin: Boolean = false, adminActive: Boolean = false,
    onEnableAdmin: (() -> Unit)? = null, extraAction: (() -> Unit)? = null,
    extraActionLabel: String? = null
) {
    Card(modifier = Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f), modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Switch(checked = enabled, onCheckedChange = { onToggle() }, enabled = serviceEnabled, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
            }
            if (requiresAdmin && enabled && !adminActive) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onEnableAdmin?.invoke() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Shield, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Enable Device Admin", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (extraAction != null && extraActionLabel != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = extraAction, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Key, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(extraActionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
}

@Composable
private fun CommandReferenceCard() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Send SMS with keyword to control:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            CommandRow("lock", "Lock screen instantly")
            CommandRow("beep", "Loud alarm + vibration")
            CommandRow("gps", "Live location reply")
            CommandRow("battery", "Battery % & charging status")
            CommandRow("photo", "Capture photo")
            CommandRow("wipe", "Factory reset")
            CommandRow("flash", "Toggle flashlight")
            CommandRow("callme", "Call owner number")
            CommandRow("wifi", "Turn on WiFi")
            CommandRow("data", "Turn on mobile data")
            CommandRow("record", "Start audio recording")
            CommandRow("record stop", "Stop recording")
            CommandRow("screenshot", "Capture screen")
        }
    }
}

@Composable
private fun CommandRow(command: String, description: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text(command, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(10.dp))
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MultiNumberDialog(
    numbers: List<String>, onAdd: () -> Unit, onPickContact: () -> Unit,
    onNumberChange: (Int, String) -> Unit, onRemove: (Int) -> Unit,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Authorized Numbers (max 5)") },
        text = {
            Column {
                Text("Only SMS from these numbers will be processed. Leave empty to accept all.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                numbers.forEachIndexed { index, number ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        OutlinedTextField(
                            value = number,
                            onValueChange = { onNumberChange(index, it) },
                            label = { Text("Number ${index + 1}") },
                            placeholder = { Text("+1234567890") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (numbers.size < 5) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onAdd, shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add number")
                        }
                        OutlinedButton(onClick = onPickContact, shape = RoundedCornerShape(10.dp)) {
                            Icon(Icons.Default.Contacts, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("From contacts")
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun BeepSettingsDialog(
    currentDuration: Int, currentRingtoneUri: String,
    onDurationChange: (Int) -> Unit, onPickRingtone: () -> Unit,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Beep Settings") },
        text = {
            Column {
                Text("Duration: ${currentDuration}s", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = currentDuration.toFloat(),
                    onValueChange = { onDurationChange(it.toInt()) },
                    valueRange = 10f..300f,
                    steps = 28
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (currentRingtoneUri.isNotBlank()) "Custom sound selected" else "Default alarm sound", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onPickRingtone, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.MusicNote, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Choose custom sound")
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun OwnerNumberDialog(
    currentNumber: String, onNumberChange: (String) -> Unit,
    onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Owner Number") },
        text = {
            Column {
                Text("Used by CALLME command. Set the number to call.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = currentNumber,
                    onValueChange = onNumberChange,
                    label = { Text("Phone number") },
                    placeholder = { Text("+1234567890") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
