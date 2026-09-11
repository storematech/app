-- supabase/sql/exam_patterns_ssc_chsl.sql
--
-- Seeds 'SSC-CHSL' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). SSC CHSL (Combined Higher Secondary
-- Level) recruits 12th-pass candidates for LDC/JSA, PA/SA, and DEO posts. This seed models
-- Tier I -- the objective MCQ stage every candidate takes -- since Tier II (descriptive) and the
-- skill/typing tests for Tier III/IV aren't MCQ-based.
--
-- Real SSC CHSL Tier I structure: 100 single-best-response MCQs (4 options, exactly one
-- correct), 25 questions each from General Intelligence, General Awareness, Quantitative
-- Aptitude, and English Language, 2 marks each (200 total), -0.5 for a wrong answer, 60 minutes
-- -- numerically identical to SSC-CGL Tier I (see exam_patterns_ssc_cgl.sql), but pitched at a
-- 12th-standard level rather than graduate level, reflected in the easier chapter difficulty
-- splits below. Treat these numbers, and the chapter weightages, as a reasonable curated starting
-- point rather than an official SSC publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'SSC-CHSL',
  100,
  200,
  60,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 2, "incorrectMarks": -0.5}'::jsonb,
  '[
    {
      "name": "General Intelligence",
      "chapters": [
        {"name": "Verbal Reasoning (Analogies, Classification, Series)", "weightagePercent": 30, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Non-Verbal and Figural Reasoning", "weightagePercent": 20, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Logical Reasoning (Syllogism, Statements and Conclusions)", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Analytical Reasoning (Coding-Decoding, Blood Relations, Direction Sense)", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15}
      ]
    },
    {
      "name": "General Awareness",
      "chapters": [
        {"name": "Indian History and Culture", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 50, "hardPercent": 10},
        {"name": "Indian Polity and Geography", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Indian Economy and General Science", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Current Affairs and Static GK", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15}
      ]
    },
    {
      "name": "Quantitative Aptitude",
      "chapters": [
        {"name": "Number System and Simplification", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Arithmetic (Percentage, Profit-Loss, SI-CI, Ratio)", "weightagePercent": 30, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Geometry and Mensuration", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Trigonometry and Data Interpretation", "weightagePercent": 20, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15}
      ]
    },
    {
      "name": "English Language",
      "chapters": [
        {"name": "Grammar and Error Spotting", "weightagePercent": 30, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Vocabulary (Synonyms, Antonyms, Spelling)", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Cloze Test and Fill in the Blanks", "weightagePercent": 20, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Reading Comprehension and Para Jumbles", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
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
