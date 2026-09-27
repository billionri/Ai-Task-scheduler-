package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import com.example.data.TaskEntity
import com.example.receiver.TaskReminderReceiver
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AnalyticsViewModel
import com.example.viewmodel.FocusViewModel
import com.example.viewmodel.TaskViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.PaymentDialog
import com.example.data.SubscriptionPlan
import com.example.data.SubscriptionState

enum class AppNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    TASKS("Tasks", Icons.Filled.Checklist, Icons.Outlined.Checklist, "tab_tasks"),
    FOCUS("Focus", Icons.Filled.Timer, Icons.Outlined.Timer, "tab_focus"),
    CALENDAR("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_calendar"),
    ANALYTICS("Analytics", Icons.Filled.Insights, Icons.Outlined.Insights, "tab_analytics")
}

class MainActivity : ComponentActivity() {

    private val taskViewModel: TaskViewModel by viewModels {
        val app = application as TaskSchedulerApp
        TaskViewModel.Factory(app.repository, app.subscriptionManager)
    }

    private val focusViewModel: FocusViewModel by viewModels {
        val app = application as TaskSchedulerApp
        FocusViewModel.Factory(app.repository, app.alarmScheduler)
    }

    private val analyticsViewModel: AnalyticsViewModel by viewModels {
        val app = application as TaskSchedulerApp
        AnalyticsViewModel.Factory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntentExtras(intent)

        setContent {
            MyApplicationTheme {
                val app = application as TaskSchedulerApp
                val subscriptionState by app.subscriptionManager.subscriptionState.collectAsStateWithLifecycle()

                MainContent(
                    taskViewModel = taskViewModel,
                    focusViewModel = focusViewModel,
                    analyticsViewModel = analyticsViewModel,
                    subscriptionState = subscriptionState,
                    onActivatePlan = { plan, txnRef ->
                        app.subscriptionManager.activatePlan(plan, txnRef)
                    },
                    initialTab = if (intent.getBooleanExtra("OPEN_FOCUS_TAB", false)) AppNavTab.FOCUS else AppNavTab.TASKS
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentExtras(intent)
    }

    private fun handleIntentExtras(intent: Intent) {
        if (intent.getBooleanExtra("OPEN_FOCUS_TAB", false)) {
            val taskId = intent.getLongExtra(TaskReminderReceiver.EXTRA_TASK_ID, -1L)
            if (taskId != -1L) {
                val app = application as TaskSchedulerApp
                CoroutineScope(Dispatchers.IO).launch {
                    val db = com.example.data.AppDatabase.getDatabase(this@MainActivity)
                    val task = db.taskDao().getTaskByIdDirect(taskId)
                    task?.let { focusViewModel.selectTask(it) }
                }
            }
        }
    }
}

@Composable
fun MainContent(
    taskViewModel: TaskViewModel,
    focusViewModel: FocusViewModel,
    analyticsViewModel: AnalyticsViewModel,
    subscriptionState: SubscriptionState,
    onActivatePlan: (SubscriptionPlan, String) -> Unit,
    initialTab: AppNavTab = AppNavTab.TASKS
) {
    var currentTab by remember { mutableStateOf(initialTab) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var isMilestoneTrigger by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Automatically display subscription prompt when 2 demo tasks are completed
    LaunchedEffect(taskViewModel) {
        taskViewModel.triggerSubscriptionPrompt.collect {
            if (!subscriptionState.isPro) {
                isMilestoneTrigger = true
                showPaymentDialog = true
            }
        }
    }

    // Request notification permission on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Granted or denied */ }

        LaunchedEffect(Unit) {
            val status = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (status != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav")
            ) {
                AppNavTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag(tab.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            AppNavTab.TASKS -> {
                TasksScreen(
                    viewModel = taskViewModel,
                    subscriptionState = subscriptionState,
                    onOpenPayment = { showPaymentDialog = true },
                    onStartFocusForTask = { task ->
                        focusViewModel.selectTask(task)
                        currentTab = AppNavTab.FOCUS
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppNavTab.FOCUS -> {
                FocusScreen(
                    viewModel = focusViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppNavTab.CALENDAR -> {
                CalendarScreen(
                    viewModel = taskViewModel,
                    onStartFocusForTask = { task ->
                        focusViewModel.selectTask(task)
                        currentTab = AppNavTab.FOCUS
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppNavTab.ANALYTICS -> {
                AnalyticsScreen(
                    viewModel = analyticsViewModel,
                    subscriptionState = subscriptionState,
                    onOpenPayment = { showPaymentDialog = true },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    if (showPaymentDialog) {
        PaymentDialog(
            subscriptionState = subscriptionState,
            isMilestonePrompt = isMilestoneTrigger,
            onActivatePlan = { plan, txnRef ->
                onActivatePlan(plan, txnRef)
            },
            onDismiss = {
                showPaymentDialog = false
                isMilestoneTrigger = false
            }
        )
    }
}
