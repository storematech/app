-- supabase/sql/student_app_rpcs.sql
--
-- The Student Android app authenticates with the plain anon key (no Supabase Auth session — see
-- LearnerAuthRepository's own KDoc for why: email-only login, no OTP, for now). `learners` and
-- `quiz_responses` both carry PII and are NOT anon-readable directly — this is by design, not a bug:
-- get_quiz_leaderboard's own migration explicitly DROPs a permissive "anyone can view leaderboard
-- data" policy on quiz_responses in favor of exactly this security-definer-RPC pattern, and
-- verify-learner-password/verify-quiz-access already read `learners` only through a service-role
-- Edge Function for the same reason. These RPCs are that same narrow door for the Student app.
--
-- Run this once in the Supabase SQL Editor (safe to re-run — every statement is create-or-replace/grant).

-- Email-only learner lookup (see the accepted "no OTP for now" limitation: email has no uniqueness
-- constraint, so the most-recently-created match wins on a collision).
create or replace function public.student_find_learner_by_email(p_email text)
returns table (
  id uuid,
  student_id text,
  name text,
  email text,
  group_id uuid,
  group_name text,
  created_by uuid
)
language sql
stable
security definer
set search_path = public
as $$
  select l.id, l.student_id, l.name, l.email, l.group_id, g.name as group_name, l.created_by
  from public.learners l
  left join public.groups g on g.id = l.group_id
  where l.email ilike p_email
  order by l.created_at desc
  limit 1;
$$;

grant execute on function public.student_find_learner_by_email(text) to anon, authenticated;

-- A learner's own completed quiz responses — powers the Dashboard/Quizzes schedule-status
-- cross-reference (Live/Upcoming/Completed/Closed) and, client-side, finding the one response for
-- a specific quiz when reopening its result screen.
create or replace function public.student_get_completed_responses(p_learner_id uuid)
returns table (
  id uuid,
  quiz_id uuid,
  score int,
  completed_at timestamptz
)
language sql
stable
security definer
set search_path = public
as $$
  select qr.id, qr.quiz_id, qr.score, qr.completed_at
  from public.quiz_responses qr
  where qr.learner_id = p_learner_id
    and qr.completed = true
  order by qr.completed_at desc;
$$;

grant execute on function public.student_get_completed_responses(uuid) to anon, authenticated;

-- Per-question review rows for one already-submitted response — powers reopening a completed
-- quiz's real result screen. question_id/selected_option_id/correct_option_id are cast to text:
-- quiz_answer_details stores these as plain text snapshots (not uuid FKs), and p_response_id is
-- compared the same way so this works regardless of response_id's actual stored type.
create or replace function public.student_get_answer_details(p_response_id text)
returns table (
  question_id text,
  question_text text,
  question_type text,
  answer text,
  selected_option_id text,
  selected_option_text text,
  correct_option_id text,
  correct_option_text text,
  is_correct boolean,
  points_earned numeric,
  status text
)
language sql
stable
security definer
set search_path = public
as $$
  select question_id::text, question_text, question_type, answer, selected_option_id::text,
         selected_option_text, correct_option_id::text, correct_option_text, is_correct,
         points_earned, status
  from public.quiz_answer_details
  where response_id::text = p_response_id;
$$;

grant execute on function public.student_get_answer_details(text) to anon, authenticated;

-- Class ranking by average completed-quiz score, scoped to one group under one teacher — computed
-- server-side so the client never needs a raw learners/quiz_responses join of its own.
create or replace function public.student_get_group_leaderboard(p_group_id uuid, p_created_by uuid)
returns table (
  learner_id uuid,
  name text,
  avg_score numeric
)
language sql
stable
security definer
set search_path = public
as $$
  select l.id as learner_id, l.name, avg(qr.score) as avg_score
  from public.learners l
  join public.quiz_responses qr on qr.learner_id = l.id and qr.completed = true
  where l.group_id = p_group_id
    and l.created_by = p_created_by
  group by l.id, l.name
  order by avg_score desc;
$$;

grant execute on function public.student_get_group_leaderboard(uuid, uuid) to anon, authenticated;

-- The quiz creator's business branding (for the Student app's "share my score" card) — same
-- reasoning as everything above: `profiles` is only ever read today via an authenticated session
-- scoped to the caller's own row (see PdfBrandingProvider.kt in the Teacher app), which an anon
-- Student app client doesn't have.
create or replace function public.student_get_business_branding(p_created_by uuid)
returns table (
  business_name text,
  business_logo text,
  address text,
  letterhead_phone text
)
language sql
stable
security definer
set search_path = public
as $$
  select business_name, business_logo, address, letterhead_phone
  from public.profiles
  where id = p_created_by;
$$;

grant execute on function public.student_get_business_branding(uuid) to anon, authenticated;
