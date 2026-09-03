-- supabase/sql/fix_full_test_ungraded_questions.sql
--
-- One-off data fix, not a schema change: before the FullTestViewModel.toQuestion() bug fix,
-- EVERY non-MCQ question Full Test generated (numerical and free-text/descriptive alike) was
-- saved to the `questions` table with is_ungraded = true, permanently hiding it from the Manual
-- Marking card on QuizDetailScreen with no in-app way to flip it. The code fix only affects
-- quizzes created from now on -- this script is how you retroactively un-hide the ones already
-- sitting in your account.
--
-- exam_question_bank (the curated NEET/JEE/etc. seed table) has no is_ungraded column at all --
-- that flag only ever lives on the real `questions` table, set at the moment a Full Test batch is
-- turned into saved questions. So this script targets `questions`, not exam_question_bank.
--
-- How rows are identified as "came from Full Test": FullTestViewModel.toQuestion() always tags a
-- generated question with exactly 3 tags, `["AI Generated", subject, chapter]` -- e.g.
-- ["AI Generated", "Physics", "Mechanics..."]. Plain AI Quiz-generated questions only ever get a
-- single tag, `["AI Generated"]`, so they're unaffected by this filter. This can't be 100% certain
-- (nothing rules out a manually-created question that happens to carry the same 3-tag shape), so
-- run the SELECT preview first and eyeball the `text`/`tags` columns before running the UPDATE.
--
-- IMPORTANT: this only makes these questions *eligible* for Manual Marking again (is_ungraded =
-- false) -- it does NOT retroactively score any already-submitted response. Every submission
-- against these questions still needs a human pass through Manual Marking to award points, exactly
-- like any other gradable free-text question.

-- ---- Step 1: preview what would change -- run this first -------------------------------------
select id, text, tags, type, is_ungraded, created_at
from questions
where is_ungraded = true
  and type = 'free-text'
  and array_length(tags, 1) = 3
  and tags[1] = 'AI Generated'
order by created_at desc;

-- ---- Step 2: apply the fix, once the preview above looks right -------------------------------
update questions
set is_ungraded = false
where is_ungraded = true
  and type = 'free-text'
  and array_length(tags, 1) = 3
  and tags[1] = 'AI Generated';

-- ---- Optional: scope Step 2 to a single quiz instead of every quiz on the account -------------
-- Replace '<QUIZ_ID>' with the quiz's id (visible in its share URL / QuizDetailScreen).
-- update questions
-- set is_ungraded = false
-- where is_ungraded = true
--   and type = 'free-text'
--   and array_length(tags, 1) = 3
--   and tags[1] = 'AI Generated'
--   and id in (
--     select question_id from quiz_questions where quiz_id = '<QUIZ_ID>'
--   );
