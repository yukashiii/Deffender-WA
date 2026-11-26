# deffender VIP (`deffender.vip`)

Aplikasi Android VPN Private DNS berbasis **Hostname** dengan autentikasi API Token **Supabase**, menu **Buy Key** yang dapat dikonfigurasi, dan build otomatis via **GitHub Actions**.

## Pembaruan Versi 2
1. **DNS Berbasis Hostname (Bukan Angka IP)**:
   - `one.one.one.one` (Cloudflare Private DNS)
   - `dns.adguard-dns.com` (AdGuard Private DNS)
   - `dns.quad9.net` (Quad9 Security DNS)
2. **Menu Buy Key yang Dapat Dikonfigurasi**:
   - Tersedia di layar login token (`BUY KEY / BELI TOKEN VIP`), tombol cepat di menu utama (`BUY KEY`), serta tab khusus `Buy Key` di navigasi bawah.
   - Link website default dapat diatur di `AppConfig.kt` (`BUY_KEY_URL`) maupun diubah langsung dari dalam aplikasi.
3. **Ikon Aplikasi Kustom**:
   - Terpasang otomatis di seluruh resolusi `mipmap-*` (`ic_launcher` & `ic_launcher_round`) serta `drawable-nodpi/ic_app_logo.png`.
