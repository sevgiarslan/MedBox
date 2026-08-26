package com.medbox.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.medbox.app.data.Tag
import com.medbox.app.ui.components.TagFilterChip
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineEditorScreen(
    viewModel: MedicineEditorViewModel,
    onScanBarcode: () -> Unit,
    onFinished: () -> Unit,
    onOpenExisting: (Long) -> Unit,
    pendingScannedBarcode: String?,
    onConsumeScannedBarcode: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    val isNew = state.medicineId == null

    LaunchedEffect(pendingScannedBarcode) {
        if (pendingScannedBarcode != null) {
            viewModel.onBarcodeScanned(pendingScannedBarcode)
            onConsumeScannedBarcode()
        }
    }

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onFinished()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "İlaç Ekle" else "İlacı Düzenle") },
                navigationIcon = {
                    IconButton(onClick = onFinished) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = viewModel::delete) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::save) {
                Icon(Icons.Default.Add, contentDescription = "Kaydet")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("İlaç Adı") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.barcode,
                    onValueChange = viewModel::onBarcodeChange,
                    label = { Text("Barkod (opsiyonel)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                FloatingActionButton(onClick = onScanBarcode) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Barkod Tara")
                }
            }

            if (state.duplicateOfExistingId != null) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissDuplicateWarning,
                    title = { Text("Bu ilaç zaten kayıtlı") },
                    text = { Text("Bu barkoda sahip bir ilaç zaten envanterde var. Onu açmak ister misin, yoksa yeni bir kayıt olarak devam etmek mi istersin?") },
                    confirmButton = {
                        TextButton(onClick = {
                            val id = state.duplicateOfExistingId
                            viewModel.dismissDuplicateWarning()
                            if (id != null) onOpenExisting(id)
                        }) { Text("Mevcut Kaydı Aç") }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::dismissDuplicateWarning) { Text("Yeni Kayıt Olarak Devam Et") }
                    }
                )
            }

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Son Kullanma Tarihi: ${state.expirationDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}")
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = state.expirationDate
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                                viewModel.onExpirationDateChange(date)
                            }
                            showDatePicker = false
                        }) { Text("Tamam") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("İptal") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Adet:", style = MaterialTheme.typography.bodyLarge)
                IconButton(onClick = { viewModel.onQuantityChange(state.quantity - 1) }) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text("${state.quantity}", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { viewModel.onQuantityChange(state.quantity + 1) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Etiketler", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tags, key = { it.id }) { tag: Tag ->
                        TagFilterChip(
                            tag = tag,
                            selected = tag.id in state.selectedTagIds,
                            onClick = { viewModel.onToggleTag(tag.id) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { showAddTagDialog = true },
                            label = { Text("+ Yeni Etiket") }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text("Not (opsiyonel)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }
    }

    if (showAddTagDialog) {
        AddTagDialog(
            onDismiss = { showAddTagDialog = false },
            onConfirm = { name, color ->
                viewModel.addCustomTag(name, color)
                showAddTagDialog = false
            }
        )
    }
}

@Composable
private fun AddTagDialog(onDismiss: () -> Unit, onConfirm: (String, Long) -> Unit) {
    var text by remember { mutableStateOf("") }
    val palette = listOf(0xFF1E88E5L, 0xFFE53935L, 0xFF43A047L, 0xFFFB8C00L, 0xFF8E24AAL, 0xFF00897BL)
    var selectedColor by remember { mutableStateOf(palette.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Etiket") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Etiket Adı") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    palette.forEach { color ->
                        val isSelected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 36.dp else 28.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text, selectedColor) }, enabled = text.isNotBlank()) {
                Text("Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
