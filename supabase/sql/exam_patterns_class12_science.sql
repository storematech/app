-- supabase/sql/exam_patterns_class12_science.sql
--
-- Seeds 'Class 12 Science' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). Same three-stream split as Class 11 (see
-- exam_patterns_class11_science.sql for the rationale), now at Class 12 -- the second and final
-- CBSE board-exam grade. Models Physics, Chemistry, Mathematics, and English (the PCM path).
-- Still no negative marking and MCQ only (numerical/descriptive formats aren't the primary
-- assessment style tested here), with the hard share pushed to its highest level yet, reflecting
-- final board-exam-year rigor -- electrostatics/EM induction-optics in Physics, electrochemistry/
-- organic chemistry-biomolecules in Chemistry, and continuity-differentiability/integrals-vectors
-- in Mathematics.
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 12 Science general-knowledge practice test, not an official CBSE publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 12 Science',
  40,
  40,
  60,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "Physics",
      "chapters": [
        {"name": "Electrostatics and Current Electricity", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50},
        {"name": "Magnetism-EM Induction, and Optics-Modern Physics", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50}
      ]
    },
    {
      "name": "Chemistry",
      "chapters": [
        {"name": "Solid State, Solutions and Electrochemistry", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50},
        {"name": "Organic Chemistry (Alcohols-Aldehydes-Acids) and Biomolecules", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Relations-Functions and Continuity-Differentiability", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50},
        {"name": "Integrals, Differential Equations, Vectors and 3D Geometry", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50}
      ]
    },
    {
      "name": "English",
      "chapters": [
        {"name": "Advanced Grammar (Modals, Voice) and Note-Making", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50},
        {"name": "Reading Comprehension and Vocabulary", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50}
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
