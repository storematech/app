package com.quizmaker.android.ui.tools.rsvp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.core.alert.AlertBus
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.data.model.RSVP_EVENT_TEMPLATES
import com.quizmaker.android.data.model.RsvpEvent
import com.quizmaker.android.data.model.RsvpEventTemplate
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.RsvpRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RsvpEventListUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val events: List<RsvpEvent> = emptyList(),
    val registrationCounts: Map<String, Int> = emptyMap(),
    val isEditSheetOpen: Boolean = false,
    val editingEvent: RsvpEvent? = null,
    val isSaving: Boolean = false,
    /** Set only when the create sheet was opened from a template — [editingEvent] stays null since this is still a create, not an edit. */
    val pendingTemplate: RsvpEventTemplate? = null
)

/** Powers Tools → RSVP and Events — the management list, same shape as PollListViewModel/VotingListViewModel. */
@HiltViewModel
class RsvpEventListViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val repository: RsvpRepository,
    private val analyticsLogger: AnalyticsLogger,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(RsvpEventListUiState())
    val uiState: StateFlow<RsvpEventListUiState> = _uiState.asStateFlow()

    init {
        refresh()
        // ExploreTemplatesScreen route-arg handoff — see PollListViewModel's identical init block.
        val templateIndex = savedStateHandle.get<Int>("template") ?: -1
        if (templateIndex >= 0) {
            RSVP_EVENT_TEMPLATES.getOrNull(templateIndex)?.let { openCreateSheetFromTemplate(it) }
        }
    }

    fun refresh() {
        val userId = authRepository.currentUserId() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getEvents(userId)) {
                is AppResult.Success -> {
                    val events = result.data
                    val counts = (repository.getRegistrationCounts(events.map { it.id }) as? AppResult.Success)?.data.orEmpty()
                    _uiState.value = _uiState.value.copy(isLoading = false, events = events, registrationCounts = counts)
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
            }
        }
    }

    fun openCreateSheet() {
        _uiState.value = _uiState.value.copy(isEditSheetOpen = true, editingEvent = null, pendingTemplate = null)
    }

    fun openCreateSheetFromTemplate(template: RsvpEventTemplate) {
        _uiState.value = _uiState.value.copy(isEditSheetOpen = true, editingEvent = null, pendingTemplate = template)
    }

    fun openEditSheet(event: RsvpEvent) {
        _uiState.value = _uiState.value.copy(isEditSheetOpen = true, editingEvent = event, pendingTemplate = null)
    }

    fun dismissEditSheet() {
        _uiState.value = _uiState.value.copy(isEditSheetOpen = false, editingEvent = null, pendingTemplate = null)
    }

    fun saveEvent(
        title: String,
        description: String,
        location: String,
        eventDate: String?,
        capacity: Int?,
        rsvpDeadline: String?,
        allowGuests: Boolean,
        isActive: Boolean
    ) {
        val userId = authRepository.currentUserId() ?: return
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Enter an event title.")
            return
        }
        val editingId = _uiState.value.editingEvent?.id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            val result = if (editingId != null) {
                repository.updateEvent(
                    eventId = editingId,
                    title = title.trim(),
                    description = description.trim().ifBlank { null },
                    location = location.trim().ifBlank { null },
                    eventDate = eventDate,
                    capacity = capacity,
                    rsvpDeadline = rsvpDeadline,
                    allowGuests = allowGuests,
                    isActive = isActive
                )
            } else {
                repository.createEvent(
                    userId = userId,
                    title = title.trim(),
                    description = description.trim().ifBlank { null },
                    location = location.trim().ifBlank { null },
                    eventDate = eventDate,
                    capacity = capacity,
                    rsvpDeadline = rsvpDeadline,
                    allowGuests = allowGuests,
                    isActive = isActive
                )
            }
            when (result) {
                is AppResult.Success -> {
                    if (editingId == null) analyticsLogger.logToolCreated("rsvp")
                    val updatedEvents = if (editingId != null) {
                        _uiState.value.events.map { if (it.id == editingId) result.data else it }
                    } else {
                        listOf(result.data) + _uiState.value.events
                    }
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        isEditSheetOpen = false,
                        editingEvent = null,
                        events = updatedEvents
                    )
                    AlertBus.success(if (editingId != null) "Event updated" else "Event created")
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
            }
        }
    }

    fun toggleActive(event: RsvpEvent) {
        val newActive = !event.isActive
        _uiState.value = _uiState.value.copy(
            events = _uiState.value.events.map { if (it.id == event.id) it.copy(isActive = newActive) else it }
        )
        analyticsLogger.logToolActiveToggled("rsvp", newActive)
        viewModelScope.launch { repository.setActive(event.id, newActive) }
    }

    fun deleteEvent(eventId: String) {
        _uiState.value = _uiState.value.copy(events = _uiState.value.events.filterNot { it.id == eventId })
        analyticsLogger.logToolDeleted("rsvp")
        viewModelScope.launch {
            repository.deleteEvent(eventId)
            AlertBus.success("Event deleted")
        }
    }
}
