-- supabase/sql/exam_patterns_mht_cet.sql
--
-- Seeds 'MHT-CET' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). MHT-CET (Maharashtra Common Entrance
-- Test) is a state-level engineering/pharmacy entrance exam. This seed models the PCM (engineering)
-- group -- Physics, Chemistry, Mathematics -- the more common track, matching how JEE/BITSAT are
-- already modeled in this app.
--
-- Real MHT-CET PCM structure: 150 single-best-response MCQs (4 options, exactly one correct) --
-- 50 each in Physics, Chemistry, and Mathematics. Physics and Chemistry questions carry 1 mark
-- each; Mathematics questions carry 2 marks each (total 200 marks) -- a quirk this schema can't
-- represent per-question, so treat the 200 total as the headline figure. UNLIKE JEE/NEET/BITSAT,
-- MHT-CET has NO negative marking. The real exam is actually run as two separate papers
-- (Mathematics: 90 minutes; Physics & Chemistry combined: 90 minutes) -- this seed combines them
-- into one 180-minute session for simplicity, the same kind of simplification already made for JEE
-- Advanced's two-paper structure and BITSAT's bonus-question rule. Treat all these numbers, and
-- the subject/chapter weightages below, as a reasonable curated starting point rather than an
-- official State CET Cell publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'MHT-CET',
  150,
  200,
  180,
  '["mcq"]'::jsonb,
  '{"enabled": false, "correctMarks": 1, "incorrectMarks": 0}'::jsonb,
  '[
    {
      "name": "Physics",
      "chapters": [
        {"name": "Mechanics", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Electrodynamics", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Optics", "weightagePercent": 20, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Modern Physics", "weightagePercent": 15, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Heat and Thermodynamics", "weightagePercent": 15, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15}
      ]
    },
    {
      "name": "Chemistry",
      "chapters": [
        {"name": "Physical Chemistry", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Organic Chemistry", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Inorganic Chemistry", "weightagePercent": 20, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Electrochemistry and Chemical Kinetics", "weightagePercent": 15, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Solid State and Solutions", "weightagePercent": 15, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Algebra", "weightagePercent": 20, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Differentiation and Applications", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Integration and Differential Equations", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Coordinate Geometry", "weightagePercent": 15, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Trigonometric Functions", "weightagePercent": 15, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Vectors and 3D Geometry", "weightagePercent": 10, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
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
