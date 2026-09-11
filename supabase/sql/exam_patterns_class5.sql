-- supabase/sql/exam_patterns_class5.sql
--
-- Seeds 'Class 5' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). This continues the "Schools" category
-- exams (see ExamCategory.SCHOOLS in FullTestModels.kt) -- general CBSE-aligned grade-5
-- practice, not tied to any specific board exam. Pitched one notch up from Class 4 (see
-- exam_patterns_class4.sql): no negative marking, MCQ only (numerical/descriptive formats
-- aren't age-appropriate at this grade), a slightly larger medium/hard share, active-passive
-- voice and punctuation in English, decimals/percentages/area-volume in Maths, and ecosystems/
-- geography/government in EVS -- the last grade before EVS splits into Science and Social
-- Science from Class 6 onward.
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 5 general-knowledge practice test, not an official CBSE publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 5',
  30,
  30,
  40,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "English",
      "chapters": [
        {"name": "Articles, Prepositions and Punctuation", "weightagePercent": 35, "easyPercent": 50, "mediumPercent": 38, "hardPercent": 12},
        {"name": "Active-Passive Voice and Direct-Indirect Speech", "weightagePercent": 35, "easyPercent": 45, "mediumPercent": 40, "hardPercent": 15},
        {"name": "Reading Comprehension and Vocabulary", "weightagePercent": 30, "easyPercent": 50, "mediumPercent": 38, "hardPercent": 12}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Large Numbers and Roman Numerals", "weightagePercent": 35, "easyPercent": 50, "mediumPercent": 38, "hardPercent": 12},
        {"name": "Fractions, Decimals and Percentages", "weightagePercent": 35, "easyPercent": 45, "mediumPercent": 40, "hardPercent": 15},
        {"name": "Perimeter, Area, Volume and Data Handling", "weightagePercent": 30, "easyPercent": 45, "mediumPercent": 40, "hardPercent": 15}
      ]
    },
    {
      "name": "EVS",
      "chapters": [
        {"name": "Human Body Systems and Health", "weightagePercent": 35, "easyPercent": 50, "mediumPercent": 38, "hardPercent": 12},
        {"name": "Ecosystems, Water Cycle and Environment", "weightagePercent": 35, "easyPercent": 50, "mediumPercent": 38, "hardPercent": 12},
        {"name": "Indian Geography, Government and Freedom Struggle", "weightagePercent": 30, "easyPercent": 45, "mediumPercent": 40, "hardPercent": 15}
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
