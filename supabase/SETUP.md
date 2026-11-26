# Supabase setup for Yukashi

Project URL and the public anon/publishable key are configured as GitHub Actions secrets.
Do not add the service-role key to Android, GitHub source files, or the APK.

## 1. Create the table
Run `schema.sql` in Supabase SQL Editor.

## 2. Deploy the Edge Function
Install/link the Supabase CLI, then run:

```bash
supabase secrets set SUPABASE_URL="https://YOUR_PROJECT.supabase.co" SUPABASE_SERVICE_ROLE_KEY="YOUR_SERVICE_ROLE_KEY"
supabase functions deploy validate-key
```

The service-role key is stored only as an Edge Function secret. Never send it to the app.

## 3. Add GitHub Actions secrets
In repository Settings → Secrets and variables → Actions, add:

- `SUPABASE_URL`: your Supabase project URL
- `SUPABASE_ANON_KEY`: your public anon/publishable key

The workflow passes these values to Gradle at build time. They are public-client configuration, not privileged secrets. Keep database RLS enabled and do not create client policies exposing `api_keys`.

## 4. Issue an API key
Run this SQL in the SQL Editor. It creates a random 256-bit token, stores only its SHA-256 hash, and returns the plaintext token once. Copy the returned token and give it to the customer securely.

```sql
create extension if not exists pgcrypto;

with generated as (
  select encode(gen_random_bytes(32), 'hex') as token
),
inserted as (
  insert into public.api_keys (token_hash)
  select encode(digest(token, 'sha256'), 'hex')
  from generated
  returning id
)
select inserted.id, generated.token
from generated cross join inserted;
```

To revoke a key:
```sql
update public.api_keys set is_active = false where id = 'KEY_UUID';
```

To set an expiry:
```sql
update public.api_keys
set expires_at = now() + interval '30 days'
where id = 'KEY_UUID';
```

## 5. DNS note
Yukashi stores these three DNS-over-TLS hostnames internally:
- `dns.adguard.com`
- `dns.adguard-dns.com`
- `one.one.one.one`

The UI displays friendly labels only. Android does not allow a normal app to silently change system Private DNS; the user must confirm the provider in Android settings. This project is a Private DNS companion, not a full-tunnel VPN.
