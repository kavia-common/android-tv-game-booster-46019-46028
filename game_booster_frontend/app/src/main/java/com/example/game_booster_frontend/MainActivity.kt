package com.example.game_booster_frontend

import android.app.ActivityManager
import android.content.Context
import android.os.*
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.RandomAccessFile
import kotlin.math.roundToInt

/**
 * PUBLIC_INTERFACE
 * Main Activity for Android TV Game Booster
 * Provides:
 * - Ocean Professional themed UI (header stats, action buttons, and games grid)
 * - Real-time CPU and Memory metrics (GPU stubbed with TODO)
 * - D-pad focus and navigation
 * - Placeholder actions for Boost, Clean, Optimize with user feedback
 *
 * How to extend metrics and optimizations:
 * - CPU: reads from /proc/stat. You can switch to StatFs or BatteryManager if needed.
 * - Memory: uses ActivityManager.MemoryInfo. Consider Process API and per-app stats.
 * - GPU: Android does not expose generic GPU utilization; vendor/private APIs may be needed.
 * - Network: Implement QoS tuning via TrafficStats, or app-specific prefetching strategies.
 * - Boost/Clean: Consider killing background processes (with caution), clearing caches, or lowering animation scales.
 */
class MainActivity : FragmentActivity() {

    private lateinit var cpuValue: TextView
    private lateinit var gpuValue: TextView
    private lateinit var memValue: TextView
    private lateinit var btnBoost: TextView
    private lateinit var btnClean: TextView
    private lateinit var btnOptimize: TextView
    private lateinit var gamesGrid: RecyclerView

    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var metricsJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cpuValue = findViewById(R.id.cpu_value)
        gpuValue = findViewById(R.id.gpu_value)
        memValue = findViewById(R.id.mem_value)
        btnBoost = findViewById(R.id.btn_boost)
        btnClean = findViewById(R.id.btn_clean)
        btnOptimize = findViewById(R.id.btn_optimize)
        gamesGrid = findViewById(R.id.games_grid)

        setupActions()
        setupGamesGrid()
        startMetricsUpdates()

        // Initial focus for TV
        btnBoost.requestFocus()
    }

    private fun setupActions() {
        val focusScaleUp: (View) -> Unit = { v ->
            v.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(130)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
        val focusScaleDown: (View) -> Unit = { v ->
            v.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(130)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) focusScaleUp(v) else focusScaleDown(v)
        }

        btnBoost.onFocusChangeListener = focusListener
        btnClean.onFocusChangeListener = focusListener
        btnOptimize.onFocusChangeListener = focusListener

        btnBoost.setOnClickListener {
            applyBoost()
        }
        btnClean.setOnClickListener {
            applyClean()
        }
        btnOptimize.setOnClickListener {
            applyOptimize()
        }
    }

    private fun setupGamesGrid() {
        val spanCount = 4
        gamesGrid.layoutManager = GridLayoutManager(this, spanCount, RecyclerView.VERTICAL, false)
        val items = demoGames()
        val adapter = GamesAdapter(items) { item ->
            showToast("Launching ${item.title} (placeholder)")
            // TODO: Integrate launching installed games via PackageManager or deep links
        }
        gamesGrid.adapter = adapter

        // Let grid handle focus properly
        gamesGrid.descendantFocusability = ViewGroupFocusAfterDescendants
    }

    private fun startMetricsUpdates() {
        metricsJob?.cancel()
        metricsJob = uiScope.launch {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            var lastCpuStat: CpuSample? = null

            while (isActive) {
                // CPU usage
                val current = withContext(Dispatchers.IO) { readCpuSample() }
                val usage = if (lastCpuStat != null && current != null) {
                    computeCpuUsagePercent(lastCpuStat!!, current)
                } else null
                if (current != null) lastCpuStat = current

                // Memory usage
                val memPercent = readMemoryUsagePercent(activityManager)

                cpuValue.text = if (usage != null) "${usage.roundToInt()}%" else "--%"
                memValue.text = "${memPercent.roundToInt()}%"

                // GPU usage (stubbed)
                // TODO: GPU utilization is not generally exposed. Integrate vendor APIs if available.
                gpuValue.text = "N/A"

                delay(1500L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        metricsJob?.cancel()
        uiScope.cancel()
    }

    private fun applyBoost() {
        // Placeholder: future implementation might adjust process priorities, disable animations, etc.
        showToast(getString(R.string.feedback_boost_done))
    }

    private fun applyClean() {
        // Placeholder: reclaim memory (close background apps, clear caches with caution)
        showToast(getString(R.string.feedback_clean_done))
    }

    private fun applyOptimize() {
        // Placeholder: optimize network (TrafficStats tagging, DNS prefetch, etc.)
        showToast(getString(R.string.feedback_optimize_done))
    }

    private fun showToast(message: String) {
        val t = Toast.makeText(this, message, Toast.LENGTH_SHORT)
        t.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 48)
        t.show()
    }

    // ==== Metrics helpers ====

    private data class CpuSample(val idle: Long, val total: Long)

    private fun readCpuSample(): CpuSample? {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine() // line starts with "cpu  ..."
            reader.close()

            val toks = load.split("\\s+".toRegex()).toTypedArray()
            // toks[0] = "cpu"
            val user = toks[1].toLong()
            val nice = toks[2].toLong()
            val system = toks[3].toLong()
            val idle = toks[4].toLong()
            val iowait = if (toks.size > 5) toks[5].toLong() else 0L
            val irq = if (toks.size > 6) toks[6].toLong() else 0L
            val softirq = if (toks.size > 7) toks[7].toLong() else 0L
            val steal = if (toks.size > 8) toks[8].toLong() else 0L

            val idleAll = idle + iowait
            val systemAll = system + irq + softirq
            val total = user + nice + systemAll + idleAll + steal

            CpuSample(idleAll, total)
        } catch (e: Exception) {
            null
        }
    }

    private fun computeCpuUsagePercent(prev: CpuSample, cur: CpuSample): Float {
        val totalDiff = (cur.total - prev.total).toFloat().coerceAtLeast(1f)
        val idleDiff = (cur.idle - prev.idle).toFloat().coerceAtLeast(0f)
        val usage = (totalDiff - idleDiff) / totalDiff * 100f
        return usage.coerceIn(0f, 100f)
    }

    private fun readMemoryUsagePercent(activityManager: ActivityManager): Float {
        val mi = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(mi)
        val used = mi.totalMem - mi.availMem
        val percent = used.toDouble() / mi.totalMem.toDouble() * 100.0
        return percent.toFloat()
    }

    // ==== Demo games ====

    data class GameItem(val title: String)

    private fun demoGames(): List<GameItem> {
        return listOf(
            GameItem("Racing Turbo"),
            GameItem("Galaxy Wars"),
            GameItem("Mystic Quest"),
            GameItem("Shadow Arena"),
            GameItem("Pixel Adventure"),
            GameItem("Cyber Runner"),
            GameItem("Battle Forge"),
            GameItem("Retro Rally"),
            GameItem("Ocean Drift"),
            GameItem("Sky Fortress"),
        )
    }

    // RecyclerView Adapter and ViewHolder
    private inner class GamesAdapter(
        private val items: List<GameItem>,
        private val onClick: (GameItem) -> Unit
    ) : RecyclerView.Adapter<GameViewHolder>() {

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): GameViewHolder {
            val view = layoutInflater.inflate(R.layout.item_game_tile, parent, false)
            return GameViewHolder(view)
        }

        override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
            holder.bind(items[position], onClick)
        }

        override fun getItemCount(): Int = items.size
    }

    private inner class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.game_title)

        fun bind(item: GameItem, onClick: (GameItem) -> Unit) {
            title.text = item.title

            val focusScaleUp: (View) -> Unit = { v ->
                v.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .setDuration(120)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            val focusScaleDown: (View) -> Unit = { v ->
                v.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) focusScaleUp(v) else focusScaleDown(v)
            }

            itemView.setOnClickListener { onClick(item) }
            itemView.isFocusable = true
            itemView.isFocusableInTouchMode = true
        }
    }

    // Allow back key to exit and handle select
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                // Let currently focused view handle click
                currentFocus?.performClick()
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    companion object {
        // A descendantFocusability flag for clarity (to avoid importing ViewGroup just for constant)
        const val ViewGroupFocusAfterDescendants = 0x20000 // ViewGroup.FOCUS_AFTER_DESCENDANTS
    }
}
