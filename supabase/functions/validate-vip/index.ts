// Deploy with: supabase functions deploy validate-vip
// Set secret: supabase secrets set SUPABASE_SERVICE_ROLE_KEY=...
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response(JSON.stringify({ error: "Method not allowed" }), { status: 405 });
  try {
    const { token } = await req.json();
    if (typeof token !== "string" || token.length < 8 || token.length > 256)
      return Response.json({ valid: false, error: "VIP key tidak valid" }, { status: 400 });
    const url = Deno.env.get("SUPABASE_URL")!;
    const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!serviceKey) return Response.json({ valid: false, error: "Backend belum dikonfigurasi" }, { status: 500 });
    const db = createClient(url, serviceKey);
    // Expected columns: token, is_active, expires_at
    const { data, error } = await db.from("vip_tokens")
      .select("is_active,expires_at")
      .eq("token", token)
      .maybeSingle();
    if (error) throw error;
    const active = !!data?.is_active;
    const expiryOk = !data?.expires_at || new Date(data.expires_at).getTime() > Date.now();
    return Response.json({ valid: active && expiryOk, error: active && expiryOk ? "" : "VIP key tidak aktif atau kedaluwarsa" });
  } catch {
    return Response.json({ valid: false, error: "Terjadi kesalahan validasi" }, { status: 500 });
  }
});
