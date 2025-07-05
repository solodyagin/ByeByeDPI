package io.github.dovecoteescapee.byedpi.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.github.dovecoteescapee.byedpi.R
import io.github.dovecoteescapee.byedpi.data.Mode
import io.github.dovecoteescapee.byedpi.data.AppStatus
import io.github.dovecoteescapee.byedpi.services.appStatus
import io.github.dovecoteescapee.byedpi.services.ServiceManager
import io.github.dovecoteescapee.byedpi.utility.HistoryUtils
import io.github.dovecoteescapee.byedpi.utility.getPreferences
import io.github.dovecoteescapee.byedpi.utility.SiteCheckUtils
import androidx.core.content.edit
import kotlinx.coroutines.*
import java.io.File

class TestActivity : AppCompatActivity() {

    private lateinit var scrollTextView: ScrollView
    private lateinit var progressTextView: TextView
    private lateinit var resultsTextView: TextView
    private lateinit var startStopButton: Button

    private lateinit var siteChecker: SiteCheckUtils
    private lateinit var cmdHistoryUtils: HistoryUtils
    private lateinit var sites: MutableList<String>
    private lateinit var cmds: List<String>

    private var savedCmd: String = ""
    private var testJob: Job? = null

    private val proxyIp: String = "127.0.0.1"
    private val proxyPort: Int = 10080

    private var isTesting: Boolean
        get() = prefs.getBoolean("is_test_running", false)
        set(value) {
            prefs.edit { putBoolean("is_test_running", value) }
        }

    private val prefs by lazy { getPreferences() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proxy_test)

        siteChecker = SiteCheckUtils(proxyIp, proxyPort)
        cmdHistoryUtils = HistoryUtils(this)
        scrollTextView = findViewById(R.id.scrollView)
        startStopButton = findViewById(R.id.startStopButton)
        resultsTextView = findViewById(R.id.resultsTextView)
        progressTextView = findViewById(R.id.progressTextView)

        resultsTextView.movementMethod = LinkMovementMethod.getInstance()

        if (isTesting) {
            progressTextView.text = getString(R.string.test_proxy_error)
            resultsTextView.text = getString(R.string.test_crash)
            isTesting = false
        } else {
            lifecycleScope.launch {
                val previousLogs = loadLog()
                if (previousLogs.isNotEmpty()) {
                    progressTextView.text = getString(R.string.test_complete)
                    resultsTextView.text = ""
                    displayLog(previousLogs)
                }
            }
        }

        startStopButton.setOnClickListener {
            startStopButton.isClickable = false

            if (isTesting) {
                stopTesting()
            } else {
                startTesting()
            }

            startStopButton.postDelayed({ startStopButton.isClickable = true }, 500)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTesting) {
                    stopTesting()
                }
                finish()
            }
        })

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_test, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                if (!isTesting) {
                    val intent = Intent(this, TestSettingsActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_SHORT).show()
                }
                true
            }
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private suspend fun startProxyService() {
        withContext(Dispatchers.IO) {
            try {
                ServiceManager.start(this@TestActivity, Mode.Proxy)
            } catch (e: Exception) {
                Log.e("TestActivity", "Error start proxy service: ${e.message}")
            }
        }
    }

    private suspend fun stopProxyService() {
        withContext(Dispatchers.IO) {
            try {
                ServiceManager.stop(this@TestActivity)
            } catch (e: Exception) {
                Log.e("TestActivity", "Error stop proxy service: ${e.message}")
            }
        }
    }

    private suspend fun waitForProxyStatus(statusNeeded: AppStatus): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 3000) {
            if (appStatus.first == statusNeeded) {
                delay(100)
                return true
            }
            delay(100)
        }
        return false
    }

    private suspend fun isProxyRunning(): Boolean = withContext(Dispatchers.IO) {
        appStatus.first == AppStatus.Running
    }

    private fun updateCmdArgs(cmd: String) {
        prefs.edit { putString("byedpi_cmd_args", cmd) }
    }

    private fun startTesting() {
        sites = loadSites().toMutableList()
        cmds = loadCmds()

        if (sites.isEmpty()) {
            resultsTextView.text = ""
            appendTextToResults("${getString(R.string.test_settings_domain_empty)}\n")
            return
        }

        testJob = lifecycleScope.launch(Dispatchers.IO) {
            isTesting = true
            savedCmd = prefs.getString("byedpi_cmd_args", "").orEmpty()
            clearLog()

            withContext(Dispatchers.Main) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                startStopButton.text = getString(R.string.test_stop)
                progressTextView.text = ""
                resultsTextView.text = ""
            }

            val delaySec = prefs.getString("byedpi_proxytest_delay", "1")?.toIntOrNull() ?: 1
            val fullLog = prefs.getBoolean("byedpi_proxytest_fulllog", false)
            val logClickable = prefs.getBoolean("byedpi_proxytest_logclickable", false)
            val requestsCount = prefs.getString("byedpi_proxytest_requestsсount", "1")?.toIntOrNull()?.takeIf { it > 0 } ?: 1

            val successfulCmds = mutableListOf<Triple<String, Int, Int>>()

            for ((index, cmd) in cmds.withIndex()) {
                val cmdIndex = index + 1

                withContext(Dispatchers.Main) {
                    progressTextView.text = getString(R.string.test_process, cmdIndex, cmds.size)
                }

                updateCmdArgs("--ip $proxyIp --port $proxyPort $cmd")

                if (isProxyRunning()) stopTesting()
                else startProxyService()

                withContext(Dispatchers.Main) {
                    if (logClickable) {
                        appendLinkToResults("$cmd\n")
                    } else {
                        appendTextToResults("$cmd\n")
                    }
                }

                if (!waitForProxyStatus(AppStatus.Running)) {
                    stopTesting()
                }

                val totalRequests = sites.size * requestsCount
                val checkResults = siteChecker.checkSitesAsync(
                    sites = sites,
                    requestsCount = requestsCount,
                    fullLog = fullLog,
                    onSiteChecked = { site, successCount, countRequests ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            appendTextToResults("$site - $successCount/$countRequests\n")
                        }
                    }
                )

                val successfulCount = checkResults.sumOf { it.second }
                val successPercentage = (successfulCount * 100) / totalRequests

                if (successPercentage >= 50) successfulCmds.add(Triple(cmd, successfulCount, totalRequests))

                withContext(Dispatchers.Main) {
                    appendTextToResults("$successfulCount/$totalRequests ($successPercentage%)\n\n")
                }

                if (isProxyRunning()) stopProxyService()
                else stopTesting()

                if (!waitForProxyStatus(AppStatus.Halted)) {
                    stopTesting()
                }

                delay(delaySec * 1000L)
            }

            successfulCmds.sortByDescending { it.second }

            withContext(Dispatchers.Main) {
                appendTextToResults("${getString(R.string.test_good_cmds)}\n\n")

                successfulCmds.forEachIndexed { index, (cmd, successCount, total) ->
                    appendTextToResults("${index + 1}. ")
                    appendLinkToResults("$cmd\n")
                    appendTextToResults("$successCount/$total\n\n")
                }

                appendTextToResults(getString(R.string.test_complete_info))
            }

            withContext(Dispatchers.Main) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                progressTextView.text = getString(R.string.test_complete)
            }

            stopTesting()
        }
    }

    private fun stopTesting() {
        isTesting = false
        
        updateCmdArgs(savedCmd)

        lifecycleScope.launch {
            if (isProxyRunning()) stopProxyService()

            testJob?.cancel()
            testJob = null

            startStopButton.text = getString(R.string.test_start)
        }
    }

    private fun appendTextToResults(text: String) {
        resultsTextView.append(text)
        if (isTesting) saveLog(text)
        scrollToBottom()
    }

    private fun appendLinkToResults(text: String) {
        val spannableString = SpannableString(text)
        val menuItems = arrayOf(
            getString(R.string.cmd_history_apply),
            getString(R.string.cmd_history_copy)
        )

        spannableString.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    AlertDialog.Builder(this@TestActivity)
                        .setTitle(getString(R.string.cmd_history_menu))
                        .setItems(menuItems) { _, which ->
                            when (which) {
                                0 -> addToHistory(text.trim())
                                1 -> copyToClipboard(text.trim())
                            }
                        }
                        .show()
                }
            },
            0,
            text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        resultsTextView.append(spannableString)
        if (isTesting) saveLog("{$text}")
        scrollToBottom()
    }

    private fun scrollToBottom() {
        scrollTextView.post {
            scrollTextView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("command", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun addToHistory(command: String) {
        updateCmdArgs(command)
        cmdHistoryUtils.addCommand(command)
    }

    private fun displayLog(log: String) {
        log.split("{", "}").forEachIndexed { index, part ->
            if (index % 2 == 0) {
                appendTextToResults(part)
            } else {
                appendLinkToResults(part)
            }
        }
    }

    private fun saveLog(text: String) {
        val file = File(filesDir, "proxy_test.log")
        file.appendText(text)
    }

    private fun loadLog(): String {
        val file = File(filesDir, "proxy_test.log")
        return if (file.exists()) file.readText() else ""
    }

    private fun clearLog() {
        val file = File(filesDir, "proxy_test.log")
        file.writeText("")
    }

    private fun loadSites(): List<String> {
        val defaultDomainLists = setOf("youtube", "googlevideo")
        val selectedDomainLists = prefs.getStringSet("byedpi_proxytest_domain_lists", defaultDomainLists)?: return emptyList()

        val allDomains = mutableListOf<String>()

        for (domainList in selectedDomainLists) {
            val domains = when (domainList) {
                "custom" -> {
                    val customDomains = prefs.getString("byedpi_proxytest_domains", "").orEmpty()
                    customDomains.lines().map { it.trim() }.filter { it.isNotEmpty() }
                }
                else -> {
                    try {
                        assets.open("proxytest_$domainList.sites").bufferedReader().useLines { it.toList() }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }
            allDomains.addAll(domains)
        }

        return allDomains.distinct()
    }

    private fun loadCmds(): List<String> {
        val userCommands = prefs.getBoolean("byedpi_proxytest_usercommands", false)
        return if (userCommands) {
            val commands = prefs.getString("byedpi_proxytest_commands", "").orEmpty()
            commands.lines().map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            assets.open("proxytest_strategies.list").bufferedReader().useLines { it.toList() }
        }
    }
}
