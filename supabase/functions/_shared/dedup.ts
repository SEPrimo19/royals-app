
import { SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2";

const UNIQUE_VIOLATION = "23505";

export async function isAlreadyProcessed(
  supabase: SupabaseClient,
  dedupKey: string,
): Promise<boolean> {
  const { error } = await supabase
    .from("webhook_dedup")
    .insert({ dedup_key: dedupKey });

  if (!error) return false;
  if (error.code === UNIQUE_VIOLATION) {
    return true;
  }
  console.warn(
    `dedup: insert failed for "${dedupKey}" (${error.code}): ${error.message} — ` +
    `proceeding without guard`,
  );
  return false;
}

export function ackSkipped(reason: string): Response {
  return new Response(
    JSON.stringify({ skipped: reason }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
}
