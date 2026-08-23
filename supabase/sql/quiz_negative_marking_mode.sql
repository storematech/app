-- supabase/sql/quiz_negative_marking_mode.sql
--
-- Adds a quiz-level negative-marking mode so the Create Quiz screen can offer 2 systems:
--   'none'         — negative marking off by default for this quiz (individual questions can
--                    still be negative-marked in the question editor, same as always).
--   'uniform'      — one deduction amount (negative_marking_value) applies to every question
--                    added to this quiz; the app pre-fills each new question's own
--                    questions.negative_points with this value as it's added.
--   'per_question' — no default is applied; the teacher sets negative_points individually per
--                    question (the app shows a reminder to do so while adding one).
--
-- These two columns are creation-time UI/authoring metadata only — grading always reads the
-- existing per-question questions.negative_points column (see negative_marking.sql), so this
-- migration makes no change to how a quiz attempt is scored.
--
-- Run this once in the Supabase SQL Editor.

alter table public.quizzes
  add column if not exists negative_marking_mode text not null default 'none',
  add column if not exists negative_marking_value numeric(6,2) not null default 1;

alter table public.quizzes
  drop constraint if exists quizzes_negative_marking_mode_check;

alter table public.quizzes
  add constraint quizzes_negative_marking_mode_check
    check (negative_marking_mode in ('none', 'uniform', 'per_question'));
