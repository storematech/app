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
-- IMPORTANT: the `update_quiz_max_points()` trigger function (fired by
-- trigger_update_quiz_max_points_question_update) is not defined anywhere in this repo, so it
-- could not be inspected as part of this change. After running this migration, open that function
-- in the Supabase SQL Editor and confirm it doesn't hard-cast points/max_points to integer
-- internally (e.g. `sum(points)::integer`) — if it does, decimal points will get silently
-- truncated server-side even though the app now sends decimals correctly.
--
-- Run this once in the Supabase SQL Editor.

alter table public.questions
  alter column points type numeric(6,2) using points::numeric(6,2),
  alter column points set default 1;

alter table public.questions
  add column if not exists negative_points numeric(6,2) not null default 0;

alter table public.quizzes
  alter column max_points type numeric(8,2) using max_points::numeric(8,2);
