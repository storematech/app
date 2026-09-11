-- supabase/sql/exam_patterns_class2.sql
--
-- Seeds 'Class 2' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). This continues the "Schools" category
-- exams (see ExamCategory.SCHOOLS in FullTestModels.kt) -- general CBSE-aligned grade-2
-- practice, not tied to any specific board exam. Pitched at early-primary level, one notch up
-- from Class 1 (see exam_patterns_class1.sql): no negative marking, MCQ only (numerical/
-- descriptive formats aren't age-appropriate at this grade), and difficulty still skewed heavily
-- toward easy, but with a slightly larger medium share than Class 1.
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 2 general-knowledge practice test, not an official CBSE publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 2',
  30,
  30,
  30,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "English",
      "chapters": [
        {"name": "Naming Words, Action Words and Simple Sentences", "weightagePercent": 35, "easyPercent": 65, "mediumPercent": 30, "hardPercent": 5},
        {"name": "Rhyming Words, Opposites and Plurals", "weightagePercent": 35, "easyPercent": 65, "mediumPercent": 30, "hardPercent": 5},
        {"name": "Reading Comprehension and Picture Vocabulary", "weightagePercent": 30, "easyPercent": 65, "mediumPercent": 30, "hardPercent": 5}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Numbers up to 1000 (Place Value, Before-After-Between)", "weightagePercent": 35, "easyPercent": 65, "mediumPercent": 30, "hardPercent": 5},
        {"name": "Addition and Subtraction (up to 3-Digit Numbers)", "weightagePercent": 35, "easyPercent": 65, "mediumPercent": 30, "hardPercent": 5},
        {"name": "Shapes, Patterns, Measurement and Time", "weightagePercent": 30, "easyPercent": 65, "mediumPercent": 30, "hardPercent": 5}
      ]
    },
    {
      "name": "EVS",
      "chapters": [
        {"name": "My Family, Food and Housing", "weightagePercent": 35, "easyPercent": 65, "mediumPercent": 30, "hardPercent": 5},
        {"name": "Plants, Animals and Water", "weightagePercent": 35, "easyPercent": 65, "mediumPercent": 30, "hardPercent": 5},
        {"name": "Our Neighbourhood, Transport and Safety", "weightagePercent": 30, "easyPercent": 65, "mediumPercent": 30, "hardPercent": 5}
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
