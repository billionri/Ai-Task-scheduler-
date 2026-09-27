package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

enum class SubscriptionPlan(
    val id: String,
    val title: String,
    val durationText: String,
    val priceRupees: Int,
    val amountFormatted: String,
    val durationDays: Int,
    val badge: String? = null,
    val savings: String? = null
) {
    MONTHLY(
        id = "monthly_200",
        title = "1 Month Recharge",
        durationText = "30 Days Unlimited",
        priceRupees = 200,
        amountFormatted = "₹200",
        durationDays = 30,
        badge = null,
        savings = "Standard Plan"
    ),
    ANNUAL(
        id = "annual_2000",
        title = "1 Year Pro Plan",
        durationText = "365 Days Unlimited",
        priceRupees = 2000,
        amountFormatted = "₹2,000",
        durationDays = 365,
        badge = "BEST VALUE",
        savings = "Save ₹400 (17% OFF)"
    )
}

data class SubscriptionState(
    val isPro: Boolean = false,
    val planName: String = "Free Plan",
    val expiryDateMillis: Long = 0L,
    val transactionRef: String = "",
    val upiVpa: String = "Rashid.alam@oksbi",
    val payeeName: String = "Rashid Alam",
    val demoTasksAdded: Int = 0,
    val demoTasksCompleted: Int = 0,
    val hasShownMilestoneDialog: Boolean = false
) {
    val daysRemaining: Long
        get() {
            if (!isPro) return 0L
            val diff = expiryDateMillis - System.currentTimeMillis()
            return if (diff > 0) TimeUnit.MILLISECONDS.toDays(diff) + 1 else 0L
        }

    val isDemoMilestoneReached: Boolean
        get() = (demoTasksAdded >= 2 && demoTasksCompleted >= 2) || (demoTasksCompleted >= 2)

    val demoProgressFraction: Float
        get() {
            val addedScore = (demoTasksAdded.coerceAtMost(2))
            val completedScore = (demoTasksCompleted.coerceAtMost(2))
            return ((addedScore + completedScore) / 4f).coerceIn(0f, 1f)
        }
}

class SubscriptionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("task_scheduler_sub_prefs", Context.MODE_PRIVATE)
    private val _subscriptionState = MutableStateFlow(loadSubscriptionState())
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    companion object {
        const val UPI_ID = "Rashid.alam@oksbi"
        const val PAYEE_NAME = "Rashid Alam"
        private const val KEY_IS_PRO = "key_is_pro"
        private const val KEY_PLAN_NAME = "key_plan_name"
        private const val KEY_EXPIRY_MILLIS = "key_expiry_millis"
        private const val KEY_TXN_REF = "key_txn_ref"
        private const val KEY_DEMO_TASKS_ADDED = "key_demo_tasks_added"
        private const val KEY_DEMO_TASKS_COMPLETED = "key_demo_tasks_completed"
        private const val KEY_HAS_SHOWN_MILESTONE = "key_has_shown_milestone"
    }

    private fun loadSubscriptionState(): SubscriptionState {
        val expiry = prefs.getLong(KEY_EXPIRY_MILLIS, 0L)
        val isExpired = expiry > 0 && expiry < System.currentTimeMillis()
        val isPro = prefs.getBoolean(KEY_IS_PRO, false) && !isExpired
        val planName = prefs.getString(KEY_PLAN_NAME, "Free Plan") ?: "Free Plan"
        val txnRef = prefs.getString(KEY_TXN_REF, "") ?: ""
        val demoTasksAdded = prefs.getInt(KEY_DEMO_TASKS_ADDED, 0)
        val demoTasksCompleted = prefs.getInt(KEY_DEMO_TASKS_COMPLETED, 0)
        val hasShownMilestone = prefs.getBoolean(KEY_HAS_SHOWN_MILESTONE, false)

        return SubscriptionState(
            isPro = isPro,
            planName = if (isPro) planName else "Free Plan",
            expiryDateMillis = if (isPro) expiry else 0L,
            transactionRef = txnRef,
            upiVpa = UPI_ID,
            payeeName = PAYEE_NAME,
            demoTasksAdded = demoTasksAdded,
            demoTasksCompleted = demoTasksCompleted,
            hasShownMilestoneDialog = hasShownMilestone
        )
    }

    fun recordDemoTaskAdded(): Boolean {
        val currentAdded = prefs.getInt(KEY_DEMO_TASKS_ADDED, 0) + 1
        prefs.edit().putInt(KEY_DEMO_TASKS_ADDED, currentAdded).apply()
        val currentCompleted = prefs.getInt(KEY_DEMO_TASKS_COMPLETED, 0)
        _subscriptionState.value = _subscriptionState.value.copy(
            demoTasksAdded = currentAdded
        )
        // Returns true if milestone newly reached
        return (currentAdded >= 2 && currentCompleted >= 2)
    }

    fun recordDemoTaskCompleted(): Boolean {
        val currentCompleted = prefs.getInt(KEY_DEMO_TASKS_COMPLETED, 0) + 1
        prefs.edit().putInt(KEY_DEMO_TASKS_COMPLETED, currentCompleted).apply()
        val currentAdded = prefs.getInt(KEY_DEMO_TASKS_ADDED, 0)
        _subscriptionState.value = _subscriptionState.value.copy(
            demoTasksCompleted = currentCompleted
        )
        // Returns true if milestone reached
        return (currentAdded >= 2 && currentCompleted >= 2) || (currentCompleted >= 2)
    }

    fun markMilestonePromptShown() {
        prefs.edit().putBoolean(KEY_HAS_SHOWN_MILESTONE, true).apply()
        _subscriptionState.value = _subscriptionState.value.copy(hasShownMilestoneDialog = true)
    }

    fun activatePlan(plan: SubscriptionPlan, txnRef: String) {
        val currentExpiry = _subscriptionState.value.expiryDateMillis
        val baseTime = if (currentExpiry > System.currentTimeMillis()) currentExpiry else System.currentTimeMillis()
        val additionalMillis = TimeUnit.DAYS.toMillis(plan.durationDays.toLong())
        val newExpiry = baseTime + additionalMillis

        prefs.edit()
            .putBoolean(KEY_IS_PRO, true)
            .putString(KEY_PLAN_NAME, plan.title)
            .putLong(KEY_EXPIRY_MILLIS, newExpiry)
            .putString(KEY_TXN_REF, txnRef)
            .apply()

        _subscriptionState.value = _subscriptionState.value.copy(
            isPro = true,
            planName = plan.title,
            expiryDateMillis = newExpiry,
            transactionRef = txnRef,
            upiVpa = UPI_ID,
            payeeName = PAYEE_NAME
        )
    }

    fun resetSubscription() {
        prefs.edit().clear().apply()
        _subscriptionState.value = SubscriptionState()
    }
}
