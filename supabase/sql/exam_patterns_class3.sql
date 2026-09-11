-- supabase/sql/exam_patterns_class3.sql
--
-- Seeds 'Class 3' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). This continues the "Schools" category
-- exams (see ExamCategory.SCHOOLS in FullTestModels.kt) -- general CBSE-aligned grade-3
-- practice, not tied to any specific board exam. Pitched one notch up from Class 2 (see
-- exam_patterns_class2.sql): no negative marking, MCQ only (numerical/descriptive formats
-- aren't age-appropriate at this grade), still easy-skewed but with a somewhat larger medium
-- share, introducing multiplication tables, 4-digit numbers, and slightly broader EVS topics.
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 3 general-knowledge practice test, not an official CBSE publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 3',
  30,
  30,
  35,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "English",
      "chapters": [
        {"name": "Nouns (Common, Proper, Collective) and Pronouns", "weightagePercent": 35, "easyPercent": 60, "mediumPercent": 35, "hardPercent": 5},
        {"name": "Adjectives, Rhyming Words and Opposites", "weightagePercent": 35, "easyPercent": 60, "mediumPercent": 35, "hardPercent": 5},
        {"name": "Reading Comprehension and Vocabulary", "weightagePercent": 30, "easyPercent": 60, "mediumPercent": 35, "hardPercent": 5}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Numbers up to 10000 (Place Value, Before-After-Between)", "weightagePercent": 35, "easyPercent": 60, "mediumPercent": 35, "hardPercent": 5},
        {"name": "Addition, Subtraction and Multiplication Tables", "weightagePercent": 35, "easyPercent": 55, "mediumPercent": 40, "hardPercent": 5},
        {"name": "Shapes, Measurement, Time and Money", "weightagePercent": 30, "easyPercent": 60, "mediumPercent": 35, "hardPercent": 5}
      ]
    },
    {
      "name": "EVS",
      "chapters": [
        {"name": "Our Body, Food and Family", "weightagePercent": 35, "easyPercent": 60, "mediumPercent": 35, "hardPercent": 5},
        {"name": "Plants, Animals and Habitats", "weightagePercent": 35, "easyPercent": 60, "mediumPercent": 35, "hardPercent": 5},
        {"name": "Our Community, Transport and Communication", "weightagePercent": 30, "easyPercent": 60, "mediumPercent": 35, "hardPercent": 5}
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
