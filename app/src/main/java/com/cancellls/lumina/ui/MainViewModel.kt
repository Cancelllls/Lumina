package com.cancellls.lumina.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cancellls.lumina.AppContainer
import com.cancellls.lumina.data.AppSettings
import com.cancellls.lumina.domain.GlassPalette
import com.cancellls.lumina.domain.NaturalDateParser
import com.cancellls.lumina.domain.ParsedReminderInput
import com.cancellls.lumina.domain.Priority
import com.cancellls.lumina.domain.RecurrenceEngine
import com.cancellls.lumina.domain.ReminderItem
import com.cancellls.lumina.domain.ReminderListItem
import com.cancellls.lumina.domain.SmartCounts
import com.cancellls.lumina.domain.SmartListFilter
import com.cancellls.lumina.domain.SmartListType
import com.cancellls.lumina.domain.SortOrder
import com.cancellls.lumina.domain.SubtaskItem
import com.cancellls.lumina.domain.ThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LuminaScreen {
    data object Home : LuminaScreen
    data class SmartList(val type: SmartListType) : LuminaScreen
    data class CustomList(val listId: Long) : LuminaScreen
}

sealed interface ActiveModal {
    data class ReminderEditor(
        val existing: ReminderItem? = null,
        val prefillTitle: String = "",
        val prefillDueMillis: Long? = null,
        val prefillHasTime: Boolean = true,
        val prefillRecurrence: String = RecurrenceEngine.NONE,
        val prefillPriority: Priority = Priority.NONE,
        val prefillFlagged: Boolean = false,
        val defaultListId: Long? = null
    ) : ActiveModal

    data class ListEditor(val existing: ReminderListItem? = null) : ActiveModal
    data object Settings : ActiveModal
}

sealed interface UiEvent {
    data class UndoComplete(
        val message: String,
        val previousReminder: ReminderItem,
        val spawnedNextId: Long?
    ) : UiEvent

    data class UndoDelete(
        val message: String,
        val deletedReminder: ReminderItem
    ) : UiEvent

    data class Message(val text: String) : UiEvent
}

data class MainUiState(
    val currentScreen: LuminaScreen = LuminaScreen.Home,
    val lists: List<ReminderListItem> = emptyList(),
    val allReminders: List<ReminderItem> = emptyList(),
    val smartCounts: SmartCounts = SmartCounts(),
    val upNextReminders: List<ReminderItem> = emptyList(),
    val listActiveCounts: Map<Long, Int> = emptyMap(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val quickAddText: String = "",
    val quickAddParsed: ParsedReminderInput = ParsedReminderInput(cleanTitle = ""),
    val settings: AppSettings = AppSettings(),
    val activeModal: ActiveModal? = null
)

class MainViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val repository = container.repository
    private val preferences = container.preferences

    private data class NavDraftState(
        val screen: LuminaScreen = LuminaScreen.Home,
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
        val quickAddText: String = "",
        val activeModal: ActiveModal? = null
    )

    private val navDraft = MutableStateFlow(NavDraftState())

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    val uiState: StateFlow<MainUiState> = combine(
        repository.observeLists(),
        repository.observeReminders(),
        preferences.settings,
        navDraft
    ) { lists, reminders, settings, nav ->
        val now = System.currentTimeMillis()
        val smartCounts = SmartListFilter.computeCounts(reminders, now)
        val upNext = SmartListFilter.filterSmartList(
            reminders = reminders,
            type = SmartListType.TODAY,
            sortOrder = SortOrder.DUE_DATE,
            nowMillis = now
        ).take(3)

        val listActiveCounts = reminders
            .filter { !it.isCompleted && it.listId != null }
            .groupingBy { it.listId!! }
            .eachCount()

        val parsed = if (nav.quickAddText.isBlank()) {
            ParsedReminderInput(cleanTitle = "")
        } else {
            NaturalDateParser.parse(nav.quickAddText)
        }

        // If user deleted the currently open custom list, return cleanly to Home
        val safeScreen = when (val s = nav.screen) {
            is LuminaScreen.CustomList -> if (lists.any { it.id == s.listId }) s else LuminaScreen.Home
            else -> s
        }

        MainUiState(
            currentScreen = safeScreen,
            lists = lists,
            allReminders = reminders,
            smartCounts = smartCounts,
            upNextReminders = upNext,
            listActiveCounts = listActiveCounts,
            searchQuery = nav.searchQuery,
            isSearchActive = nav.isSearchActive,
            quickAddText = nav.quickAddText,
            quickAddParsed = parsed,
            settings = settings,
            activeModal = nav.activeModal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = MainUiState()
    )

    fun navigateToHome() {
        navDraft.update { it.copy(screen = LuminaScreen.Home) }
    }

    fun openSmartList(type: SmartListType) {
        navDraft.update { it.copy(screen = LuminaScreen.SmartList(type), isSearchActive = false, searchQuery = "") }
    }

    fun openCustomList(listId: Long) {
        navDraft.update { it.copy(screen = LuminaScreen.CustomList(listId), isSearchActive = false, searchQuery = "") }
    }

    fun toggleSearch(active: Boolean) {
        navDraft.update {
            it.copy(
                isSearchActive = active,
                searchQuery = if (active) it.searchQuery else ""
            )
        }
    }

    fun updateSearchQuery(query: String) {
        navDraft.update { it.copy(searchQuery = query) }
    }

    fun updateQuickAddText(text: String) {
        navDraft.update { it.copy(quickAddText = text) }
    }

    fun submitQuickAdd() {
        val raw = navDraft.value.quickAddText.trim()
        if (raw.isEmpty()) return

        val parsed = NaturalDateParser.parse(raw)
        val screen = navDraft.value.screen
        val defaultListId = when (screen) {
            is LuminaScreen.CustomList -> screen.listId
            else -> uiState.value.lists.firstOrNull()?.id
        }
        val flagged = parsed.isFlagged || (screen == LuminaScreen.SmartList(SmartListType.FLAGGED))
        val dueAtMillis = parsed.dueAtMillis ?: if (screen == LuminaScreen.SmartList(SmartListType.TODAY)) {
            NaturalDateParser.parse("today 9am").dueAtMillis
        } else {
            null
        }
        val hasTime = if (parsed.dueAtMillis != null) parsed.hasTime else false

        viewModelScope.launch {
            repository.createReminder(
                title = parsed.cleanTitle,
                listId = defaultListId,
                dueAtMillis = dueAtMillis,
                hasTime = hasTime,
                priority = parsed.priority,
                isFlagged = flagged,
                recurrenceRule = parsed.recurrenceRule
            )
            navDraft.update { it.copy(quickAddText = "") }
        }
    }

    fun openDetailedCreatorFromQuickAdd() {
        val raw = navDraft.value.quickAddText.trim()
        val parsed = if (raw.isEmpty()) ParsedReminderInput("") else NaturalDateParser.parse(raw)
        val screen = navDraft.value.screen
        val defaultListId = when (screen) {
            is LuminaScreen.CustomList -> screen.listId
            else -> uiState.value.lists.firstOrNull()?.id
        }
        navDraft.update {
            it.copy(
                quickAddText = "",
                activeModal = ActiveModal.ReminderEditor(
                    existing = null,
                    prefillTitle = parsed.cleanTitle,
                    prefillDueMillis = parsed.dueAtMillis,
                    prefillHasTime = if (parsed.dueAtMillis != null) parsed.hasTime else true,
                    prefillRecurrence = parsed.recurrenceRule,
                    prefillPriority = parsed.priority,
                    prefillFlagged = parsed.isFlagged || (screen == LuminaScreen.SmartList(SmartListType.FLAGGED)),
                    defaultListId = defaultListId
                )
            )
        }
    }

    fun openReminderEditor(existing: ReminderItem) {
        navDraft.update { it.copy(activeModal = ActiveModal.ReminderEditor(existing = existing)) }
    }

    fun openReminderById(reminderId: Long) {
        if (reminderId <= 0L) return
        viewModelScope.launch {
            val found = repository.getReminderById(reminderId)
            if (found != null) {
                navDraft.update { it.copy(activeModal = ActiveModal.ReminderEditor(existing = found)) }
            }
        }
    }

    fun openListEditor(existing: ReminderListItem? = null) {
        navDraft.update { it.copy(activeModal = ActiveModal.ListEditor(existing = existing)) }
    }

    fun openSettings() {
        navDraft.update { it.copy(activeModal = ActiveModal.Settings) }
    }

    fun dismissModal() {
        navDraft.update { it.copy(activeModal = null) }
    }

    fun saveReminder(
        existing: ReminderItem?,
        title: String,
        notes: String,
        listId: Long?,
        dueAtMillis: Long?,
        hasTime: Boolean,
        priority: Priority,
        isFlagged: Boolean,
        recurrenceRule: String,
        subtasks: List<SubtaskItem>
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        viewModelScope.launch {
            if (existing == null) {
                repository.createReminder(
                    title = cleanTitle,
                    notes = notes,
                    listId = listId,
                    dueAtMillis = dueAtMillis,
                    hasTime = hasTime,
                    priority = priority,
                    isFlagged = isFlagged,
                    recurrenceRule = recurrenceRule,
                    subtasks = subtasks
                )
            } else {
                val clearedSnooze = if (existing.dueAtMillis != dueAtMillis) null else existing.snoozedUntilMillis
                repository.updateReminder(
                    existing.copy(
                        title = cleanTitle,
                        notes = notes,
                        listId = listId,
                        dueAtMillis = dueAtMillis,
                        hasTime = hasTime,
                        snoozedUntilMillis = clearedSnooze,
                        priority = priority,
                        isFlagged = isFlagged,
                        recurrenceRule = recurrenceRule,
                        subtasks = subtasks
                    )
                )
            }
            dismissModal()
        }
    }

    fun toggleComplete(reminderId: Long) {
        viewModelScope.launch {
            val result = repository.toggleComplete(reminderId) ?: return@launch
            val label = if (result.nowCompleted) {
                if (result.spawnedNextId != null) {
                    "Completed • Next occurrence scheduled"
                } else {
                    "Completed \"${result.previousReminder.title}\""
                }
            } else {
                "Marked incomplete"
            }
            _events.emit(
                UiEvent.UndoComplete(
                    message = label,
                    previousReminder = result.previousReminder,
                    spawnedNextId = result.spawnedNextId
                )
            )
        }
    }

    fun undoToggleComplete(event: UiEvent.UndoComplete) {
        viewModelScope.launch {
            repository.undoToggleComplete(event.previousReminder, event.spawnedNextId)
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            val deleted = repository.deleteReminder(reminderId) ?: return@launch
            dismissModal()
            _events.emit(
                UiEvent.UndoDelete(
                    message = "Deleted \"${deleted.title}\"",
                    deletedReminder = deleted
                )
            )
        }
    }

    fun undoDelete(item: ReminderItem) {
        viewModelScope.launch {
            repository.restoreReminder(item)
        }
    }

    fun snoozeReminder(reminderId: Long, minutes: Int) {
        viewModelScope.launch {
            repository.snoozeReminder(reminderId, minutes)
            dismissModal()
            _events.emit(UiEvent.Message("Snoozed for $minutes minutes"))
        }
    }

    fun toggleFlag(reminderId: Long) {
        viewModelScope.launch {
            repository.toggleFlag(reminderId)
        }
    }

    fun toggleSubtask(subtaskId: Long) {
        viewModelScope.launch {
            repository.toggleSubtask(subtaskId)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch {
            repository.clearCompleted()
            _events.emit(UiEvent.Message("Cleared completed reminders"))
        }
    }

    fun saveList(existing: ReminderListItem?, name: String, iconKey: String, colorHex: Long) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            if (existing == null) {
                repository.createList(cleanName, iconKey, colorHex)
            } else {
                repository.updateList(
                    existing.copy(name = cleanName, iconKey = iconKey, colorHex = colorHex)
                )
            }
            dismissModal()
        }
    }

    fun deleteList(listId: Long) {
        viewModelScope.launch {
            repository.deleteList(listId)
            dismissModal()
        }
    }

    fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)
    fun setPalette(palette: GlassPalette) = preferences.setPalette(palette)
    fun setDefaultSnoozeMinutes(minutes: Int) = preferences.setDefaultSnoozeMinutes(minutes)
    fun setSortOrder(order: SortOrder) = preferences.setSortOrder(order)
    fun setShowCompletedInLists(show: Boolean) = preferences.setShowCompletedInLists(show)
    fun dismissPermissionBanner() = preferences.dismissPermissionBanner()

    suspend fun generateBackupJson(): String = repository.exportBackupJson()

    fun restoreBackupJson(json: String) {
        viewModelScope.launch {
            runCatching {
                repository.importBackupJson(json)
            }.onSuccess { (listsCount, remindersCount) ->
                dismissModal()
                _events.emit(
                    UiEvent.Message("Restored $listsCount lists and $remindersCount reminders")
                )
            }.onFailure { err ->
                _events.emit(
                    UiEvent.Message("Import failed: ${err.message ?: "Invalid backup"}")
                )
            }
        }
    }

    fun showMessage(text: String) {
        viewModelScope.launch {
            _events.emit(UiEvent.Message(text))
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(container) as T
        }
    }
}
