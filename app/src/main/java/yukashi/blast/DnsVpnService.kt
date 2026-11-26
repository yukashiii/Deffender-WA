package yukashi.blast

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.util.concurrent.Executors

/**
 * DNS-only VPN tunnel scaffold.
 * The selected DoH endpoint is intentionally not displayed in the UI.
 * This baseline establishes a DNS-only TUN route; packet forwarding is not
 * enabled until the production DNS packet engine is integrated and tested.
 */
class DnsVpnService : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private val executor = Executors.newSingleThreadExecutor()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(71, notification())
        val dnsIndex = intent?.getIntExtra("dns_index", 0) ?: 0
        tun?.close()
        tun = Builder()
            .setSession("Yukashi Private DNS")
            .setMtu(1500)
            .addAddress("10.111.0.1", 32)
            .addDnsServer("10.111.0.2")
            .addRoute("10.111.0.2", 32)
            .establish()
        // Prevent a false claim of a working tunnel: packet engine must be added.
        // DNS-over-TLS (DoT) hostnames; intentionally never displayed in the UI.
        // These are configuration values only until the DoT packet-forwarding
        // engine is integrated. The current TUN scaffold does not yet forward DNS.
        val selectedEndpoint = listOf(
            "one.one.one.one",       // Cloudflare DoT
            "dns.google",            // Google Public DNS DoT
            "dns.quad9.net"          // Quad9 DoT
        ).getOrElse(dnsIndex) { "one.one.one.one" }
        getSharedPreferences("yukashi_runtime", MODE_PRIVATE).edit().putString("doh_endpoint", selectedEndpoint).apply()
        return START_STICKY
    }
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("yukashi_vpn", "Yukashi VPN", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    private fun notification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "yukashi_vpn") else Notification.Builder(this)
        return builder.setContentTitle("Yukashi").setContentText("VPN service berjalan").setSmallIcon(android.R.drawable.ic_lock_lock).build()
    }
    override fun onDestroy() { tun?.close(); tun = null; executor.shutdownNow(); super.onDestroy() }
}
