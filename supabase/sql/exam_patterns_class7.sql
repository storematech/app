-- supabase/sql/exam_patterns_class7.sql
--
-- Seeds 'Class 7' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). This continues the "Schools" category
-- exams (see ExamCategory.SCHOOLS in FullTestModels.kt) -- general CBSE-aligned grade-7
-- practice, not tied to any specific board exam. Same 4-subject structure introduced at Class 6
-- (see exam_patterns_class6.sql): English, Mathematics, Science, and Social Science. Still no
-- negative marking and MCQ only (numerical/descriptive formats aren't age-appropriate here), with
-- a further-increased hard share -- clauses/reported speech in English, integers/simple equations
-- in Maths, nutrition/heat/acids-bases-salts in Science, and Delhi Sultanate-Mughals/civics in
-- Social Science.
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 7 general-knowledge practice test, not an official CBSE publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 7',
  40,
  40,
  45,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "English",
      "chapters": [
        {"name": "Tenses, Clauses and Reported Speech", "weightagePercent": 50, "easyPercent": 35, "mediumPercent": 40, "hardPercent": 25},
        {"name": "Reading Comprehension and Vocabulary", "weightagePercent": 50, "easyPercent": 35, "mediumPercent": 40, "hardPercent": 25}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Integers, Rational Numbers and Simple Equations", "weightagePercent": 50, "easyPercent": 35, "mediumPercent": 40, "hardPercent": 25},
        {"name": "Percentage, Profit-Loss, Ratio-Proportion and Geometry", "weightagePercent": 50, "easyPercent": 35, "mediumPercent": 40, "hardPercent": 25}
      ]
    },
    {
      "name": "Science",
      "chapters": [
        {"name": "Nutrition, Respiration and Reproduction in Organisms", "weightagePercent": 50, "easyPercent": 35, "mediumPercent": 40, "hardPercent": 25},
        {"name": "Heat, Acids-Bases-Salts and Electric Current", "weightagePercent": 50, "easyPercent": 35, "mediumPercent": 40, "hardPercent": 25}
      ]
    },
    {
      "name": "Social Science",
      "chapters": [
        {"name": "History (Delhi Sultanate to Mughals) and Geography (Environment)", "weightagePercent": 50, "easyPercent": 35, "mediumPercent": 40, "hardPercent": 25},
        {"name": "Civics (Equality, State Government and Media)", "weightagePercent": 50, "easyPercent": 35, "mediumPercent": 40, "hardPercent": 25}
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
