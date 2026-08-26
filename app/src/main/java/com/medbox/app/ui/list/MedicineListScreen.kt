package com.medbox.app.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medbox.app.data.MedicineWithTags
import com.medbox.app.ui.components.ExpiryBadge
import com.medbox.app.ui.components.TagFilterChip
import com.medbox.app.util.shareInventoryExport
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineListScreen(
    viewModel: MedicineListViewModel,
    onAddMedicine: () -> Unit,
    onOpenMedicine: (Long) -> Unit
) {
    val medicines by viewModel.visibleMedicines.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val selectedTagId by viewModel.selectedTagId.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val expiryFilter by viewModel.expiryFilter.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (json == null) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Dosya okunamadı.") }
            return@rememberLauncherForActivityResult
        }
        viewModel.importFromJson(json) { result ->
            coroutineScope.launch {
                result.fold(
                    onSuccess = { count -> snackbarHostState.showSnackbar("$count ilaç içe aktarıldı.") },
                    onFailure = { snackbarHostState.showSnackbar("Geçersiz dosya formatı, içe aktarılamadı.") }
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("İlaç Dolabım") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Diğer İşlemler")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Dışa Aktar (Paylaş)") },
                            onClick = {
                                showMenu = false
                                viewModel.exportAsJson { json -> shareInventoryExport(context, json) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("İçe Aktar") },
                            onClick = {
                                showMenu = false
                                importLauncher.launch(arrayOf("*/*"))
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMedicine,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("İlaç Ekle") }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            DashboardRow(
                stats = stats,
                expiryFilter = expiryFilter,
                onFilterClick = viewModel::onExpiryFilterChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            TextField(
                value = query,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("İlaç ara...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            if (tags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tags, key = { it.id }) { tag ->
                        TagFilterChip(
                            tag = tag,
                            selected = tag.id == selectedTagId,
                            onClick = { viewModel.onTagFilterChange(tag.id) }
                        )
                    }
                }
            }

            if (medicines.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (stats.total == 0) {
                            "Henüz ilaç eklenmedi. Sağ alttaki + butonuyla ilk ilacını ekleyebilirsin."
                        } else {
                            "Bu filtreye uyan ilaç yok."
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(medicines, key = { it.medicine.id }) { item ->
                        MedicineRow(
                            item = item,
                            onClick = { onOpenMedicine(item.medicine.id) },
                            onDelete = { viewModel.deleteMedicine(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardRow(
    stats: DashboardStats,
    expiryFilter: ExpiryFilter,
    onFilterClick: (ExpiryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DashboardCard(
            label = "Toplam",
            count = stats.total,
            color = MaterialTheme.colorScheme.primary,
            selected = expiryFilter == ExpiryFilter.ALL,
            onClick = { onFilterClick(ExpiryFilter.ALL) },
            modifier = Modifier.weight(1f)
        )
        DashboardCard(
            label = "Süresi Doldu",
            count = stats.expired,
            color = Color(0xFFB71C1C),
            selected = expiryFilter == ExpiryFilter.EXPIRED,
            onClick = { onFilterClick(ExpiryFilter.EXPIRED) },
            modifier = Modifier.weight(1f)
        )
        DashboardCard(
            label = "Yakında Doluyor",
            count = stats.expiringSoon,
            color = Color(0xFFE65100),
            selected = expiryFilter == ExpiryFilter.EXPIRING_SOON,
            onClick = { onFilterClick(ExpiryFilter.EXPIRING_SOON) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DashboardCard(
    label: String,
    count: Int,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("$count", style = MaterialTheme.typography.headlineSmall, color = color)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MedicineRow(item: MedicineWithTags, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    item.medicine.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text("Adet: ${item.medicine.quantity}", style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "İlacı Sil")
                }
            }

            ExpiryBadge(medicine = item.medicine)

            if (item.tags.isNotEmpty()) {
                Text(
                    item.tags.joinToString(" · ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("İlacı Sil") },
            text = { Text("\"${item.medicine.name}\" envanterden kaldırılsın mı? Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("İptal") }
            }
        )
    }
}
