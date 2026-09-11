-- supabase/sql/exam_patterns_class12_commerce.sql
--
-- Seeds 'Class 12 Commerce' into exam_patterns (see exam_patterns.sql for the table itself and
-- how generate-full-test's research phase checks it first). Second of the three Class 12 stream
-- entries (see exam_patterns_class12_science.sql for the rationale, and
-- exam_patterns_class12_arts.sql for the third). Models Accountancy, Business Studies,
-- Economics, and English at final-board-year rigor -- partnership/company accounts and financial
-- statement analysis in Accountancy, management principles/marketing-entrepreneurship in Business
-- Studies, and macroeconomics/government budget-balance of payments in Economics. Still no
-- negative marking and MCQ only (numerical/descriptive formats aren't the primary assessment
-- style tested here).
--
-- Treat these numbers, and the subject/chapter weightages below, as a reasonable curated starting
-- point for a Class 12 Commerce general-knowledge practice test, not an official CBSE
-- publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'Class 12 Commerce',
  40,
  40,
  60,
  '["mcq"]'::jsonb,
  null,
  '[
    {
      "name": "Accountancy",
      "chapters": [
        {"name": "Partnership Accounts (Reconstitution and Dissolution)", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50},
        {"name": "Company Accounts (Shares-Debentures) and Financial Statement Analysis", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50}
      ]
    },
    {
      "name": "Business Studies",
      "chapters": [
        {"name": "Principles and Functions of Management", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50},
        {"name": "Marketing, Consumer Protection and Entrepreneurship", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50}
      ]
    },
    {
      "name": "Economics",
      "chapters": [
        {"name": "Macroeconomics: National Income and Money-Banking", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50},
        {"name": "Government Budget, Balance of Payments and Indian Economic Development", "weightagePercent": 50, "easyPercent": 15, "mediumPercent": 35, "hardPercent": 50}
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
