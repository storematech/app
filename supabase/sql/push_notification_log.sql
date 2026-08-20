-- supabase/sql/push_notification_log.sql
--
-- Backs the `scheduled-push` edge function's lifecycle push-notification campaign (signup +30min/
-- +8h, daily Day 1-10 nudges, trial-expiry warnings). One row per (user, notification_key) sent —
-- the unique constraint is what makes every cron run idempotent: re-running (or overlapping runs)
-- can never double-send the same notification to the same user.
--
-- Run this once in the Supabase SQL Editor. Safe to re-run (create table if not exists).

create table if not exists push_notification_log (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  notification_key text not null,
  sent_at timestamptz not null default now(),
  unique (user_id, notification_key)
);

create index if not exists push_notification_log_user_id_idx on push_notification_log (user_id);

-- No policies on purpose — only ever read/written by scheduled-push via the service-role key,
-- same reasoning as quiz_ai_summaries/class_ai_summaries.
alter table push_notification_log enable row level security;
