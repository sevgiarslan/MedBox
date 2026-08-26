package com.medbox.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medbox.app.data.MedicineRepository
import com.medbox.app.data.MedicineWithTags
import com.medbox.app.data.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MedicineListViewModel(private val repository: MedicineRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = _selectedTagId.asStateFlow()

    val allTags: StateFlow<List<Tag>> = repository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val visibleMedicines: StateFlow<List<MedicineWithTags>> = combine(
        repository.observeMedicines(),
        _searchQuery,
        _selectedTagId
    ) { medicines, query, tagId ->
        medicines
            .filter { tagId == null || it.tags.any { tag -> tag.id == tagId } }
            .filter { query.isBlank() || it.medicine.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTagFilterChange(tagId: Long?) {
        _selectedTagId.value = if (_selectedTagId.value == tagId) null else tagId
    }
}
