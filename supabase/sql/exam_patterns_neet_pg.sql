-- supabase/sql/exam_patterns_neet_pg.sql
--
-- Seeds 'NEET PG' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). NEET PG is India's postgraduate medical
-- entrance exam (for MD/MS/PG-Diploma admission) -- a completely different syllabus from NEET UG
-- (which is school-level Physics/Chemistry/Biology): NEET PG tests clinical, pre-clinical, and
-- para-clinical subjects from the MBBS curriculum.
--
-- Real NEET PG structure: 200 single-best-response MCQs (4 options, exactly one correct -- unlike
-- JEE Advanced, there is no multi-correct or numerical-answer format), +4 for a correct answer,
-- -1 for an incorrect one, no marks for unattempted, 3 hours 30 minutes. Treat these numbers, and
-- the subject/chapter weightages below, as a reasonable curated starting point rather than an
-- official NBEMS publication -- actual yield varies by year.
--
-- Chapters are grouped under 4 broad subjects the way JEE's chapters are grouped under
-- Physics/Chemistry/Mathematics; weightagePercent is relative to the OTHER chapters within the
-- same subject (they sum to ~100 per subject), not the overall exam.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'NEET PG',
  200,
  800,
  210,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 4, "incorrectMarks": -1}'::jsonb,
  '[
    {
      "name": "Pre- and Para-Clinical Sciences",
      "chapters": [
        {"name": "Anatomy", "weightagePercent": 10, "easyPercent": 20, "mediumPercent": 50, "hardPercent": 30},
        {"name": "Physiology", "weightagePercent": 10, "easyPercent": 20, "mediumPercent": 50, "hardPercent": 30},
        {"name": "Biochemistry", "weightagePercent": 10, "easyPercent": 20, "mediumPercent": 50, "hardPercent": 30},
        {"name": "Pathology", "weightagePercent": 20, "easyPercent": 10, "mediumPercent": 45, "hardPercent": 45},
        {"name": "Microbiology", "weightagePercent": 15, "easyPercent": 15, "mediumPercent": 45, "hardPercent": 40},
        {"name": "Pharmacology", "weightagePercent": 20, "easyPercent": 10, "mediumPercent": 45, "hardPercent": 45},
        {"name": "Forensic Medicine and Toxicology", "weightagePercent": 5, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "PSM (Community Medicine)", "weightagePercent": 10, "easyPercent": 20, "mediumPercent": 50, "hardPercent": 30}
      ]
    },
    {
      "name": "Clinical Medicine and Allied",
      "chapters": [
        {"name": "General Medicine", "weightagePercent": 55, "easyPercent": 10, "mediumPercent": 45, "hardPercent": 45},
        {"name": "Pediatrics", "weightagePercent": 20, "easyPercent": 15, "mediumPercent": 50, "hardPercent": 35},
        {"name": "Psychiatry", "weightagePercent": 15, "easyPercent": 15, "mediumPercent": 50, "hardPercent": 35},
        {"name": "Dermatology", "weightagePercent": 10, "easyPercent": 20, "mediumPercent": 50, "hardPercent": 30}
      ]
    },
    {
      "name": "Surgery and Allied",
      "chapters": [
        {"name": "General Surgery", "weightagePercent": 65, "easyPercent": 10, "mediumPercent": 45, "hardPercent": 45},
        {"name": "Orthopedics", "weightagePercent": 35, "easyPercent": 15, "mediumPercent": 50, "hardPercent": 35}
      ]
    },
    {
      "name": "Obstetrics and Gynaecology, Anesthesia",
      "chapters": [
        {"name": "Obstetrics and Gynaecology", "weightagePercent": 70, "easyPercent": 15, "mediumPercent": 50, "hardPercent": 35},
        {"name": "Anesthesia and Critical Care", "weightagePercent": 30, "easyPercent": 10, "mediumPercent": 45, "hardPercent": 45}
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
