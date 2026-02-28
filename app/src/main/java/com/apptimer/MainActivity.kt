package com.apptimer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apptimer.database.AppDatabase
import com.apptimer.database.entities.AppLimit
import com.apptimer.service.MonitorService
import com.apptimer.ui.AppListAdapter
import com.apptimer.utils.AppUsageHelper
import com.apptimer.utils.PermissionHelper
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private lateinit var adapter: AppListAdapter
    private lateinit var monitorSwitch: SwitchMaterial
    private lateinit var permissionWarning: TextView
    private lateinit var periodInput: EditText
    private lateinit var appListRecyclerView: RecyclerView
    private lateinit var letterIndexList: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        database = AppDatabase.getDatabase(this)

        setupViews()
        checkPermissions()
        ensureMonitoringStartedIfEnabled()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        ensureMonitoringStartedIfEnabled()
        adapter.refreshUsageTimes()
    }

    override fun onPause() {
        super.onPause()
        adapter.saveAllLimits()
        if (monitorSwitch.isChecked && PermissionHelper.hasAllPermissions(this)) {
            MonitorService.refresh(this)
        }
    }

    private fun setupViews() {
        monitorSwitch = findViewById(R.id.monitorSwitch)
        permissionWarning = findViewById(R.id.permissionWarning)
        periodInput = findViewById(R.id.periodInput)
        appListRecyclerView = findViewById(R.id.appListRecyclerView)
        letterIndexList = findViewById(R.id.letterIndexList)

        val prefs = getSharedPreferences(MonitorService.PREFS_NAME, MODE_PRIVATE)
        val isMonitoringEnabled = prefs.getBoolean("monitoring_enabled", false)
        monitorSwitch.isChecked = isMonitoringEnabled

        val periodHours = prefs.getInt(MonitorService.KEY_TIME_PERIOD, 1).coerceAtLeast(1)
        periodInput.setText(periodHours.toString())

        periodInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                val raw = s?.toString()?.trim().orEmpty()
                if (raw.isEmpty()) return

                val hours = raw.toIntOrNull()?.coerceAtLeast(1) ?: return
                savePeriodSettings(hours)
            }
        })

        periodInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val normalizedHours = periodInput.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
                periodInput.setText(normalizedHours.toString())
                periodInput.setSelection(periodInput.text.length)
                savePeriodSettings(normalizedHours)
            }
        }

        appListRecyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AppListAdapter(this, packageManager, this) { packageName, timeLimit ->
            saveAppLimit(packageName, timeLimit)
        }
        appListRecyclerView.adapter = adapter
        setupLetterIndex()

        monitorSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("monitoring_enabled", isChecked).apply()

            if (isChecked) {
                if (PermissionHelper.hasAllPermissions(this)) {
                    adapter.saveAllLimits()
                    MonitorService.start(this)
                } else {
                    monitorSwitch.isChecked = false
                    checkPermissions()
                }
            } else {
                MonitorService.stop(this)
            }
        }
    }

    private fun setupLetterIndex() {
        val letters = ('A'..'Z').map { it.toString() } + "#"
        val indexAdapter = ArrayAdapter(this, R.layout.item_letter_index, letters)
        letterIndexList.adapter = indexAdapter

        val jumpToLetterAt = { position: Int ->
            val safePosition = position.coerceIn(0, letters.lastIndex)
            val letter = letters[safePosition].first()
            val targetPosition = adapter.getPositionForLetter(letter)
            appListRecyclerView.scrollToPosition(targetPosition)
        }

        letterIndexList.setOnItemClickListener { _, _, position, _ ->
            jumpToLetterAt(position)
        }

        letterIndexList.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                view.parent?.requestDisallowInterceptTouchEvent(true)
                val itemCount = letters.size
                if (itemCount > 0 && view.height > 0) {
                    val ratio = (event.y / view.height).coerceIn(0f, 0.9999f)
                    val position = (ratio * itemCount).toInt()
                    jumpToLetterAt(position)
                }
                true
            } else {
                view.parent?.requestDisallowInterceptTouchEvent(false)
                true
            }
        }
    }

    private fun checkPermissions() {
        val hasUsageStats = PermissionHelper.hasUsageStatsPermission(this)
        val hasOverlay = PermissionHelper.hasOverlayPermission(this)

        if (!hasUsageStats || !hasOverlay) {
            permissionWarning.visibility = View.VISIBLE
            val message = buildString {
                if (!hasUsageStats) append("需要使用统计权限\n")
                if (!hasOverlay) append("需要悬浮窗权限\n")
            }
            permissionWarning.text = message

            permissionWarning.setOnClickListener {
                if (!hasUsageStats) {
                    PermissionHelper.openUsageStatsSettings(this)
                } else if (!hasOverlay) {
                    PermissionHelper.openOverlaySettings(this)
                }
            }
        } else {
            permissionWarning.visibility = View.GONE
        }
    }

    private fun loadApps() {
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                AppUsageHelper.getInstalledUserApps(this@MainActivity)
            }
            adapter.setApps(apps)

            database.appLimitDao().getAllLimits().collect { limits ->
                adapter.setLimits(limits)
            }
        }
    }

    private fun saveAppLimit(packageName: String, timeLimit: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (packageName == this@MainActivity.packageName) {
                database.appLimitDao().deleteLimitByPackage(packageName)
                return@launch
            }

            if (timeLimit > 0) {
                val appName = AppUsageHelper.getAppName(this@MainActivity, packageName)
                val limit = AppLimit(
                    packageName = packageName,
                    appName = appName,
                    timeLimit = timeLimit,
                    enabled = true
                )
                database.appLimitDao().insertLimit(limit)
            } else {
                database.appLimitDao().deleteLimitByPackage(packageName)
            }

            withContext(Dispatchers.Main) {
                if (monitorSwitch.isChecked) {
                    MonitorService.refresh(this@MainActivity)
                }
            }
        }
    }

    private fun savePeriodSettings(hours: Int) {
        val prefs = getSharedPreferences(MonitorService.PREFS_NAME, MODE_PRIVATE)
        val previousHours = prefs.getInt(MonitorService.KEY_TIME_PERIOD, 1)
        if (hours == previousHours) return

        prefs.edit()
            .putInt(MonitorService.KEY_TIME_PERIOD, hours)
            .putLong(MonitorService.KEY_PERIOD_START_TIME, System.currentTimeMillis())
            .apply()

        if (monitorSwitch.isChecked) {
            MonitorService.refresh(this)
        }
    }

    private fun ensureMonitoringStartedIfEnabled() {
        val prefs = getSharedPreferences(MonitorService.PREFS_NAME, MODE_PRIVATE)
        val isMonitoringEnabled = prefs.getBoolean("monitoring_enabled", false)
        if (isMonitoringEnabled && PermissionHelper.hasAllPermissions(this)) {
            MonitorService.start(this)
        }
    }

}
