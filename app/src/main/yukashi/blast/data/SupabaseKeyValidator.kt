package yukashi.blast.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SupabaseKeyValidator(
    private val projectUrl: String,
    private val anonKey: String
) {
    suspend fun validate(token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            require(projectUrl.startsWith("https://")) { "Supabase belum dikonfigurasi." }
            require(anonKey.isNotBlank()) { "Supabase anon key belum dikonfigurasi." }
            val endpoint = projectUrl.trimEnd('/') + "/functions/v1/validate-key"
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 12000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $anonKey")
            }
            try {
                connection.outputStream.use {
                    it.write(JSONObject().put("token", token).toString().toByteArray())
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) error("Token tidak valid atau server tidak tersedia.")
                JSONObject(body).optBoolean("valid", false)
            } finally {
                connection.disconnect()
            }
        }
    }
}
