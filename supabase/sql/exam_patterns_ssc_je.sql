-- supabase/sql/exam_patterns_ssc_je.sql
--
-- Seeds 'SSC-JE' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). SSC JE (Junior Engineer) recruits
-- diploma/degree engineers for Civil, Electrical, and Mechanical posts across central government
-- departments (CPWD, MES, BRO, etc.). This seed models Paper I -- the objective CBT stage every
-- candidate takes -- since Paper II (conventional/descriptive, discipline-specific) isn't
-- MCQ-based.
--
-- Real SSC JE Paper I structure: 200 single-best-response MCQs (4 options, exactly one
-- correct), 1 mark each (200 total), -0.25 for a wrong answer, 120 minutes -- General
-- Intelligence & Reasoning (50 Q) and General Awareness (50 Q) are common to every candidate,
-- while General Engineering (100 Q) is discipline-specific (Civil & Structural, Electrical, or
-- Mechanical, chosen at the time of application). This seed's Engineering subject models the
-- Civil & Structural stream specifically, the most commonly chosen discipline, since the app has
-- no per-candidate discipline selection. Treat these numbers, and the chapter weightages below,
-- as a reasonable curated starting point rather than an official SSC publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'SSC-JE',
  200,
  200,
  120,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 1, "incorrectMarks": -0.25}'::jsonb,
  '[
    {
      "name": "General Intelligence and Reasoning",
      "chapters": [
        {"name": "Analogies and Classification", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Series and Coding-Decoding", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Non-Verbal Reasoning", "weightagePercent": 25, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Blood Relations and Direction Sense", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
      ]
    },
    {
      "name": "General Awareness",
      "chapters": [
        {"name": "Indian History and Culture", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Indian Polity and Geography", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "General Science", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Current Affairs and Static GK", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15}
      ]
    },
    {
      "name": "Civil Engineering",
      "chapters": [
        {"name": "Building Materials", "weightagePercent": 12, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Surveying", "weightagePercent": 12, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Strength of Materials", "weightagePercent": 14, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Structural Analysis and Design", "weightagePercent": 14, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Estimating and Costing", "weightagePercent": 12, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Soil Mechanics and Foundation Engineering", "weightagePercent": 12, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Fluid Mechanics and Hydraulics", "weightagePercent": 12, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Environmental Engineering (Water Supply and Sanitation)", "weightagePercent": 12, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15}
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
