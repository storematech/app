-- supabase/sql/exam_patterns_wbjee.sql
--
-- Seeds 'WBJEE' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). WBJEE (West Bengal Joint Entrance
-- Examination) is a state-level engineering entrance exam -- Physics, Chemistry, Mathematics.
--
-- Real WBJEE structure is genuinely more complex than this schema can represent: it runs two
-- papers (Paper 1: Mathematics; Paper 2: Physics & Chemistry) and splits questions into 3
-- categories with DIFFERENT marks and negative-marking rules each -- Category 1 (1 mark, -0.25),
-- Category 2 (2 marks, -0.5), and Category 3 (2 marks, one-or-more-correct, no negative marking).
-- This schema only supports one flat marks/negative-marking pair and one question_types list, so
-- this seed picks a single representative scheme (2 marks correct, -0.5 incorrect) rather than
-- modeling all three categories -- similar in spirit to the simplifications already made for JEE
-- Advanced's two-paper structure, BITSAT's bonus questions, and MHT-CET's two-paper split. Treat
-- all the numbers below, including chapter weightages, as a reasonable curated starting point
-- rather than an official WBJEEB publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'WBJEE',
  155,
  200,
  180,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 2, "incorrectMarks": -0.5}'::jsonb,
  '[
    {
      "name": "Physics",
      "chapters": [
        {"name": "Mechanics", "weightagePercent": 25, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25},
        {"name": "Electrodynamics", "weightagePercent": 25, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25},
        {"name": "Optics", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Modern Physics and Semiconductor Devices", "weightagePercent": 15, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Heat and Thermodynamics", "weightagePercent": 15, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25}
      ]
    },
    {
      "name": "Chemistry",
      "chapters": [
        {"name": "Physical Chemistry", "weightagePercent": 25, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25},
        {"name": "Organic Chemistry", "weightagePercent": 25, "easyPercent": 20, "mediumPercent": 50, "hardPercent": 30},
        {"name": "Inorganic Chemistry", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Chemical Bonding and States of Matter", "weightagePercent": 15, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25},
        {"name": "Environmental and Analytical Chemistry", "weightagePercent": 15, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Algebra", "weightagePercent": 20, "easyPercent": 20, "mediumPercent": 50, "hardPercent": 30},
        {"name": "Differentiation and Applications", "weightagePercent": 20, "easyPercent": 15, "mediumPercent": 50, "hardPercent": 35},
        {"name": "Integration and Differential Equations", "weightagePercent": 20, "easyPercent": 15, "mediumPercent": 50, "hardPercent": 35},
        {"name": "Coordinate Geometry", "weightagePercent": 15, "easyPercent": 20, "mediumPercent": 50, "hardPercent": 30},
        {"name": "Trigonometry", "weightagePercent": 12, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Vectors and Statistics", "weightagePercent": 13, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25}
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
