-- supabase/sql/exam_patterns_class4.sql
--
-- Seeds 'Class 4' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). This continues the "Schools" category
-- exams (see ExamCategory.SCHOOLS in FullTestModels.kt) -- general CBSE-aligned grade-4
-- practice, not tied to any specific board exam. Pitched one notch up from Class 3 (see
-- exam_patterns_class3.sql): no negative marking, MCQ only (numerical/descriptive formats
-- aren't age-appropriate at this grade), still easy-skewed but introducing a small hard share
-- for the first time, tenses/adverbs in English, fractions and 5-digit numbers in Maths, and
-- broader civics/heritage topics in EVS.
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 4 general-knowledge practice test, not an official CBSE publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 4',
  30,
  30,
  35,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "English",
      "chapters": [
        {"name": "Tenses (Present, Past, Future) and Types of Sentences", "weightagePercent": 35, "easyPercent": 55, "mediumPercent": 35, "hardPercent": 10},
        {"name": "Adverbs, Conjunctions and Prefixes-Suffixes", "weightagePercent": 35, "easyPercent": 55, "mediumPercent": 35, "hardPercent": 10},
        {"name": "Reading Comprehension and Vocabulary", "weightagePercent": 30, "easyPercent": 55, "mediumPercent": 35, "hardPercent": 10}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Numbers up to 1,00,000 (Place Value and Rounding)", "weightagePercent": 35, "easyPercent": 55, "mediumPercent": 35, "hardPercent": 10},
        {"name": "Multiplication, Division, Factors and Multiples", "weightagePercent": 35, "easyPercent": 50, "mediumPercent": 40, "hardPercent": 10},
        {"name": "Fractions, Measurement and Geometry", "weightagePercent": 30, "easyPercent": 50, "mediumPercent": 40, "hardPercent": 10}
      ]
    },
    {
      "name": "EVS",
      "chapters": [
        {"name": "Food, Digestion and Health", "weightagePercent": 35, "easyPercent": 55, "mediumPercent": 35, "hardPercent": 10},
        {"name": "Adaptations, Habitats and Natural Resources", "weightagePercent": 35, "easyPercent": 55, "mediumPercent": 35, "hardPercent": 10},
        {"name": "Our State, Government and Heritage", "weightagePercent": 30, "easyPercent": 55, "mediumPercent": 35, "hardPercent": 10}
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
