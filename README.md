# Yukashi VPN

Android Kotlin starter project, package `yukashi.blast`.

## Included
- Dark purple Compose UI
- VIP login calling Supabase Edge Function `validate-vip`
- Buy Key WhatsApp link
- Three hidden DNS-over-TLS (DoT) hostname choices
- Android VpnService DNS-only TUN scaffold
- GitHub Actions debug APK workflow

## Important VPN implementation status
This starter currently establishes a DNS-only TUN interface but does **not** include the DNS packet forwarding engine. Therefore it is not yet a functional DNS VPN and must not be distributed as a working VPN until the packet engine is implemented and tested. The service comments mark this explicitly. Do not treat the VPN notification as proof that DNS is protected.

## Supabase setup
Expected `vip_tokens` columns: `token` (text), `is_active` (boolean), `expires_at` (timestamptz, nullable). Run `supabase/schema.sql`.

Deploy Edge Function:
1. `supabase functions deploy validate-vip`
2. `supabase secrets set SUPABASE_SERVICE_ROLE_KEY=YOUR_SERVICE_ROLE_KEY`
3. Ensure `SUPABASE_URL` is available to the function runtime.

The Android app contains only the Supabase anon key. The service-role key stays server-side.

## Build APK
Push this folder to a GitHub repository. Open Actions > Build Yukashi APK > Run workflow. Download artifact `Yukashi-debug-apk`.

Build locally with JDK 17 and Gradle 8.7:
`gradle assembleDebug`

## DNS choices (DNS-over-TLS / DoT)
1. Cloudflare: `one.one.one.one`
2. Google: `dns.google`
3. Quad9: `dns.quad9.net`

The UI does not show endpoint hostnames. These are DNS-over-TLS hostnames suitable for port 853. They are stored as internal configuration values. The current TUN scaffold does not yet establish the TLS connection or forward DNS packets; implement and test that packet-forwarding engine before treating the app as a working VPN.
