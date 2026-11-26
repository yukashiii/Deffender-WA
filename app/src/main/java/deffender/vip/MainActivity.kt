package deffender.vip

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var selectedDnsIndex = 0

    // Tabs
    private lateinit var layoutHome: LinearLayout
    private lateinit var layoutDnsList: LinearLayout
    private lateinit var layoutStats: LinearLayout
    private lateinit var layoutAccount: LinearLayout

    // Home Views
    private lateinit var tvTimer: TextView
    private lateinit var tvConnectionBadge: TextView
    private lateinit var btnPower: FrameLayout
    private lateinit var tvSelectedDnsBadge: TextView
    private lateinit var tvSelectedDnsName: TextView
    private lateinit var tvSelectedDnsHost: TextView
    private lateinit var btnQuickSwitchDns: ImageButton
    private lateinit var btnHomeBuyKey: TextView

    // DNS Selector Tab Views (3 Host DNS)
    private lateinit var cardDns1: LinearLayout
    private lateinit var cardDns2: LinearLayout
    private lateinit var cardDns3: LinearLayout
    private lateinit var radioDns1: View
    private lateinit var radioDns2: View
    private lateinit var radioDns3: View

    // Stats & VIP / Buy Key Views
    private lateinit var tvStatDnsName: TextView
    private lateinit var tvStatDnsHost: TextView
    private lateinit var tvStatPing: TextView
    private lateinit var tvAccountOwner: TextView
    private lateinit var tvAccountToken: TextView
    private lateinit var tvAccountExpiry: TextView
    private lateinit var tvCurrentBuyKeyUrl: TextView
    private lateinit var btnOpenBuyKeyWeb: Button
    private lateinit var btnEditBuyKeyUrl: TextView
    private lateinit var btnLogoutToken: TextView

    // Bottom Nav Pill Buttons
    private lateinit var navHome: LinearLayout
    private lateinit var navDns: LinearLayout
    private lateinit var navStats: LinearLayout
    private lateinit var navAccount: LinearLayout

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateTimerDisplay()
            timerHandler.postDelayed(this, 1000)
        }
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startSelectedDnsVpn()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)
        selectedDnsIndex = sessionManager.getSelectedDnsIndex().coerceIn(0, 2)

        bindViews()
        setupDnsCards()
        setupNavigation()
        refreshDnsSelectionUi()
        refreshConnectionUi()
    }

    private fun bindViews() {
        layoutHome = findViewById(R.id.layoutTabHome)
        layoutDnsList = findViewById(R.id.layoutTabDns)
        layoutStats = findViewById(R.id.layoutTabStats)
        layoutAccount = findViewById(R.id.layoutTabAccount)

        tvTimer = findViewById(R.id.tvConnectionTimer)
        tvConnectionBadge = findViewById(R.id.tvConnectionStatusBadge)
        btnPower = findViewById(R.id.btnPowerConnect)
        tvSelectedDnsBadge = findViewById(R.id.tvSelectedDnsBadge)
        tvSelectedDnsName = findViewById(R.id.tvSelectedDnsName)
        tvSelectedDnsHost = findViewById(R.id.tvSelectedDnsHost)
        btnQuickSwitchDns = findViewById(R.id.btnQuickSwitchDns)
        btnHomeBuyKey = findViewById(R.id.btnHomeBuyKey)

        cardDns1 = findViewById(R.id.cardDnsOption1)
        cardDns2 = findViewById(R.id.cardDnsOption2)
        cardDns3 = findViewById(R.id.cardDnsOption3)
        radioDns1 = findViewById(R.id.radioDns1)
        radioDns2 = findViewById(R.id.radioDns2)
        radioDns3 = findViewById(R.id.radioDns3)

        tvStatDnsName = findViewById(R.id.tvStatDnsName)
        tvStatDnsHost = findViewById(R.id.tvStatDnsHost)
        tvStatPing = findViewById(R.id.tvStatPing)
        tvAccountOwner = findViewById(R.id.tvAccountOwner)
        tvAccountToken = findViewById(R.id.tvAccountToken)
        tvAccountExpiry = findViewById(R.id.tvAccountExpiry)
        tvCurrentBuyKeyUrl = findViewById(R.id.tvCurrentBuyKeyUrl)
        btnOpenBuyKeyWeb = findViewById(R.id.btnOpenBuyKeyWeb)
        btnEditBuyKeyUrl = findViewById(R.id.btnEditBuyKeyUrl)
        btnLogoutToken = findViewById(R.id.btnLogoutToken)

        navHome = findViewById(R.id.navItemHome)
        navDns = findViewById(R.id.navItemDns)
        navStats = findViewById(R.id.navItemStats)
        navAccount = findViewById(R.id.navItemAccount)

        btnPower.setOnClickListener {
            if (DnsVpnService.isRunning) {
                stopDnsVpn()
            } else {
                prepareAndStartVpn()
            }
        }

        findViewById<LinearLayout>(R.id.cardCurrentDnsHome).setOnClickListener {
            cycleNextDns()
        }
        btnQuickSwitchDns.setOnClickListener {
            cycleNextDns()
        }

        btnHomeBuyKey.setOnClickListener {
            openBuyKeyWebsite()
        }
        btnOpenBuyKeyWeb.setOnClickListener {
            openBuyKeyWebsite()
        }
        btnEditBuyKeyUrl.setOnClickListener {
            showConfigureBuyKeyDialog()
        }

        btnLogoutToken.setOnClickListener {
            stopDnsVpn()
            sessionManager.clearSession()
            startActivity(Intent(this, LoginTokenActivity::class.java))
            finish()
        }

        tvAccountOwner.text = sessionManager.getOwnerName()
        tvAccountToken.text = sessionManager.getSavedToken() ?: "-"
        tvAccountExpiry.text = sessionManager.getExpiresAt()
        tvCurrentBuyKeyUrl.text = sessionManager.getBuyKeyUrl()
    }

    private fun openBuyKeyWebsite() {
        val url = sessionManager.getBuyKeyUrl()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak dapat membuka link: $url", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showConfigureBuyKeyDialog() {
        val input = EditText(this).apply {
            setText(sessionManager.getBuyKeyUrl())
            hint = "https://website-anda.com/buy-key"
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("Konfigurasi Link Buy Key")
            .setMessage("Masukkan URL website pembelian API Key VIP:")
            .setView(input)
            .setPositiveButton("Simpan") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.startsWith("http://") || newUrl.startsWith("https://")) {
                    sessionManager.saveCustomBuyKeyUrl(newUrl)
                    tvCurrentBuyKeyUrl.text = newUrl
                    Toast.makeText(this, "Link Buy Key diperbarui!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "URL harus diawali http:// atau https://", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun setupDnsCards() {
        val options = AppConfig.DNS_OPTIONS
        findViewById<TextView>(R.id.tvDns1Title).text = options[0].name
        findViewById<TextView>(R.id.tvDns1Host).text = options[0].dnsHost
        findViewById<TextView>(R.id.tvDns1Sub).text = options[0].description

        findViewById<TextView>(R.id.tvDns2Title).text = options[1].name
        findViewById<TextView>(R.id.tvDns2Host).text = options[1].dnsHost
        findViewById<TextView>(R.id.tvDns2Sub).text = options[1].description

        findViewById<TextView>(R.id.tvDns3Title).text = options[2].name
        findViewById<TextView>(R.id.tvDns3Host).text = options[2].dnsHost
        findViewById<TextView>(R.id.tvDns3Sub).text = options[2].description

        cardDns1.setOnClickListener { selectDns(0) }
        cardDns2.setOnClickListener { selectDns(1) }
        cardDns3.setOnClickListener { selectDns(2) }
    }

    private fun setupNavigation() {
        navHome.setOnClickListener { switchTab(0) }
        navDns.setOnClickListener { switchTab(1) }
        navStats.setOnClickListener { switchTab(2) }
        navAccount.setOnClickListener { switchTab(3) }
        switchTab(0)
    }

    private fun switchTab(tabIndex: Int) {
        layoutHome.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        layoutDnsList.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        layoutStats.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE
        layoutAccount.visibility = if (tabIndex == 3) View.VISIBLE else View.GONE

        navHome.setBackgroundResource(if (tabIndex == 0) R.drawable.bg_nav_active_pill else 0)
        navDns.setBackgroundResource(if (tabIndex == 1) R.drawable.bg_nav_active_pill else 0)
        navStats.setBackgroundResource(if (tabIndex == 2) R.drawable.bg_nav_active_pill else 0)
        navAccount.setBackgroundResource(if (tabIndex == 3) R.drawable.bg_nav_active_pill else 0)
    }

    private fun cycleNextDns() {
        val nextIndex = (selectedDnsIndex + 1) % AppConfig.DNS_OPTIONS.size
        selectDns(nextIndex)
    }

    private fun selectDns(index: Int) {
        selectedDnsIndex = index
        sessionManager.saveSelectedDnsIndex(index)
        refreshDnsSelectionUi()

        if (DnsVpnService.isRunning) {
            startSelectedDnsVpn()
        }
    }

    private fun refreshDnsSelectionUi() {
        val current = AppConfig.DNS_OPTIONS[selectedDnsIndex]
        tvSelectedDnsBadge.text = current.badgeText
        tvSelectedDnsName.text = current.name
        tvSelectedDnsHost.text = "Host: ${current.dnsHost}  •  ${current.latencyLabel}"

        tvStatDnsName.text = current.name
        tvStatDnsHost.text = current.dnsHost
        tvStatPing.text = "Ping ${current.latencyLabel}"

        radioDns1.setBackgroundResource(
            if (selectedDnsIndex == 0) R.drawable.bg_radio_selected else R.drawable.bg_radio_unselected
        )
        radioDns2.setBackgroundResource(
            if (selectedDnsIndex == 1) R.drawable.bg_radio_selected else R.drawable.bg_radio_unselected
        )
        radioDns3.setBackgroundResource(
            if (selectedDnsIndex == 2) R.drawable.bg_radio_selected else R.drawable.bg_radio_unselected
        )
    }

    private fun prepareAndStartVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startSelectedDnsVpn()
        }
    }

    private fun startSelectedDnsVpn() {
        val dns = AppConfig.DNS_OPTIONS[selectedDnsIndex]
        val intent = Intent(this, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_CONNECT
            putExtra(DnsVpnService.EXTRA_DNS_HOST, dns.dnsHost)
            putExtra(DnsVpnService.EXTRA_DNS_NAME, dns.name)
        }
        startService(intent)
        btnPower.postDelayed({ refreshConnectionUi() }, 300)
    }

    private fun stopDnsVpn() {
        val intent = Intent(this, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_DISCONNECT
        }
        startService(intent)
        btnPower.postDelayed({ refreshConnectionUi() }, 200)
    }

    private fun refreshConnectionUi() {
        if (DnsVpnService.isRunning) {
            tvConnectionBadge.text = "CONNECTED"
            tvConnectionBadge.setBackgroundResource(R.drawable.bg_badge_connected)
            btnPower.setBackgroundResource(R.drawable.bg_power_orb_connected)
        } else {
            tvConnectionBadge.text = "DISCONNECTED"
            tvConnectionBadge.setBackgroundResource(R.drawable.bg_badge_disconnected)
            btnPower.setBackgroundResource(R.drawable.bg_power_orb_idle)
            tvTimer.text = "00:00:00"
        }
    }

    private fun updateTimerDisplay() {
        if (!DnsVpnService.isRunning || DnsVpnService.connectedSinceMillis == 0L) {
            tvTimer.text = "00:00:00"
            return
        }
        val elapsedSec = (System.currentTimeMillis() - DnsVpnService.connectedSinceMillis) / 1000L
        val hours = elapsedSec / 3600
        val minutes = (elapsedSec % 3600) / 60
        val seconds = elapsedSec % 60
        tvTimer.text = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onResume() {
        super.onResume()
        refreshConnectionUi()
        timerHandler.post(timerRunnable)
    }

    override fun onPause() {
        super.onPause()
        timerHandler.removeCallbacks(timerRunnable)
    }
}
