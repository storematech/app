-- supabase/sql/negative_marking.sql
--
-- Widens questions.points (and the quizzes.max_points it sums into) from integer to numeric so
-- decimal point values (e.g. 1.2) are possible, and adds a per-question negative-marking amount.
-- negative_points is stored as a positive magnitude (how much to deduct on a wrong answer) —
-- 0 means negative marking is off for that question; there's no separate boolean flag, the app
-- infers "enabled" from negative_points > 0.
--
-- quiz_answer_details.points_earned is ALREADY numeric in this DB (confirmed via the app's own
-- QuizAnswerDetailDto.kt) — not touched here.
--
-- update_quiz_max_points() (fired by trigger_update_quiz_max_points_question_update on
-- public.questions) was inspected via pg_get_functiondef — it sums points with no integer cast
-- (`COALESCE(SUM(q.points), 0)`), so numeric points flow through it untruncated. No changes
-- needed to the function itself.
--
-- Postgres refuses to ALTER COLUMN TYPE on questions.points while that trigger depends on it
-- ("cannot alter type of a column used in a trigger definition"), so this drops the trigger,
-- alters the column, then recreates the trigger from its own captured definition — this doesn't
-- require knowing the trigger's exact text in advance, and guarantees it comes back identical.
--
-- Run this once in the Supabase SQL Editor.

do $$
declare
  trigger_def text;
begin
  select pg_get_triggerdef(oid) into trigger_def
  from pg_trigger
  where tgname = 'trigger_update_quiz_max_points_question_update'
    and not tgisinternal;

  if trigger_def is null then
    raise exception 'trigger_update_quiz_max_points_question_update not found — aborting before altering points';
  end if;

  execute 'drop trigger trigger_update_quiz_max_points_question_update on public.questions';

  alter table public.questions
    alter column points type numeric(6,2) using points::numeric(6,2),
    alter column points set default 1;

  execute trigger_def;
end $$;

alter table public.questions
  add column if not exists negative_points numeric(6,2) not null default 0;

alter table public.quizzes
  alter column max_points type numeric(8,2) using max_points::numeric(8,2);
