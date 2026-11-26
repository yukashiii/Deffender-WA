package yukashi.blast

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import yukashi.blast.data.DnsProfiles
import yukashi.blast.data.SupabaseKeyValidator
import kotlinx.coroutines.launch

private val Ink = Color(0xFF10091D)
private val Panel = Color(0xFF211333)
private val Violet = Color(0xFF9B35FF)
private val Pink = Color(0xFFE05BFF)
private val Muted = Color(0xFFB9A9CC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Violet, secondary = Pink, background = Ink, surface = Panel
            )) {
                YukashiApp(
                    onBuyKey = { openUrl("https://wa.me/6285175215023") },
                    onPrivateDns = {
                        try {
                            startActivity(Intent(Settings.ACTION_PRIVATE_DNS_SETTINGS))
                        } catch (_: Exception) {
                            startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                )
            }
        }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
private fun YukashiApp(onBuyKey: () -> Unit, onPrivateDns: () -> Unit) {
    val scope = rememberCoroutineScope()
    val validator = remember {
        SupabaseKeyValidator(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY)
    }
    var token by remember { mutableStateOf("") }
    var loggedIn by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf(DnsProfiles.all.first().id) }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF30104C), Ink, Color(0xFF090511)))
        ).padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!loggedIn) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("YUKASHI", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Text("PRIVATE DNS PROTECTION", color = Pink, letterSpacing = 2.sp, fontSize = 11.sp)
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Welcome back", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text("Masukkan key akses untuk melanjutkan.", color = Muted)
                        OutlinedTextField(
                            value = token, onValueChange = { token = it; error = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("API Key / Token") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions.Default,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = Pink, unfocusedBorderColor = Muted,
                                focusedLabelColor = Pink, unfocusedLabelColor = Muted
                            )
                        )
                        error?.let { Text(it, color = Color(0xFFFF8B9A), fontSize = 12.sp) }
                        Button(
                            onClick = {
                                if (token.isBlank()) { error = "Masukkan API key terlebih dahulu."; return@Button }
                                loading = true
                                scope.launch {
                                    val result = validator.validate(token.trim())
                                    loading = false
                                    result.onSuccess { valid ->
                                        if (valid) loggedIn = true else error = "API key tidak valid atau kedaluwarsa."
                                    }.onFailure { error = it.message ?: "Gagal menghubungi server." }
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text(if (loading) "MEMERIKSA..." else "LOGIN") }
                        OutlinedButton(onClick = onBuyKey, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                            Text("BUY KEY VIA WHATSAPP")
                        }
                    }
                }
                Text("Yukashi • Secure connection experience", color = Muted, fontSize = 11.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("YUKASHI", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text("PRIVATE DNS", color = Pink, letterSpacing = 2.sp, fontSize = 11.sp)
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Pilih proteksi DNS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Alamat server tidak ditampilkan di aplikasi.", color = Muted, fontSize = 12.sp)
                        DnsProfiles.all.forEachIndexed { index, profile ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(if (selected == profile.id) Color(0xFF49216C) else Color(0xFF2B1B3B), RoundedCornerShape(15.dp))
                                    .clickable { selected = profile.id }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(profile.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("Encrypted DNS profile ${index + 1}", color = Muted, fontSize = 11.sp)
                                }
                                RadioButton(selected == profile.id, onClick = { selected = profile.id })
                            }
                        }
                        Button(onClick = onPrivateDns, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                            Text("BUKA PENGATURAN PRIVATE DNS")
                        }
                        Text(
                            "Android meminta konfirmasi sistem untuk mengubah Private DNS. Pilihan di atas belum otomatis diterapkan.",
                            color = Muted, fontSize = 11.sp
                        )
                        TextButton(onClick = { loggedIn = false; token = "" }) { Text("LOGOUT") }
                    }
                }
            }
        }
    }
}
