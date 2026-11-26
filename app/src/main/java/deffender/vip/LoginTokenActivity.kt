package deffender.vip

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class LoginTokenActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var etToken: EditText
    private lateinit var btnActivate: Button
    private lateinit var btnBuyKeyLogin: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private val uiScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_token)

        sessionManager = SessionManager(this)
        etToken = findViewById(R.id.etApiToken)
        btnActivate = findViewById(R.id.btnActivateToken)
        btnBuyKeyLogin = findViewById(R.id.btnBuyKeyLogin)
        tvStatus = findViewById(R.id.tvLoginStatus)
        progressBar = findViewById(R.id.progressLogin)

        val savedToken = sessionManager.getSavedToken()
        if (!savedToken.isNullOrEmpty()) {
            etToken.setText(savedToken)
            validateAndProceed(savedToken, isAutoCheck = true)
        }

        btnActivate.setOnClickListener {
            validateAndProceed(etToken.text.toString(), isAutoCheck = false)
        }

        btnBuyKeyLogin.setOnClickListener {
            openBuyKeyWebsite()
        }
    }

    private fun openBuyKeyWebsite() {
        val url = sessionManager.getBuyKeyUrl()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Link Buy Key tidak valid: $url", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateAndProceed(token: String, isAutoCheck: Boolean) {
        setLoading(true)
        tvStatus.text = if (isAutoCheck) {
            "Memverifikasi sesi token VIP..."
        } else {
            "Menghubungkan ke server Supabase..."
        }

        uiScope.launch {
            val result = SupabaseTokenManager.verifyToken(token, sessionManager.getDeviceId())
            setLoading(false)

            if (result.isValid) {
                sessionManager.saveSession(token.trim(), result.ownerName, result.expiresAt)
                startActivity(Intent(this@LoginTokenActivity, MainActivity::class.java))
                finish()
            } else {
                if (isAutoCheck) {
                    sessionManager.clearSession()
                }
                tvStatus.text = result.message
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnActivate.isEnabled = !loading
        etToken.isEnabled = !loading
    }
}
