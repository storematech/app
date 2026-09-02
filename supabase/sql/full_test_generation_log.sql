-- supabase/sql/full_test_generation_log.sql
--
-- Backs generate-full-test's per-user daily rate limit (1/day free, 3/day premium — see that
-- function's FULL_TEST_DAILY_LIMIT_FREE/FULL_TEST_DAILY_LIMIT_PREMIUM), kept as its own table
-- rather than reusing ai_generation_log: a single "generate" call here can fan out into dozens of
-- underlying AI calls (one per chapter batch), so it costs far more per invocation than a Quick
-- Test generation and needs a separately-tracked, much stricter limit. One row per "generate"
-- phase call that ACTUALLY SUCCEEDED (recorded only once questions.length > 0 — see
-- recordGenerateUsage/checkGenerateQuota; the research phase isn't logged/limited here either —
-- it's a single lightweight call, cheap enough not to need its own cap). Same "RLS on, no
-- policies, service-role only" shape as ai_generation_log.sql.
--
-- exam_name also doubles as the source for "Set N" quiz titles: generate-full-test counts a
-- user's prior successful rows for the same exam_name (all-time, not just the 24h rate-limit
-- window) to number each mock test — "JEE Main - Set 1", then "JEE Main - Set 2", etc. — instead
-- of every attempt getting an identical, indistinguishable title.
--
-- Run this once in the Supabase SQL Editor. Safe to re-run (create table if not exists, add
-- column if not exists).

create table if not exists full_test_generation_log (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  exam_name text,
  created_at timestamptz not null default now()
);

alter table full_test_generation_log add column if not exists exam_name text;

create index if not exists full_test_generation_log_user_id_created_at_idx on full_test_generation_log (user_id, created_at);
create index if not exists full_test_generation_log_user_id_exam_name_idx on full_test_generation_log (user_id, lower(exam_name));

-- No policies on purpose — only ever read/written by generate-full-test via the service-role key,
-- same reasoning as ai_generation_log/push_notification_log.
alter table full_test_generation_log enable row level security;
