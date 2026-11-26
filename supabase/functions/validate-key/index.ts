import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

async function sha256(value: string): Promise<string> {
  const bytes = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ valid: false }), { status: 405, headers: { ...cors, "Content-Type": "application/json" } });
  }

  try {
    const { token } = await req.json();
    if (typeof token !== "string" || token.length < 16 || token.length > 256) {
      return new Response(JSON.stringify({ valid: false }), { status: 200, headers: { ...cors, "Content-Type": "application/json" } });
    }

    const url = Deno.env.get("SUPABASE_URL")!;
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
    const supabase = createClient(url, serviceKey, { auth: { persistSession: false } });
    const tokenHash = await sha256(token);
    const { data, error } = await supabase
      .from("api_keys")
      .select("id, is_active, expires_at")
      .eq("token_hash", tokenHash)
      .maybeSingle();

    if (error) throw error;
    const notExpired = !data?.expires_at || new Date(data.expires_at).getTime() > Date.now();
    const valid = Boolean(data?.is_active && notExpired);

    return new Response(JSON.stringify({ valid }), {
      status: 200, headers: { ...cors, "Content-Type": "application/json" },
    });
  } catch (_error) {
    return new Response(JSON.stringify({ valid: false }), {
      status: 200, headers: { ...cors, "Content-Type": "application/json" },
    });
  }
});
