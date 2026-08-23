-- supabase/sql/ai_generation_log.sql
--
-- Backs generate-quiz-ai's per-user daily rate limit (10/day free, 25/day premium — see that
-- function's AI_DAILY_LIMIT_FREE/AI_DAILY_LIMIT_PREMIUM). One row per AI generation attempt that
-- passed the rate check (whether the AI call itself then succeeded or failed) — the count of rows
-- in the trailing 24h is what the limit is checked against. Same "RLS on, no policies, service-role
-- only" shape as push_notification_log.sql.
--
-- Run this once in the Supabase SQL Editor. Safe to re-run (create table if not exists).

create table if not exists ai_generation_log (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);

create index if not exists ai_generation_log_user_id_created_at_idx on ai_generation_log (user_id, created_at);

-- No policies on purpose — only ever read/written by generate-quiz-ai via the service-role key,
-- same reasoning as push_notification_log/quiz_ai_summaries.
alter table ai_generation_log enable row level security;
