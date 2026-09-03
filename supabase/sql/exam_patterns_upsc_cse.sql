-- supabase/sql/exam_patterns_upsc_cse.sql
--
-- Seeds 'UPSC CSE (IAS)' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). UPSC Civil Services Examination is India's
-- premier civil services exam (IAS/IPS/IFS and allied services), run in three stages: Prelims
-- (objective MCQ), Mains (descriptive/essay), and Interview (personality test). Only Prelims is
-- MCQ-based, so this seed -- and the question bank -- models Prelims only; Mains/Interview don't
-- fit an MCQ question bank at all.
--
-- Real UPSC Prelims structure: TWO separate papers on the same day. Paper I (General Studies,
-- 100 questions, 2 marks each = 200 marks, counts toward merit) and Paper II (CSAT -- General
-- Studies Paper 2, an aptitude test, 80 questions, 2.5 marks each = 200 marks, QUALIFYING ONLY --
-- a candidate needs 33% to pass, but the CSAT score itself does not count toward the merit list).
-- Both use -1/3rd-of-the-question's-marks negative marking for wrong answers. This schema can't
-- represent two papers with different marks-per-question and different merit-counting rules, so
-- (like the earlier simplifications for JEE Advanced, MHT-CET, WBJEE, and VITEEE) this seed picks
-- GS Paper I's scheme (2 marks correct, -0.66 incorrect) as the single representative negative-
-- marking value, and duration combines both 2-hour papers into one 240-minute session. Treat all
-- these numbers, and the subject/chapter weightages below, as a reasonable curated starting point
-- rather than an official UPSC publication -- actual topic-wise question counts vary every year.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'UPSC CSE (IAS)',
  180,
  400,
  240,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 2, "incorrectMarks": -0.66}'::jsonb,
  '[
    {
      "name": "General Studies Paper I",
      "chapters": [
        {"name": "Ancient Indian History", "weightagePercent": 8, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25},
        {"name": "Medieval Indian History", "weightagePercent": 8, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25},
        {"name": "Modern Indian History and Freedom Struggle", "weightagePercent": 12, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25},
        {"name": "Indian Art and Culture", "weightagePercent": 8, "easyPercent": 20, "mediumPercent": 50, "hardPercent": 30},
        {"name": "Indian and World Geography", "weightagePercent": 15, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25},
        {"name": "Indian Polity and Governance", "weightagePercent": 20, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25},
        {"name": "Indian Economy", "weightagePercent": 12, "easyPercent": 15, "mediumPercent": 55, "hardPercent": 30},
        {"name": "Environment and Ecology", "weightagePercent": 8, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25},
        {"name": "General Science and Technology", "weightagePercent": 5, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Current Affairs and International Relations", "weightagePercent": 4, "easyPercent": 20, "mediumPercent": 50, "hardPercent": 30}
      ]
    },
    {
      "name": "CSAT (General Studies Paper II)",
      "chapters": [
        {"name": "Reading Comprehension", "weightagePercent": 35, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Logical Reasoning and Analytical Ability", "weightagePercent": 25, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Decision Making and Problem Solving", "weightagePercent": 10, "easyPercent": 25, "mediumPercent": 55, "hardPercent": 20},
        {"name": "Basic Numeracy", "weightagePercent": 15, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Data Interpretation", "weightagePercent": 10, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Interpersonal and Communication Skills", "weightagePercent": 5, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15}
      ]
    }
  ]'::jsonb,
  'curated'
)
on conflict (exam_name) do update set
  total_questions = excluded.total_questions,
  total_marks = excluded.total_marks,
  duration_minutes = excluded.duration_minutes,
  question_types = excluded.question_types,
  negative_marking = excluded.negative_marking,
  subjects = excluded.subjects,
  source = excluded.source,
  updated_at = now();
