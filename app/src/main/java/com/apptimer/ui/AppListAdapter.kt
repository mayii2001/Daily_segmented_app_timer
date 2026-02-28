package com.apptimer.ui

import android.icu.text.Transliterator
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.apptimer.R
import com.apptimer.database.AppDatabase
import com.apptimer.database.entities.AppLimit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Comparator
import java.util.Locale

class AppListAdapter(
    private val context: Context,
    private val packageManager: PackageManager,
    private val lifecycleOwner: LifecycleOwner,
    private val onLimitChanged: (String, Long) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    private val apps = mutableListOf<ApplicationInfo>()
    private val limits = mutableMapOf<String, Long>()
    private val persistedLimits = mutableMapOf<String, Long>()
    private val viewHolders = mutableListOf<AppViewHolder>()
    private val letterPositionMap = mutableMapOf<Char, Int>()
    private val database = AppDatabase.getDatabase(context)
    private val hanToLatin by lazy {
        Transliterator.getInstance("Han-Latin; Latin-ASCII; Any-Upper")
    }

    fun setApps(newApps: List<ApplicationInfo>) {
        val collator = Comparator<ApplicationInfo> { a, b ->
            val aName = a.loadLabel(packageManager).toString().trim()
            val bName = b.loadLabel(packageManager).toString().trim()
            toSortKey(aName).compareTo(toSortKey(bName))
        }
        val sortedApps = newApps.sortedWith(collator)

        apps.clear()
        apps.addAll(sortedApps)
        buildLetterIndex()
        notifyDataSetChanged()
    }

    fun setLimits(appLimits: List<AppLimit>) {
        val incoming = appLimits.associate { it.packageName to it.timeLimit }
        if (persistedLimits == incoming) return

        persistedLimits.clear()
        persistedLimits.putAll(incoming)
        limits.clear()
        limits.putAll(incoming)
        notifyDataSetChanged()
    }

    fun refreshUsageTimes() {
        notifyDataSetChanged()
    }

    fun saveAllLimits() {
        viewHolders.forEach { holder ->
            holder.saveCurrentLimit()
        }
    }

    private suspend fun getTodayUsage(packageName: String): Long {
        val startOfDay = getStartOfDay()
        return database.usageHistoryDao().getTotalDurationToday(packageName, startOfDay) ?: 0L
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(app)

        if (!viewHolders.contains(holder)) {
            viewHolders.add(holder)
        }
    }

    override fun onViewRecycled(holder: AppViewHolder) {
        super.onViewRecycled(holder)
        viewHolders.remove(holder)
    }

    override fun getItemCount() = apps.size

    fun getPositionForLetter(letter: Char): Int {
        val normalized = letter.uppercaseChar()
        return letterPositionMap[normalized] ?: 0
    }

    private fun buildLetterIndex() {
        letterPositionMap.clear()
        apps.forEachIndexed { index, app ->
            val label = app.loadLabel(packageManager).toString().trim()
            val key = toIndexKey(label)
            if (!letterPositionMap.containsKey(key)) {
                letterPositionMap[key] = index
            }
        }
    }

    private fun toSortKey(label: String): String {
        if (label.isBlank()) return "ZZZZ#"
        val normalized = runCatching { hanToLatin.transliterate(label) }
            .getOrElse { label }
            .uppercase(Locale.getDefault())
        val clean = normalized.replace(Regex("[^A-Z0-9]"), "")
        return if (clean.isNotEmpty()) clean else "ZZZZ#"
    }

    private fun toIndexKey(label: String): Char {
        if (label.isBlank()) return '#'
        val normalized = runCatching { hanToLatin.transliterate(label) }
            .getOrElse { label }
            .uppercase(Locale.getDefault())
        return normalized.firstOrNull { it in 'A'..'Z' } ?: '#'
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        private val appName: TextView = itemView.findViewById(R.id.appName)
        private val usageText: TextView = itemView.findViewById(R.id.usageText)
        private val timeLimitInput: EditText = itemView.findViewById(R.id.timeLimitInput)

        private var textWatcher: TextWatcher? = null
        private var currentPackageName: String? = null

        fun bind(app: ApplicationInfo) {
            textWatcher?.let { timeLimitInput.removeTextChangedListener(it) }
            timeLimitInput.onFocusChangeListener = null

            currentPackageName = app.packageName
            appIcon.setImageDrawable(app.loadIcon(packageManager))
            appName.text = app.loadLabel(packageManager)
            usageText.text = "Today usage: loading..."
            val boundPackageName = app.packageName

            lifecycleOwner.lifecycleScope.launch {
                val todayUsage = withContext(Dispatchers.IO) {
                    runCatching { getTodayUsage(boundPackageName) }.getOrDefault(0L)
                }
                val minutes = todayUsage / 60
                val position = bindingAdapterPosition
                val isStillBound = position != RecyclerView.NO_POSITION &&
                    position < apps.size &&
                    apps[position].packageName == boundPackageName
                if (isStillBound) {
                    usageText.text = "Today usage: ${minutes} min"
                }
            }

            val currentLimit = limits[app.packageName]
            if (currentLimit != null && currentLimit > 0) {
                val minutes = currentLimit / 60.0
                val displayText = if (minutes % 1.0 == 0.0) {
                    minutes.toInt().toString()
                } else {
                    String.format(Locale.US, "%.1f", minutes)
                }
                timeLimitInput.setText(displayText)
            } else {
                timeLimitInput.setText("")
            }

            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable?) {
                    val packageName = currentPackageName ?: return
                    val minutes = s?.toString()?.toDoubleOrNull() ?: 0.0
                    val seconds = if (minutes > 0) (minutes * 60).toLong() else 0L
                    limits[packageName] = seconds
                }
            }

            timeLimitInput.addTextChangedListener(textWatcher)
            timeLimitInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    saveCurrentLimit()
                }
            }
        }

        fun saveCurrentLimit() {
            val pkg = currentPackageName ?: return
            val minutes = timeLimitInput.text?.toString()?.toDoubleOrNull() ?: 0.0
            val seconds = if (minutes > 0) (minutes * 60).toLong() else 0L

            val persisted = persistedLimits[pkg] ?: 0L
            if (seconds == persisted) {
                limits[pkg] = seconds
                return
            }

            limits[pkg] = seconds
            onLimitChanged(pkg, seconds)
            if (seconds > 0) {
                persistedLimits[pkg] = seconds
            } else {
                persistedLimits.remove(pkg)
            }
        }
    }
}
