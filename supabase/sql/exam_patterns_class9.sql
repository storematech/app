-- supabase/sql/exam_patterns_class9.sql
--
-- Seeds 'Class 9' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). This continues the "Schools" category
-- exams (see ExamCategory.SCHOOLS in FullTestModels.kt) -- general CBSE-aligned grade-9
-- practice, not tied to any specific board exam. Same 4-subject structure as Class 6-8 (see
-- exam_patterns_class8.sql): English, Mathematics, Science, and Social Science, but now at
-- secondary-school rigor -- the first grade where Social Science introduces a distinct Civics
-- (Democracy) and Economics (Poverty and Livelihood) strand rather than a single combined civics
-- chapter. Still no negative marking and MCQ only (numerical/descriptive formats aren't the
-- primary assessment style tested here), with a further-increased hard share.
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 9 general-knowledge practice test, not an official CBSE publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 9',
  40,
  40,
  50,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "English",
      "chapters": [
        {"name": "Modals, Voice and Sentence Transformation", "weightagePercent": 50, "easyPercent": 25, "mediumPercent": 40, "hardPercent": 35},
        {"name": "Reading Comprehension and Vocabulary", "weightagePercent": 50, "easyPercent": 25, "mediumPercent": 40, "hardPercent": 35}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Number Systems and Polynomials", "weightagePercent": 50, "easyPercent": 25, "mediumPercent": 40, "hardPercent": 35},
        {"name": "Coordinate Geometry, Linear Equations and Mensuration", "weightagePercent": 50, "easyPercent": 25, "mediumPercent": 40, "hardPercent": 35}
      ]
    },
    {
      "name": "Science",
      "chapters": [
        {"name": "Matter, Atoms-Molecules and Cell Structure", "weightagePercent": 50, "easyPercent": 25, "mediumPercent": 40, "hardPercent": 35},
        {"name": "Motion, Force, Gravitation and Sound", "weightagePercent": 50, "easyPercent": 25, "mediumPercent": 40, "hardPercent": 35}
      ]
    },
    {
      "name": "Social Science",
      "chapters": [
        {"name": "History (French Revolution) and Geography (Physical Features of India)", "weightagePercent": 50, "easyPercent": 25, "mediumPercent": 40, "hardPercent": 35},
        {"name": "Civics (Democracy) and Economics (Poverty and Livelihood)", "weightagePercent": 50, "easyPercent": 25, "mediumPercent": 40, "hardPercent": 35}
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
