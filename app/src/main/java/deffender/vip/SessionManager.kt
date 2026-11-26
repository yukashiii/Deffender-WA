package deffender.vip

import android.content.Context
import android.provider.Settings

class SessionManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("deffender_vip_prefs", Context.MODE_PRIVATE)

    fun saveSession(token: String, ownerName: String, expiresAt: String) {
        prefs.edit()
            .putString("vip_token", token)
            .putString("owner_name", ownerName)
            .putString("expires_at", expiresAt)
            .apply()
    }

    fun getSavedToken(): String? = prefs.getString("vip_token", null)

    fun getOwnerName(): String = prefs.getString("owner_name", "VIP Member") ?: "VIP Member"

    fun getExpiresAt(): String = prefs.getString("expires_at", "-") ?: "-"

    fun clearSession() {
        prefs.edit().remove("vip_token").remove("owner_name").remove("expires_at").apply()
    }

    fun saveSelectedDnsIndex(index: Int) {
        prefs.edit().putInt("selected_dns_index", index).apply()
    }

    fun getSelectedDnsIndex(): Int = prefs.getInt("selected_dns_index", 0)

    fun getBuyKeyUrl(): String {
        val custom = prefs.getString("custom_buy_key_url", null)
        return if (!custom.isNullOrBlank()) custom else AppConfig.BUY_KEY_URL
    }

    fun saveCustomBuyKeyUrl(url: String) {
        prefs.edit().putString("custom_buy_key_url", url.trim()).apply()
    }

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-device"
    }
}
