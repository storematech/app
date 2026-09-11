-- supabase/sql/exam_patterns_class11_science.sql
--
-- Seeds 'Class 11 Science' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). This continues the "Schools" category
-- exams (see ExamCategory.SCHOOLS in FullTestModels.kt). Class 11 is where CBSE splits into
-- distinct streams with almost no shared curriculum, so unlike Class 1-10 (a single combined
-- pattern per grade, see exam_patterns_class10.sql) this grade gets one exam_patterns entry per
-- stream: 'Class 11 Science' (this file), 'Class 11 Commerce', and 'Class 11 Arts'. This entry
-- models the PCM (Physics, Chemistry, Mathematics) path plus English -- the stream most relevant
-- to this app's existing competitive-exam-prep audience (JEE question banks already exist
-- elsewhere in this table). Still no negative marking and MCQ only (numerical/descriptive formats
-- aren't the primary assessment style tested here), with a further-increased hard share
-- reflecting senior-secondary rigor.
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 11 Science general-knowledge practice test, not an official CBSE publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 11 Science',
  40,
  40,
  60,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "Physics",
      "chapters": [
        {"name": "Units, Measurements and Kinematics", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45},
        {"name": "Laws of Motion, Work-Energy and Gravitation", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45}
      ]
    },
    {
      "name": "Chemistry",
      "chapters": [
        {"name": "Atomic Structure, Periodic Table and Chemical Bonding", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45},
        {"name": "States of Matter, Thermodynamics and Equilibrium", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Sets, Relations-Functions and Trigonometry", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45},
        {"name": "Complex Numbers, Sequences-Series and Straight Lines", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45}
      ]
    },
    {
      "name": "English",
      "chapters": [
        {"name": "Grammar (Determiners, Advanced Tenses) and Note-Making", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45},
        {"name": "Reading Comprehension and Vocabulary", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45}
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
