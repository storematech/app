-- supabase/sql/exam_patterns_class11_commerce.sql
--
-- Seeds 'Class 11 Commerce' into exam_patterns (see exam_patterns.sql for the table itself and
-- how generate-full-test's research phase checks it first). Second of the three Class 11 stream
-- entries (see exam_patterns_class11_science.sql for the rationale on splitting by stream, and
-- exam_patterns_class11_arts.sql for the third). Models Accountancy, Business Studies,
-- Economics, and English. Still no negative marking and MCQ only (numerical/descriptive formats
-- aren't the primary assessment style tested here), with the same senior-secondary hard share as
-- the Science stream.
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 11 Commerce general-knowledge practice test, not an official CBSE
-- publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 11 Commerce',
  40,
  40,
  60,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "Accountancy",
      "chapters": [
        {"name": "Basic Accounting Concepts and Journal-Ledger", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45},
        {"name": "Trial Balance, Bank Reconciliation and Depreciation", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45}
      ]
    },
    {
      "name": "Business Studies",
      "chapters": [
        {"name": "Nature of Business and Forms of Business Organisation", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45},
        {"name": "Business Services, Finance Sources and Small Business", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45}
      ]
    },
    {
      "name": "Economics",
      "chapters": [
        {"name": "Introduction to Economics and Statistics for Economics", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45},
        {"name": "Collection, Organisation and Presentation of Data", "weightagePercent": 50, "easyPercent": 20, "mediumPercent": 35, "hardPercent": 45}
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
