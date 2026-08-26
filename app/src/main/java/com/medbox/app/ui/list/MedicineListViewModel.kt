package com.medbox.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medbox.app.data.ExpiryStatus
import com.medbox.app.data.MedicineRepository
import com.medbox.app.data.MedicineWithTags
import com.medbox.app.data.Tag
import com.medbox.app.data.expiryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ExpiryFilter { ALL, EXPIRED, EXPIRING_SOON }

data class DashboardStats(
    val total: Int = 0,
    val expired: Int = 0,
    val expiringSoon: Int = 0
)

class MedicineListViewModel(private val repository: MedicineRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = _selectedTagId.asStateFlow()

    private val _expiryFilter = MutableStateFlow(ExpiryFilter.ALL)
    val expiryFilter: StateFlow<ExpiryFilter> = _expiryFilter.asStateFlow()

    val allTags: StateFlow<List<Tag>> = repository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allMedicines: StateFlow<List<MedicineWithTags>> = repository.observeMedicines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats> = allMedicines.map { medicines ->
        var expired = 0
        var expiringSoon = 0
        medicines.forEach {
            when (it.medicine.expiryStatus()) {
                ExpiryStatus.EXPIRED -> expired++
                ExpiryStatus.EXPIRING_SOON -> expiringSoon++
                ExpiryStatus.OK -> Unit
            }
        }
        DashboardStats(total = medicines.size, expired = expired, expiringSoon = expiringSoon)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    val visibleMedicines: StateFlow<List<MedicineWithTags>> = combine(
        allMedicines,
        _searchQuery,
        _selectedTagId,
        _expiryFilter
    ) { medicines, query, tagId, expiryFilter ->
        medicines
            .filter { tagId == null || it.tags.any { tag -> tag.id == tagId } }
            .filter { query.isBlank() || it.medicine.name.contains(query, ignoreCase = true) }
            .filter {
                when (expiryFilter) {
                    ExpiryFilter.ALL -> true
                    ExpiryFilter.EXPIRED -> it.medicine.expiryStatus() == ExpiryStatus.EXPIRED
                    ExpiryFilter.EXPIRING_SOON -> it.medicine.expiryStatus() == ExpiryStatus.EXPIRING_SOON
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTagFilterChange(tagId: Long?) {
        _selectedTagId.value = if (_selectedTagId.value == tagId) null else tagId
    }

    fun onExpiryFilterChange(filter: ExpiryFilter) {
        _expiryFilter.value = if (_expiryFilter.value == filter) ExpiryFilter.ALL else filter
    }

    fun deleteMedicine(item: MedicineWithTags) {
        viewModelScope.launch {
            repository.deleteMedicine(item.medicine)
        }
    }
}
