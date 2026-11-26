create table if not exists public.api_keys (
  id uuid primary key default gen_random_uuid(),
  token_hash text not null unique,
  is_active boolean not null default true,
  expires_at timestamptz,
  created_at timestamptz not null default now()
);

alter table public.api_keys enable row level security;
-- No client-facing policies: validation is performed only by the Edge Function
-- using the service-role key stored as a Supabase function secret.
