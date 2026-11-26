package deffender.vip

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant

data class TokenValidationResult(
    val isValid: Boolean,
    val message: String,
    val ownerName: String = "VIP User",
    val expiresAt: String = "Lifetime"
)

object SupabaseTokenManager {

    suspend fun verifyToken(rawToken: String, deviceId: String): TokenValidationResult =
        withContext(Dispatchers.IO) {
            val token = rawToken.trim()
            if (token.isEmpty()) {
                return@withContext TokenValidationResult(false, "Masukkan API Token terlebih dahulu.")
            }

            if (AppConfig.SUPABASE_URL.contains("YOUR_PROJECT_ID") ||
                AppConfig.SUPABASE_ANON_KEY.contains("YOUR_SUPABASE_ANON_KEY")
            ) {
                return@withContext TokenValidationResult(
                    false,
                    "Konfigurasi Supabase belum diisi di AppConfig.kt"
                )
            }

            try {
                val encodedToken = URLEncoder.encode(token, "UTF-8")
                val endpoint =
                    "${AppConfig.SUPABASE_URL}/rest/v1/${AppConfig.SUPABASE_TABLE}?token_key=eq.$encodedToken&select=*"

                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("apikey", AppConfig.SUPABASE_ANON_KEY)
                    setRequestProperty("Authorization", "Bearer ${AppConfig.SUPABASE_ANON_KEY}")
                    setRequestProperty("Accept", "application/json")
                }

                val responseCode = conn.responseCode
                if (responseCode !in 200..299) {
                    return@withContext TokenValidationResult(
                        false,
                        "Gagal terhubung ke Supabase (HTTP $responseCode)"
                    )
                }

                val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val jsonArray = JSONArray(body)

                if (jsonArray.length() == 0) {
                    return@withContext TokenValidationResult(false, "API Token tidak ditemukan.")
                }

                val record = jsonArray.getJSONObject(0)
                val isActive = record.optBoolean("is_active", false)
                if (!isActive) {
                    return@withContext TokenValidationResult(false, "API Token telah dinonaktifkan.")
                }

                val dbDeviceId = record.optString("device_id", "")
                if (dbDeviceId.isNotEmpty() && dbDeviceId != "null" && dbDeviceId != deviceId) {
                    return@withContext TokenValidationResult(
                        false,
                        "Token sudah terikat pada perangkat lain."
                    )
                }

                val expiresAt = record.optString("expires_at", "")
                if (expiresAt.isNotEmpty() && expiresAt != "null") {
                    try {
                        val expiryInstant = Instant.parse(expiresAt)
                        if (Instant.now().isAfter(expiryInstant)) {
                            return@withContext TokenValidationResult(
                                false,
                                "Masa aktif API Token sudah habis."
                            )
                        }
                    } catch (_: Exception) {
                    }
                }

                if (dbDeviceId.isEmpty() || dbDeviceId == "null") {
                    bindDeviceToToken(encodedToken, deviceId)
                }

                val ownerName = record.optString("owner_name", "VIP Member")
                val cleanExpiry = if (expiresAt.isEmpty() || expiresAt == "null") {
                    "Aktif Permanen"
                } else {
                    expiresAt.take(10)
                }

                TokenValidationResult(
                    isValid = true,
                    message = "Token VIP terverifikasi!",
                    ownerName = ownerName,
                    expiresAt = cleanExpiry
                )
            } catch (e: Exception) {
                TokenValidationResult(false, "Koneksi error: ${e.localizedMessage}")
            }
        }

    private fun bindDeviceToToken(encodedToken: String, deviceId: String) {
        try {
            val endpoint =
                "${AppConfig.SUPABASE_URL}/rest/v1/${AppConfig.SUPABASE_TABLE}?token_key=eq.$encodedToken"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                connectTimeout = 8000
                readTimeout = 8000
                doOutput = true
                setRequestProperty("apikey", AppConfig.SUPABASE_ANON_KEY)
                setRequestProperty("Authorization", "Bearer ${AppConfig.SUPABASE_ANON_KEY}")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Prefer", "return=minimal")
            }

            val payload = JSONObject().apply {
                put("device_id", deviceId)
            }

            OutputStreamWriter(conn.outputStream).use {
                it.write(payload.toString())
                it.flush()
            }
            conn.responseCode
        } catch (_: Exception) {
        }
    }
}
