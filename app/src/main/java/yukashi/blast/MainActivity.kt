package yukashi.blast

import android.content.Intent
import android.net.VpnService
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private val Bg = Color(0xFF0D0F18)
private val Panel = Color(0xFF191C2B)
private val Purple = Color(0xFF9B7BFF)

class MainActivity : ComponentActivity() {
    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) startVpn()
    }
    private var loggedIn by mutableStateOf(false)
    private var selectedDns by mutableIntStateOf(0)
    private var message by mutableStateOf("")
    private var busy by mutableStateOf(false)
    private val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var token by remember { mutableStateOf("") }
            var connected by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Bg, surface = Panel)) {
                Surface(Modifier.fillMaxSize(), color = Bg) {
                    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(28.dp))
                        Text("YUKASHI", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("PRIVATE DNS VPN", color = Purple, letterSpacing = 3.sp, fontSize = 11.sp)
                        Spacer(Modifier.height(32.dp))
                        if (!loggedIn) {
                            Text("Welcome back", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text("Masukkan VIP key untuk melanjutkan", color = Color.LightGray)
                            Spacer(Modifier.height(22.dp))
                            OutlinedTextField(
                                value = token, onValueChange = { token = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("VIP API Token") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = {
                                if (token.isBlank()) { message = "Masukkan VIP key terlebih dahulu"; return@Button }
                                scope.launch {
                                    busy = true; message = "Memeriksa key..."
                                    val result = withContext(Dispatchers.IO) { validateToken(token) }
                                    busy = false
                                    if (result.first) { loggedIn = true; getSharedPreferences("yukashi", MODE_PRIVATE).edit().putString("vip_token", token).apply(); message = "Login berhasil" }
                                    else message = result.second
                                }
                            }, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = !busy) {
                                Text(if (busy) "MEMERIKSA..." else "LOGIN")
                            }
                            TextButton(onClick = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.BUY_KEY_URL))) }) {
                                Text("BUY KEY  ↗", color = Purple, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("Pilih DNS Server", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("Pilih salah satu server privat", color = Color.LightGray)
                            Spacer(Modifier.height(18.dp))
                            listOf("DNS OPTION 01", "DNS OPTION 02", "DNS OPTION 03").forEachIndexed { index, title ->
                                Card(
                                    onClick = { selectedDns = index },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (selectedDns == index) Color(0xFF30264F) else Panel),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        RadioButton(selected = selectedDns == index, onClick = { selectedDns = index })
                                    }
                                }
                            }
                            Spacer(Modifier.height(28.dp))
                            Button(
                                onClick = {
                                    if (connected) { stopService(Intent(this@MainActivity, DnsVpnService::class.java)); connected = false }
                                    else {
                                        val intent = VpnService.prepare(this@MainActivity)
                                        if (intent != null) vpnPermission.launch(intent) else { startVpn(); connected = true }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(58.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (connected) Color(0xFFB94C62) else Purple)
                            ) { Text(if (connected) "DISCONNECT VPN" else "CONNECT VPN", fontWeight = FontWeight.Bold) }
                            TextButton(onClick = { stopService(Intent(this@MainActivity, DnsVpnService::class.java)); connected = false; loggedIn = false }) {
                                Text("LOG OUT", color = Color.LightGray)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text(message, color = if (message.contains("berhasil")) Color(0xFF72D6A0) else Color(0xFFFFB4AB))
                        Text("Yukashi • Secure private connection", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    private fun startVpn() {
        startForegroundService(Intent(this, DnsVpnService::class.java).putExtra("dns_index", selectedDns))
    }

    private fun validateToken(token: String): Pair<Boolean, String> {
        return try {
            val body = JSONObject().put("token", token).toString()
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("${BuildConfig.SUPABASE_URL}/functions/v1/validate-vip")
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .post(body).build()
            client.newCall(req).execute().use { response ->
                val json = JSONObject(response.body?.string() ?: "{}")
                if (response.isSuccessful && json.optBoolean("valid")) true to "VIP key valid"
                else false to (json.optString("error").ifBlank { "VIP key tidak valid atau server belum dikonfigurasi" })
            }
        } catch (e: Exception) { false to "Gagal menghubungi server: periksa koneksi internet" }
    }
}
