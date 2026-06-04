// Idempotency guard for webhook-triggered Edge Functions. Backed by the
// webhook_dedup table (see supabase/feature-webhook-dedup.sql).
//
// Usage at the top of an Edge Function handler:
//
//   const already = await isAlreadyProcessed(supabase, `notify-event:${id}`);
//   if (already) return ackResponse("already-processed");
//
// Returns:
//   - true  → this dedup_key was already processed; caller should NOT re-run
//   - false → we just inserted the key; caller should proceed
//
// Defensive behavior: if the dedup table doesn't exist OR the insert fails
// for any non-unique-violation reason, we return false and log — better to
// risk a double-fire than to drop a legitimate notification.

import { SupabaseClient } from "https://esm.sh/@supabase/supabase-js@2";

const UNIQUE_VIOLATION = "23505";

export async function isAlreadyProcessed(
  supabase: SupabaseClient,
  dedupKey: string,
): Promise<boolean> {
  const { error } = await supabase
    .from("webhook_dedup")
    .insert({ dedup_key: dedupKey });

  if (!error) return false;                 // first time → proceed
  if (error.code === UNIQUE_VIOLATION) {    // already exists → skip
    return true;
  }
  // Any other DB error (network blip, table missing, etc.) — log and let the
  // caller proceed. False positives (re-sending) are recoverable; false
  // negatives (silent drops) are not.
  console.warn(
    `dedup: insert failed for "${dedupKey}" (${error.code}): ${error.message} — ` +
    `proceeding without guard`,
  );
  return false;
}

/** Convenience HTTP 200 acknowledgment when we skip a duplicate. */
export function ackSkipped(reason: string): Response {
  return new Response(
    JSON.stringify({ skipped: reason }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
}
