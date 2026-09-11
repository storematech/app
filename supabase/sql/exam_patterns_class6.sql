-- supabase/sql/exam_patterns_class6.sql
--
-- Seeds 'Class 6' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). This continues the "Schools" category
-- exams (see ExamCategory.SCHOOLS in FullTestModels.kt) -- general CBSE-aligned grade-6
-- practice, not tied to any specific board exam. Class 6 is where NCERT's combined EVS (used
-- through Class 5, see exam_patterns_class5.sql) splits into separate Science and Social Science
-- subjects, so this pattern moves from 3 subjects to 4: English, Mathematics, Science, and Social
-- Science. Still no negative marking and MCQ only (numerical/descriptive formats aren't
-- age-appropriate here), with a further-increased hard share reflecting middle-school difficulty.
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 6 general-knowledge practice test, not an official CBSE publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 6',
  40,
  40,
  45,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "English",
      "chapters": [
        {"name": "Grammar: Nouns, Pronouns, Tenses and Punctuation", "weightagePercent": 50, "easyPercent": 40, "mediumPercent": 40, "hardPercent": 20},
        {"name": "Reading Comprehension and Vocabulary", "weightagePercent": 50, "easyPercent": 40, "mediumPercent": 40, "hardPercent": 20}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Knowing Numbers, Whole Numbers and Integers", "weightagePercent": 50, "easyPercent": 40, "mediumPercent": 40, "hardPercent": 20},
        {"name": "Fractions, Decimals and Basic Geometry", "weightagePercent": 50, "easyPercent": 40, "mediumPercent": 40, "hardPercent": 20}
      ]
    },
    {
      "name": "Science",
      "chapters": [
        {"name": "Food, Materials and the Living World", "weightagePercent": 50, "easyPercent": 40, "mediumPercent": 40, "hardPercent": 20},
        {"name": "Motion, Measurement, Light and Electricity", "weightagePercent": 50, "easyPercent": 40, "mediumPercent": 40, "hardPercent": 20}
      ]
    },
    {
      "name": "Social Science",
      "chapters": [
        {"name": "History and Geography (Early India, Earth and Maps)", "weightagePercent": 50, "easyPercent": 40, "mediumPercent": 40, "hardPercent": 20},
        {"name": "Civics (Government, Diversity and Local Bodies)", "weightagePercent": 50, "easyPercent": 40, "mediumPercent": 40, "hardPercent": 20}
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
