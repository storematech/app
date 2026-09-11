-- supabase/sql/exam_patterns_class12_arts.sql
--
-- Seeds 'Class 12 Arts' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). Third of the three Class 12 stream
-- entries (see exam_patterns_class12_science.sql for the rationale, and
-- exam_patterns_class12_commerce.sql for the second). Models History, Political Science,
-- Geography, and English at final-board-year rigor -- Mauryan-Gupta empires and colonialism/
-- nationalism in History, the Cold War era and post-independence Indian politics in Political
-- Science, and human geography/India's people-economy in Geography. Still no negative marking
-- and MCQ only (numerical/descriptive formats aren't the primary assessment style tested here).
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 12 Arts general-knowledge practice test, not an official CBSE publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 12 Arts',
  40,
  40,
  60,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "History",
      "chapters": [
        {"name": "Mauryan-Gupta Empires and Mughal Court Culture", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50},
        {"name": "Colonialism and Indian Nationalism (Advanced)", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50}
      ]
    },
    {
      "name": "Political Science",
      "chapters": [
        {"name": "The Cold War Era and Contemporary World Politics", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50},
        {"name": "Indian Politics Since Independence (Nation-Building)", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50}
      ]
    },
    {
      "name": "Geography",
      "chapters": [
        {"name": "Human Geography: Population and Human Settlements", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50},
        {"name": "India: People, Resources, Agriculture and Industries", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50}
      ]
    },
    {
      "name": "English",
      "chapters": [
        {"name": "Advanced Grammar (Modals, Voice) and Note-Making", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50},
        {"name": "Reading Comprehension and Vocabulary", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50}
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
