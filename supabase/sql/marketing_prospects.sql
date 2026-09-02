-- supabase/sql/marketing_prospects.sql
--
-- Cold-outreach prospect list for the marketing email campaign (see
-- supabase/functions/scheduled-marketing-email). One row per prospect organization; the scheduled
-- function picks the single oldest 'pending' row per run (hourly cron), sends it via send-email's
-- "marketing_proposal" emailType, and flips it to 'sent'/'failed' so the same prospect is never
-- emailed twice and failures stay visible here for manual follow-up.
--
-- No RLS policies are added on purpose — only the service role (used by the edge function) and
-- whoever has Supabase SQL Editor access should ever read/write this table; it holds prospects'
-- contact info, not anything the app's anon/authenticated API keys should ever expose.
--
-- Run this once in the Supabase SQL Editor. Safe to re-run (create-if-not-exists).

create table if not exists public.marketing_prospects (
  id uuid primary key default gen_random_uuid(),
  -- Organization/contact name — substituted into the email as "{{1}}" (subject + greeting).
  name text not null,
  email text not null unique,
  status text not null default 'pending' check (status in ('pending', 'sent', 'failed')),
  sent_at timestamptz,
  -- Set when status = 'failed' so a bounce/API error is visible without checking function logs.
  -- Reset a row back to status = 'pending' (and clear this) to retry it on the next hourly run.
  error_message text,
  created_at timestamptz not null default now()
);

-- Matches the scheduled function's "oldest pending row" query exactly.
create index if not exists marketing_prospects_status_created_at_idx
  on public.marketing_prospects (status, created_at);

alter table public.marketing_prospects enable row level security;
