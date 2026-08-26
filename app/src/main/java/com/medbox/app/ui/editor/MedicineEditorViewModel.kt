package com.medbox.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medbox.app.data.Medicine
import com.medbox.app.data.MedicineRepository
import com.medbox.app.data.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MedicineEditorState(
    val medicineId: Long? = null,
    val name: String = "",
    val barcode: String = "",
    // Null until the user explicitly picks a date, or an existing medicine is loaded.
    // A barcode only carries the product's identity, never its expiration date, so we must
    // never guess one - a silent default here previously got mistaken for a real value.
    val expirationDate: LocalDate? = null,
    val quantity: Int = 1,
    val notes: String = "",
    val selectedTagIds: Set<Long> = emptySet(),
    val isLoading: Boolean = true,
    val duplicateOfExistingId: Long? = null,
    val showDateRequiredError: Boolean = false,
    val barcodeCapturedHint: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false
)

class MedicineEditorViewModel(
    private val repository: MedicineRepository,
    medicineId: Long?
) : ViewModel() {

    private val _state = MutableStateFlow(MedicineEditorState(medicineId = medicineId))
    val state: StateFlow<MedicineEditorState> = _state.asStateFlow()

    val allTags: StateFlow<List<Tag>> = repository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (medicineId != null) {
            viewModelScope.launch {
                repository.observeMedicine(medicineId).collect { withTags ->
                    if (withTags != null) {
                        _state.value = _state.value.copy(
                            name = withTags.medicine.name,
                            barcode = withTags.medicine.barcode.orEmpty(),
                            expirationDate = withTags.medicine.expirationDate,
                            quantity = withTags.medicine.quantity,
                            notes = withTags.medicine.notes,
                            selectedTagIds = withTags.tags.map { it.id }.toSet(),
                            isLoading = false
                        )
                    }
                }
            }
        } else {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun onNameChange(value: String) {
        _state.value = _state.value.copy(name = value)
    }

    fun onBarcodeChange(value: String) {
        _state.value = _state.value.copy(barcode = value, duplicateOfExistingId = null)
    }

    fun onBarcodeScanned(value: String) {
        _state.value = _state.value.copy(barcode = value, barcodeCapturedHint = true)
        viewModelScope.launch {
            val existing = repository.findByBarcode(value)
            if (existing != null && existing.medicine.id != _state.value.medicineId) {
                _state.value = _state.value.copy(duplicateOfExistingId = existing.medicine.id)
            }
        }
    }

    fun dismissBarcodeCapturedHint() {
        _state.value = _state.value.copy(barcodeCapturedHint = false)
    }

    fun dismissDuplicateWarning() {
        _state.value = _state.value.copy(duplicateOfExistingId = null)
    }

    fun onExpirationDateChange(date: LocalDate) {
        _state.value = _state.value.copy(expirationDate = date, showDateRequiredError = false)
    }

    fun onQuantityChange(quantity: Int) {
        _state.value = _state.value.copy(quantity = quantity.coerceAtLeast(0))
    }

    fun onNotesChange(value: String) {
        _state.value = _state.value.copy(notes = value)
    }

    fun onToggleTag(tagId: Long) {
        val current = _state.value.selectedTagIds
        _state.value = _state.value.copy(
            selectedTagIds = if (tagId in current) current - tagId else current + tagId
        )
    }

    fun addCustomTag(name: String, color: Long) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createTag(name.trim(), color)
            onToggleTag(id)
        }
    }

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) return
        val expirationDate = s.expirationDate
        if (expirationDate == null) {
            _state.value = s.copy(showDateRequiredError = true)
            return
        }
        viewModelScope.launch {
            val medicine = Medicine(
                id = s.medicineId ?: 0,
                name = s.name.trim(),
                barcode = s.barcode.trim().ifBlank { null },
                expirationDate = expirationDate,
                quantity = s.quantity,
                notes = s.notes.trim()
            )
            repository.saveMedicine(medicine, s.selectedTagIds.toList())
            _state.value = _state.value.copy(saved = true)
        }
    }

    fun delete() {
        val id = _state.value.medicineId ?: return
        viewModelScope.launch {
            repository.deleteMedicine(
                Medicine(id = id, name = _state.value.name, expirationDate = LocalDate.now())
            )
            _state.value = _state.value.copy(deleted = true)
        }
    }
}
