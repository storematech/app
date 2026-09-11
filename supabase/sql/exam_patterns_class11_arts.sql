-- supabase/sql/exam_patterns_class11_arts.sql
--
-- Seeds 'Class 11 Arts' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). Third of the three Class 11 stream
-- entries (see exam_patterns_class11_science.sql for the rationale on splitting by stream, and
-- exam_patterns_class11_commerce.sql for the second). Models History, Political Science,
-- Geography, and English. Still no negative marking and MCQ only (numerical/descriptive formats
-- aren't the primary assessment style tested here), with the same senior-secondary hard share as
-- the Science and Commerce streams.
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 11 Arts general-knowledge practice test, not an official CBSE publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 11 Arts',
  40,
  40,
  60,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "History",
      "chapters": [
        {"name": "Early Societies and The Roman Empire (World History)", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45},
        {"name": "Central Islamic Lands and Changing Cultural Traditions", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45}
      ]
    },
    {
      "name": "Political Science",
      "chapters": [
        {"name": "Constitution: Why and How, and Rights in the Indian Constitution", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45},
        {"name": "Elections, Legislature, Executive and Judiciary", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45}
      ]
    },
    {
      "name": "Geography",
      "chapters": [
        {"name": "Geography as a Discipline and The Earth''s Interior-Landforms", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45},
        {"name": "Climate, Water (Oceans) and Life on Earth", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45}
      ]
    },
    {
      "name": "English",
      "chapters": [
        {"name": "Grammar (Determiners, Advanced Tenses) and Note-Making", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45},
        {"name": "Reading Comprehension and Vocabulary", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45}
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
