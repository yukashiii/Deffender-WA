package deffender.vip

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import java.net.Inet4Address
import java.net.InetAddress

class DnsVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "deffender.vip.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "deffender.vip.ACTION_DISCONNECT"
        const val EXTRA_DNS_HOST = "extra_dns_host"
        const val EXTRA_DNS_NAME = "extra_dns_name"

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var connectedSinceMillis: Long = 0L
            private set
    }

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                stopVpn()
                return START_NOT_STICKY
            }
            ACTION_CONNECT -> {
                val dnsHost = intent.getStringExtra(EXTRA_DNS_HOST) ?: "one.one.one.one"
                val dnsName = intent.getStringExtra(EXTRA_DNS_NAME) ?: "Cloudflare Private DNS"
                Thread {
                    startVpnWithHost(dnsHost, dnsName)
                }.start()
            }
        }
        return START_STICKY
    }

    private fun startVpnWithHost(dnsHost: String, dnsName: String) {
        try {
            val resolvedAddresses = InetAddress.getAllByName(dnsHost)
                .filterIsInstance<Inet4Address>()
                .mapNotNull { it.hostAddress }
                .distinct()

            val targetDnsIps = if (resolvedAddresses.isNotEmpty()) {
                resolvedAddresses.take(2)
            } else {
                listOf("1.1.1.1", "1.0.0.1")
            }

            vpnInterface?.close()

            val builder = Builder()
                .setSession("deffender VIP - $dnsHost")
                .setMtu(1500)
                .addAddress("10.220.0.2", 32)

            for (ip in targetDnsIps) {
                builder.addDnsServer(ip)
                builder.addRoute(ip, 32)
            }

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                isRunning = true
                if (connectedSinceMillis == 0L) {
                    connectedSinceMillis = System.currentTimeMillis()
                }
                startForeground(101, createNotification(dnsName, dnsHost))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopVpn()
        }
    }

    private fun stopVpn() {
        isRunning = false
        connectedSinceMillis = 0L
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(dnsName: String, dnsHost: String): Notification {
        val channelId = "deffender_vip_vpn_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "deffender VIP Protection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("deffender VIP Aktif")
            .setContentText("Host DNS: $dnsHost ($dnsName)")
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
