# Yukashi

Android app starter for Yukashi (`yukashi.blast`).

## What this starter includes
- Purple/dark Jetpack Compose interface.
- API-key login backed by a Supabase Edge Function.
- Login gate before the DNS profile screen.
- Three selectable DNS profiles (hostnames are kept in app configuration and are not rendered in the UI).
- Buy Key button opens `https://wa.me/6285175215023`.
- GitHub Actions workflow to build a debug APK.

## Important Android limitation
This starter is a **Private DNS companion**, not a full-tunnel VPN. Ordinary Android apps cannot silently change the device-wide Private DNS provider. The Activate button opens Android's Private DNS settings. The user must configure/confirm the provider there. The app does not claim that DNS is active merely because the settings screen opened.

The DNS hostname list is in `app/src/main/java/yukashi/blast/data/DnsProfiles.kt`.
The first value was supplied as `dns.aguard.com`; verify whether this is a typo before release. It has intentionally not been silently changed.

## Configure Supabase
1. Create a Supabase project.
2. Run `supabase/schema.sql` in the SQL Editor.
3. Deploy the Edge Function in `supabase/functions/validate-key`.
4. Set function secrets `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY`.
5. In GitHub repository settings, add Actions secrets:
   - `SUPABASE_URL`
   - `SUPABASE_ANON_KEY`
6. Push this project to GitHub. The workflow builds an APK on pushes and can be run manually.

Never put the service-role key in the Android app. Only the public anon key belongs in the client.

## Build locally
Open this folder in Android Studio (JDK 17), sync Gradle, then run the `app` configuration. The GitHub workflow installs Gradle directly; a Gradle wrapper is not included in this starter.
For a local build, provide Gradle properties or environment variables for `SUPABASE_URL` and `SUPABASE_ANON_KEY`. The app uses safe placeholder defaults when they are absent, so login will not work until configured.

## Create API keys
Store only SHA-256 hashes of issued keys in `public.api_keys`. Generate a random high-entropy key outside the app, hash it with SHA-256, and insert the hash into the table. Do not store raw customer keys.


## Deploy Supabase Edge Function
With the Supabase CLI installed and linked to your project:
```bash
supabase secrets set SUPABASE_URL="https://YOUR_PROJECT.supabase.co" SUPABASE_SERVICE_ROLE_KEY="YOUR_SERVICE_ROLE_KEY"
supabase functions deploy validate-key
```
Do not commit either secret to GitHub or the Android source.

For detailed Supabase deployment, GitHub secret setup, and API-key issuance, see [`supabase/SETUP.md`](supabase/SETUP.md).
