-- supabase/sql/exam_patterns_class8.sql
--
-- Seeds 'Class 8' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). This continues the "Schools" category
-- exams (see ExamCategory.SCHOOLS in FullTestModels.kt) -- general CBSE-aligned grade-8
-- practice, not tied to any specific board exam. Same 4-subject structure as Class 6/7 (see
-- exam_patterns_class7.sql): English, Mathematics, Science, and Social Science. Still no
-- negative marking and MCQ only (numerical/descriptive formats aren't age-appropriate here), with
-- a further-increased hard share -- modals/subject-verb agreement in English, exponents/linear
-- equations in Maths, cell structure/force-friction/chemical effects of current in Science, and
-- Company Rule-1857 Revolt/Constitution-judiciary in Social Science.
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 8 general-knowledge practice test, not an official CBSE publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 8',
  40,
  40,
  50,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "English",
      "chapters": [
        {"name": "Modals, Determiners and Subject-Verb Agreement", "weightagePercent": 50, "easyPercent": 30, "mediumPercent": 40, "hardPercent": 30},
        {"name": "Reading Comprehension and Vocabulary", "weightagePercent": 50, "easyPercent": 30, "mediumPercent": 40, "hardPercent": 30}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Rational Numbers, Exponents and Squares-Square Roots", "weightagePercent": 50, "easyPercent": 30, "mediumPercent": 40, "hardPercent": 30},
        {"name": "Linear Equations, Mensuration and Data Handling", "weightagePercent": 50, "easyPercent": 30, "mediumPercent": 40, "hardPercent": 30}
      ]
    },
    {
      "name": "Science",
      "chapters": [
        {"name": "Crop Production, Microorganisms and Cell Structure", "weightagePercent": 50, "easyPercent": 30, "mediumPercent": 40, "hardPercent": 30},
        {"name": "Force, Friction, Sound and Chemical Effects of Current", "weightagePercent": 50, "easyPercent": 30, "mediumPercent": 40, "hardPercent": 30}
      ]
    },
    {
      "name": "Social Science",
      "chapters": [
        {"name": "History (Company Rule to 1857 Revolt) and Geography (Resources)", "weightagePercent": 50, "easyPercent": 30, "mediumPercent": 40, "hardPercent": 30},
        {"name": "Civics (Indian Constitution, Judiciary and Social Justice)", "weightagePercent": 50, "easyPercent": 30, "mediumPercent": 40, "hardPercent": 30}
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
