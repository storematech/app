-- supabase/sql/exam_patterns_viteee.sql
--
-- Seeds 'VITEEE' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). VITEEE (VIT Engineering Entrance Exam) is
-- VIT University's computer-based admission test -- like BITSAT, it adds English and Aptitude
-- sections alongside Physics, Chemistry, and Mathematics (this seed models the Mathematics track,
-- not the Biology track offered for some biotech programs).
--
-- Real VITEEE structure: single-best-response MCQs (4 options, exactly one correct -- no
-- multi-correct or numerical-answer format), NO negative marking (like MHT-CET, unlike
-- JEE/NEET/BITSAT/WBJEE), 2.5 hours, computer-based. Treat these numbers, and the subject/chapter
-- weightages below, as a reasonable curated starting point rather than an official VIT
-- publication -- section sizes have varied slightly by year.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'VITEEE',
  105,
  125,
  150,
  '["mcq"]'::jsonb,
  '{"enabled": false, "correctMarks": 1, "incorrectMarks": 0}'::jsonb,
  '[
    {
      "name": "Physics",
      "chapters": [
        {"name": "Mechanics", "weightagePercent": 30, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Electrodynamics", "weightagePercent": 30, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Optics and Wave Optics", "weightagePercent": 20, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Modern Physics and Semiconductor Devices", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25}
      ]
    },
    {
      "name": "Chemistry",
      "chapters": [
        {"name": "Physical Chemistry", "weightagePercent": 30, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Organic Chemistry", "weightagePercent": 30, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Inorganic Chemistry", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Analytical Chemistry and Environmental Chemistry", "weightagePercent": 15, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Algebra and Matrices", "weightagePercent": 25, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Calculus", "weightagePercent": 30, "easyPercent": 20, "mediumPercent": 50, "hardPercent": 30},
        {"name": "Coordinate Geometry and Vectors", "weightagePercent": 25, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Trigonometry and Probability", "weightagePercent": 20, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
      ]
    },
    {
      "name": "English",
      "chapters": [
        {"name": "Grammar and Sentence Correction", "weightagePercent": 55, "easyPercent": 30, "mediumPercent": 55, "hardPercent": 15},
        {"name": "Vocabulary and Comprehension", "weightagePercent": 45, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15}
      ]
    },
    {
      "name": "Aptitude",
      "chapters": [
        {"name": "Numerical Aptitude", "weightagePercent": 50, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Reasoning and Data Interpretation", "weightagePercent": 50, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
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
