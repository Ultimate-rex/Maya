package com.maya.assistant.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.maya.assistant.core.*

private enum class ToolTab { MENU, SCAN, GENERATE_QR, PASSWORD, FILES, PHOTOS, TRIGGERS }

@Composable
fun ToolsScreen(onClose: () -> Unit) {
    var tab by remember { mutableStateOf(ToolTab.MENU) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tools") },
                navigationIcon = {
                    TextButton(onClick = { if (tab == ToolTab.MENU) onClose() else tab = ToolTab.MENU }) {
                        Text("Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (tab) {
                ToolTab.MENU -> ToolsMenu(onSelect = { tab = it })
                ToolTab.SCAN -> BarcodeScannerScreen(onResult = { })
                ToolTab.GENERATE_QR -> GenerateQrTool()
                ToolTab.PASSWORD -> PasswordTool()
                ToolTab.FILES -> FileManagerTool()
                ToolTab.PHOTOS -> PhotoCleanupTool()
                ToolTab.TRIGGERS -> TriggersTool()
            }
        }
    }
}

@Composable
private fun ToolsMenu(onSelect: (ToolTab) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = { onSelect(ToolTab.SCAN) }, modifier = Modifier.fillMaxWidth()) { Text("Scan a barcode / QR code") }
        Button(onClick = { onSelect(ToolTab.GENERATE_QR) }, modifier = Modifier.fillMaxWidth()) { Text("Generate a QR code") }
        Button(onClick = { onSelect(ToolTab.PASSWORD) }, modifier = Modifier.fillMaxWidth()) { Text("Generate a password") }
        Button(onClick = { onSelect(ToolTab.FILES) }, modifier = Modifier.fillMaxWidth()) { Text("Manage a folder's files") }
        Button(onClick = { onSelect(ToolTab.PHOTOS) }, modifier = Modifier.fillMaxWidth()) { Text("Clean up recent photos") }
        Button(onClick = { onSelect(ToolTab.TRIGGERS) }, modifier = Modifier.fillMaxWidth()) { Text("Automation triggers") }
    }
}

@Composable
private fun GenerateQrTool() {
    var text by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Text or URL") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { if (text.isNotBlank()) bitmap = QrCodeGenerator.generate(text) }) { Text("Generate") }
        bitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = "QR code") }
    }
}

@Composable
private fun PasswordTool() {
    var length by remember { mutableStateOf(16f) }
    var symbols by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Length: ${length.toInt()}")
        Slider(value = length, onValueChange = { length = it }, valueRange = 8f..32f)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = symbols, onCheckedChange = { symbols = it })
            Text("Include symbols")
        }
        Button(onClick = { password = PasswordGenerator.generate(length.toInt(), symbols) }) { Text("Generate") }
        if (password.isNotEmpty()) Text(password, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun FileManagerTool() {
    val context = LocalContext.current
    var treeUri by remember { mutableStateOf<Uri?>(null) }
    var files by remember { mutableStateOf<List<androidx.documentfile.provider.DocumentFile>>(emptyList()) }
    val manager = remember { ScopedFileManager(context) }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            manager.persistAccess(uri)
            treeUri = uri
            files = manager.listFiles(uri)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { pickFolder.launch(null) }) { Text("Choose a folder") }
        Text("Maya can only see files inside the folder you pick.", style = MaterialTheme.typography.bodySmall)
        LazyColumn {
            items(files) { file ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(file.name ?: "unnamed")
                    TextButton(onClick = {
                        manager.deleteFile(file)
                        treeUri?.let { files = manager.listFiles(it) }
                    }) { Text("Delete") }
                }
            }
        }
    }
}

@Composable
private fun PhotoCleanupTool() {
    val context = LocalContext.current
    val manager = remember { PhotoManager(context) }
    var photos by remember { mutableStateOf(manager.recentPhotos()) }
    var selected by remember { mutableStateOf(setOf<Long>()) }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        photos = manager.recentPhotos()
        selected = emptySet()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Recent photos - tap to select, then delete. Android will show its own confirmation.", style = MaterialTheme.typography.bodySmall)
        LazyColumn {
            items(photos) { photo ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(photo.displayName)
                    Checkbox(
                        checked = selected.contains(photo.id),
                        onCheckedChange = { checked ->
                            selected = if (checked) selected + photo.id else selected - photo.id
                        },
                    )
                }
            }
        }
        Button(
            onClick = {
                val toDelete = photos.filter { selected.contains(it.id) }
                manager.requestDelete(toDelete)?.let { sender ->
                    deleteLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(sender).build())
                }
            },
            enabled = selected.isNotEmpty(),
        ) { Text("Delete selected") }
    }
}

@Composable
private fun TriggersTool() {
    val context = LocalContext.current
    val store = remember { TriggerRuleStore(LocalJsonStore(context)) }
    var rules by remember { mutableStateOf(store.all()) }
    var threshold by remember { mutableStateOf("20") }
    var line by remember { mutableStateOf("Boss, battery is running low.") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Existing rules", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(rules) { rule ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${rule.event}${rule.threshold?.let { " ($it%)" } ?: ""} -> ${rule.actionParam}")
                    TextButton(onClick = { store.remove(rule.id); rules = store.all() }) { Text("Remove") }
                }
            }
        }
        HorizontalDivider()
        Text("New rule: if battery drops below this %, speak a line")
        OutlinedTextField(value = threshold, onValueChange = { threshold = it }, label = { Text("Battery %") })
        OutlinedTextField(value = line, onValueChange = { line = it }, label = { Text("Line to speak") })
        Button(onClick = {
            store.add(
                TriggerRule(
                    id = System.currentTimeMillis().toString(),
                    event = TriggerEvent.BATTERY_BELOW,
                    threshold = threshold.toIntOrNull() ?: 20,
                    actionType = TriggerActionType.SPEAK_LINE,
                    actionParam = line,
                )
            )
            rules = store.all()
        }) { Text("Add rule") }
    }
}
