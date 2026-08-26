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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medbox.app.data.MedicineWithTags
import com.medbox.app.ui.components.ExpiryBadge
import com.medbox.app.ui.components.TagFilterChip

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

    Scaffold(
        topBar = { TopAppBar(title = { Text("İlaç Dolabım") }) },
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
            TextField(
                value = query,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("İlaç ara...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            if (tags.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
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
                        "Henüz ilaç eklenmedi. Sağ alttaki + butonuyla ilk ilacını ekleyebilirsin.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(medicines, key = { it.medicine.id }) { item ->
                        MedicineRow(item = item, onClick = { onOpenMedicine(item.medicine.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicineRow(item: MedicineWithTags, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
}
