-- supabase/sql/exam_patterns_upsc_epfo.sql
--
-- Seeds 'UPSC EPFO' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). UPSC EPFO refers to the Enforcement
-- Officer/Accounts Officer (EO/AO) recruitment exam run by UPSC for the Employees' Provident Fund
-- Organisation -- a completely different exam from UPSC CSE (IAS): it's a single-stage objective
-- Recruitment Test followed by an interview, with a syllabus blending general aptitude/GK with
-- accounting and labour-law knowledge specific to the EPFO role.
--
-- Real UPSC EPFO (EO/AO) structure: a single Computer Based Recruitment Test, 120 single-best-
-- response MCQs (4 options, exactly one correct), 2.5 marks each (300 total), -1/3rd mark (-0.83)
-- for a wrong answer, 120 minutes, followed by a separate (non-MCQ) interview stage not modeled
-- here. Treat these numbers, and the subject/chapter weightages below, as a reasonable curated
-- starting point rather than an official UPSC publication -- topic-wise question counts vary by
-- year.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'UPSC EPFO',
  120,
  300,
  120,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 2.5, "incorrectMarks": -0.83}'::jsonb,
  '[
    {
      "name": "General English",
      "chapters": [
        {"name": "Grammar and Usage", "weightagePercent": 55, "easyPercent": 30, "mediumPercent": 55, "hardPercent": 15},
        {"name": "Vocabulary and Comprehension", "weightagePercent": 45, "easyPercent": 30, "mediumPercent": 55, "hardPercent": 15}
      ]
    },
    {
      "name": "General Knowledge and Social Studies",
      "chapters": [
        {"name": "Indian Freedom Struggle and Modern History", "weightagePercent": 25, "easyPercent": 25, "mediumPercent": 55, "hardPercent": 20},
        {"name": "Indian Polity and Governance", "weightagePercent": 30, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25},
        {"name": "Indian Economy", "weightagePercent": 25, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25},
        {"name": "Current Affairs and General Knowledge", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25}
      ]
    },
    {
      "name": "General Accounting Principles",
      "chapters": [
        {"name": "Basic Accounting Concepts", "weightagePercent": 55, "easyPercent": 25, "mediumPercent": 55, "hardPercent": 20},
        {"name": "Financial Statements and Analysis", "weightagePercent": 45, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25}
      ]
    },
    {
      "name": "Industrial Relations and Labour Laws",
      "chapters": [
        {"name": "Industrial Relations", "weightagePercent": 45, "easyPercent": 25, "mediumPercent": 55, "hardPercent": 20},
        {"name": "Labour Laws and Social Security", "weightagePercent": 55, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25}
      ]
    },
    {
      "name": "General Science and Computer Applications",
      "chapters": [
        {"name": "General Science", "weightagePercent": 50, "easyPercent": 30, "mediumPercent": 55, "hardPercent": 15},
        {"name": "Computer Applications and IT Awareness", "weightagePercent": 50, "easyPercent": 30, "mediumPercent": 55, "hardPercent": 15}
      ]
    },
    {
      "name": "Quantitative Aptitude and Reasoning",
      "chapters": [
        {"name": "Quantitative Aptitude - Arithmetic", "weightagePercent": 30, "easyPercent": 25, "mediumPercent": 55, "hardPercent": 20},
        {"name": "Quantitative Aptitude - Algebra and Data Interpretation", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Logical and Analytical Reasoning", "weightagePercent": 25, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Verbal and Non-verbal Reasoning", "weightagePercent": 25, "easyPercent": 25, "mediumPercent": 55, "hardPercent": 20}
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
