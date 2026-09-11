-- supabase/sql/exam_patterns_class10.sql
--
-- Seeds 'Class 10' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). This continues the "Schools" category
-- exams (see ExamCategory.SCHOOLS in FullTestModels.kt) -- general CBSE-aligned grade-10
-- practice, not tied to any specific board exam, but pitched at board-exam-year rigor (this is
-- the first of the two CBSE board-exam grades). Same 4-subject structure as Class 9 (see
-- exam_patterns_class9.sql): English, Mathematics, Science, and Social Science, with the hard
-- share pushed further -- quadratic equations/trigonometry in Maths, chemical reactions/
-- electricity-light in Science, and Nationalism in India/power-sharing-economic development in
-- Social Science. Still no negative marking and MCQ only (numerical/descriptive formats aren't
-- the primary assessment style tested here).
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 10 general-knowledge practice test, not an official CBSE publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 10',
  40,
  40,
  55,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "English",
      "chapters": [
        {"name": "Reported Speech, Editing and Error Correction", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 40, "hardPercent": 40},
        {"name": "Reading Comprehension and Vocabulary", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 40, "hardPercent": 40}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Real Numbers, Polynomials and Quadratic Equations", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 40, "hardPercent": 40},
        {"name": "Trigonometry, Circles and Statistics-Probability", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 40, "hardPercent": 40}
      ]
    },
    {
      "name": "Science",
      "chapters": [
        {"name": "Chemical Reactions, Acids-Bases-Salts and Life Processes", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 40, "hardPercent": 40},
        {"name": "Electricity, Magnetism and Light", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 40, "hardPercent": 40}
      ]
    },
    {
      "name": "Social Science",
      "chapters": [
        {"name": "History (Nationalism in India) and Geography (Resources and Development)", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 40, "hardPercent": 40},
        {"name": "Civics (Power Sharing and Political Parties) and Economics (Development and Credit)", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 40, "hardPercent": 40}
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
