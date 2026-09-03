-- supabase/sql/exam_patterns_class1.sql
--
-- Seeds 'Class 1' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). This is the first of the "Schools"
-- category exams (see ExamCategory.SCHOOLS in FullTestModels.kt) -- general CBSE-aligned grade-1
-- practice, not tied to any specific board exam. Pitched at early-primary level: no negative
-- marking, MCQ only (numerical/descriptive formats aren't age-appropriate at this grade), and
-- difficulty skewed heavily toward easy.
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 1 general-knowledge practice test, not an official CBSE publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 1',
  30,
  30,
  30,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "English",
      "chapters": [
        {"name": "Alphabets and Phonics", "weightagePercent": 35, "easyPercent": 70, "mediumPercent": 25, "hardPercent": 5},
        {"name": "Vowels, Consonants and Simple Words", "weightagePercent": 35, "easyPercent": 70, "mediumPercent": 25, "hardPercent": 5},
        {"name": "Rhyming Words, Opposites and Picture Reading", "weightagePercent": 30, "easyPercent": 70, "mediumPercent": 25, "hardPercent": 5}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Numbers 1 to 100 (Counting, Before-After-Between)", "weightagePercent": 35, "easyPercent": 70, "mediumPercent": 25, "hardPercent": 5},
        {"name": "Addition and Subtraction (up to 20)", "weightagePercent": 35, "easyPercent": 70, "mediumPercent": 25, "hardPercent": 5},
        {"name": "Shapes, Patterns and Measurement", "weightagePercent": 30, "easyPercent": 70, "mediumPercent": 25, "hardPercent": 5}
      ]
    },
    {
      "name": "EVS",
      "chapters": [
        {"name": "Myself, My Family and My Body", "weightagePercent": 35, "easyPercent": 70, "mediumPercent": 25, "hardPercent": 5},
        {"name": "Plants and Animals Around Us", "weightagePercent": 35, "easyPercent": 70, "mediumPercent": 25, "hardPercent": 5},
        {"name": "Good Habits, Safety and Our Surroundings", "weightagePercent": 30, "easyPercent": 70, "mediumPercent": 25, "hardPercent": 5}
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
