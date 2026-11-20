package com.cancellls.lumina

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cancellls.lumina.alarm.NotificationHelper
import com.cancellls.lumina.domain.SmartListType
import com.cancellls.lumina.ui.ActiveModal
import com.cancellls.lumina.ui.HomeScreen
import com.cancellls.lumina.ui.ListEditorDialog
import com.cancellls.lumina.ui.LuminaScreen
import com.cancellls.lumina.ui.MainViewModel
import com.cancellls.lumina.ui.QuickAddDock
import com.cancellls.lumina.ui.ReminderEditorSheet
import com.cancellls.lumina.ui.ReminderListScreen
import com.cancellls.lumina.ui.SettingsSheet
import com.cancellls.lumina.ui.UiEvent
import com.cancellls.lumina.ui.glass.AmbientMeshBackdrop
import com.cancellls.lumina.ui.theme.LuminaTheme
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as LuminaApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleLaunchIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LuminaTheme(
                themeMode = uiState.settings.themeMode,
                glassMode = uiState.settings.glassMode,
                palette = uiState.settings.palette
            ) {
                LuminaRootHost(
                    viewModel = viewModel,
                    onRequestExactAlarmSettings = ::openExactAlarmSettings,
                    onRequestNotificationSettings = ::openAppNotificationSettings
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_QUICK_ADD, false)) {
            viewModel.setQuickAddExpanded(true)
            intent.removeExtra(NotificationHelper.EXTRA_OPEN_QUICK_ADD)
        }
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
        if (reminderId > 0L) {
            viewModel.openReminderById(reminderId)
            intent.removeExtra(NotificationHelper.EXTRA_REMINDER_ID)
        }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } else {
            openAppNotificationSettings()
        }
    }

    private fun openAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }
}

@Composable
private fun LuminaRootHost(
    viewModel: MainViewModel,
    onRequestExactAlarmSettings: () -> Unit,
    onRequestNotificationSettings: () -> Unit
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as LuminaApplication).container
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasNotifications = remember(resumeTick) {
        NotificationHelper.hasNotificationPermission(context)
    }
    val canExactAlarms = remember(resumeTick) {
        appContainer.alarmScheduler.canScheduleExactAlarms()
    }
    val showPermissionBanner = !uiState.settings.dismissedPermissionBanner &&
        (!hasNotifications || !canExactAlarms)

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        resumeTick++
        if (!granted) {
            onRequestNotificationSettings()
        } else if (!appContainer.alarmScheduler.canScheduleExactAlarms()) {
            onRequestExactAlarmSettings()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            runCatching {
                val json = viewModel.generateBackupJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open output stream")
                }
            }.onSuccess {
                viewModel.showMessage("Backup exported successfully")
            }.onFailure { err ->
                viewModel.showMessage("Export failed: ${err.message ?: "Unknown error"}")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error("Unable to read selected file")
                }
            }.onSuccess { json ->
                viewModel.restoreBackupJson(json)
            }.onFailure { err ->
                viewModel.showMessage("Import failed: ${err.message ?: "Unknown error"}")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            snackbarHostState.currentSnackbarData?.dismiss()
            when (event) {
                is UiEvent.UndoComplete -> {
                    val res = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (res == SnackbarResult.ActionPerformed) {
                        viewModel.undoToggleComplete(event)
                    }
                }
                is UiEvent.UndoDelete -> {
                    val res = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (res == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete(event.deletedReminder)
                    }
                }
                is UiEvent.Message -> {
                    snackbarHostState.showSnackbar(
                        message = event.text,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    BackHandler(enabled = uiState.isQuickAddExpanded || uiState.currentScreen != LuminaScreen.Home) {
        if (uiState.isQuickAddExpanded) {
            viewModel.setQuickAddExpanded(false)
        } else {
            viewModel.navigateToHome()
        }
    }

    val meshHazeState = rememberHazeState()
    val chromeHazeState = rememberHazeState()

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientMeshBackdrop(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = meshHazeState)
                .hazeSource(state = chromeHazeState)
        )

        AnimatedContent(
            targetState = uiState.currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "lumina_screen_transition"
        ) { screen ->
                when (screen) {
                    LuminaScreen.Home -> {
                        HomeScreen(
                            uiState = uiState,
                            meshHazeState = meshHazeState,
                            chromeHazeState = chromeHazeState,
                            showPermissionBanner = showPermissionBanner,
                            onGrantPermissionsClick = {
                                if (!hasNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else if (!canExactAlarms) {
                                    onRequestExactAlarmSettings()
                                }
                            },
                            onDismissPermissionBanner = viewModel::dismissPermissionBanner,
                            onToggleSearch = viewModel::toggleSearch,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onOpenSettings = viewModel::openSettings,
                            onOpenSmartList = viewModel::openSmartList,
                            onOpenCustomList = viewModel::openCustomList,
                            onCreateList = { viewModel.openListEditor(null) },
                            onEditList = { viewModel.openListEditor(it) },
                            onDeleteList = viewModel::deleteList,
                            onToggleComplete = viewModel::toggleComplete,
                            onToggleFlag = viewModel::toggleFlag,
                            onToggleSubtask = viewModel::toggleSubtask,
                            onDeleteReminder = viewModel::deleteReminder,
                            onOpenReminder = viewModel::openReminderEditor
                        )
                    }
                    is LuminaScreen.SmartList, is LuminaScreen.CustomList -> {
                        ReminderListScreen(
                            screen = screen,
                            uiState = uiState,
                            meshHazeState = meshHazeState,
                            chromeHazeState = chromeHazeState,
                            onBack = viewModel::navigateToHome,
                            onSortOrderChange = viewModel::setSortOrder,
                            onToggleShowCompleted = viewModel::setShowCompletedInLists,
                            onClearCompleted = viewModel::clearCompleted,
                            onToggleComplete = viewModel::toggleComplete,
                            onToggleFlag = viewModel::toggleFlag,
                            onToggleSubtask = viewModel::toggleSubtask,
                            onDeleteReminder = viewModel::deleteReminder,
                            onOpenReminder = viewModel::openReminderEditor
                        )
                    }
                }
            }

        // Snackbar positioned cleanly above the Dual-Island Liquid Glass Dock
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 88.dp, start = 16.dp, end = 16.dp)
        )

        // BitChord iOS 26 Dual-Island Liquid Glass Dock
        QuickAddDock(
            currentScreen = uiState.currentScreen,
            isExpanded = uiState.isQuickAddExpanded,
            text = uiState.quickAddText,
            parsed = uiState.quickAddParsed,
            chromeHazeState = chromeHazeState,
            onSelectScreen = { target ->
                when (target) {
                    LuminaScreen.Home -> viewModel.navigateToHome()
                    is LuminaScreen.SmartList -> viewModel.openSmartList(target.type)
                    is LuminaScreen.CustomList -> viewModel.openCustomList(target.listId)
                }
            },
            onExpandedChange = viewModel::setQuickAddExpanded,
            onTextChange = viewModel::updateQuickAddText,
            onSubmit = viewModel::submitQuickAdd,
            onOpenFullEditor = viewModel::openDetailedCreatorFromQuickAdd,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    when (val modal = uiState.activeModal) {
        is ActiveModal.ReminderEditor -> {
            ReminderEditorSheet(
                modal = modal,
                lists = uiState.lists,
                onDismiss = viewModel::dismissModal,
                onSave = viewModel::saveReminder,
                onDelete = viewModel::deleteReminder,
                onSnooze = viewModel::snoozeReminder
            )
        }
        is ActiveModal.ListEditor -> {
            ListEditorDialog(
                existing = modal.existing,
                onDismiss = viewModel::dismissModal,
                onSave = { name, iconKey, colorHex ->
                    viewModel.saveList(modal.existing, name, iconKey, colorHex)
                },
                onDelete = modal.existing?.let { { id -> viewModel.deleteList(id) } }
            )
        }
        ActiveModal.Settings -> {
            SettingsSheet(
                settings = uiState.settings,
                hasNotificationPermission = hasNotifications,
                canScheduleExactAlarms = canExactAlarms,
                onDismiss = viewModel::dismissModal,
                onThemeModeChange = viewModel::setThemeMode,
                onGlassModeChange = viewModel::setGlassMode,
                onPaletteChange = viewModel::setPalette,
                onDefaultSnoozeChange = viewModel::setDefaultSnoozeMinutes,
                onOpenNotificationSettings = {
                    if (!hasNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onRequestNotificationSettings()
                    }
                },
                onOpenExactAlarmSettings = onRequestExactAlarmSettings,
                onExportJson = {
                    val fileName = "lumina-backup-${LocalDate.now()}.json"
                    exportLauncher.launch(fileName)
                },
                onImportJson = {
                    importLauncher.launch(arrayOf("application/json", "text/plain"))
                }
            )
        }
        null -> Unit
    }
}
