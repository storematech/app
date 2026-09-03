-- supabase/sql/exam_patterns_neet_ug.sql
--
-- Seeds 'NEET UG' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). NEET UG is India's undergraduate medical
-- entrance exam (for MBBS/BDS admission) -- school-level (class 11-12 NCERT) Physics, Chemistry,
-- and Biology, distinct from NEET PG (see exam_patterns_neet_pg.sql), which tests postgraduate
-- clinical subjects.
--
-- Real NEET UG structure: each of the 4 subjects (Physics, Chemistry, Botany, Zoology) has a
-- Section A (35 compulsory questions) and Section B (15 questions, attempt any 10) -- 200 total,
-- 180 actually attempted/scored. All questions are single-best-response MCQ (4 options, exactly
-- one correct), +4 for a correct answer, -1 for an incorrect one, 3 hours 20 minutes. Treat these
-- numbers, and the subject/chapter weightages below, as a reasonable curated starting point
-- rather than an official NTA publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'NEET UG',
  180,
  720,
  200,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 4, "incorrectMarks": -1}'::jsonb,
  '[
    {
      "name": "Physics",
      "chapters": [
        {"name": "Mechanics", "weightagePercent": 35, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Electrodynamics", "weightagePercent": 30, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Optics and Wave Phenomena", "weightagePercent": 20, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Modern Physics and Semiconductor Devices", "weightagePercent": 15, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
      ]
    },
    {
      "name": "Chemistry",
      "chapters": [
        {"name": "Physical Chemistry", "weightagePercent": 30, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Organic Chemistry", "weightagePercent": 30, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Inorganic Chemistry", "weightagePercent": 30, "easyPercent": 35, "mediumPercent": 45, "hardPercent": 20},
        {"name": "Biomolecules and Environmental Chemistry", "weightagePercent": 10, "easyPercent": 35, "mediumPercent": 45, "hardPercent": 20}
      ]
    },
    {
      "name": "Botany",
      "chapters": [
        {"name": "Plant Physiology", "weightagePercent": 30, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Cell Biology and Genetics (Plant/General)", "weightagePercent": 30, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Ecology and Environment", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 45, "hardPercent": 20},
        {"name": "Plant Morphology and Anatomy", "weightagePercent": 15, "easyPercent": 35, "mediumPercent": 45, "hardPercent": 20}
      ]
    },
    {
      "name": "Zoology",
      "chapters": [
        {"name": "Human Physiology", "weightagePercent": 35, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Genetics and Evolution", "weightagePercent": 30, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Human Reproduction and Reproductive Health", "weightagePercent": 20, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Biotechnology and its Applications", "weightagePercent": 15, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
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
