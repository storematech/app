-- supabase/sql/exam_patterns_rrb_alp.sql
--
-- Seeds 'RRB ALP' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). RRB ALP (Assistant Loco Pilot) recruits
-- trainee loco pilots and technicians across Indian Railways. This seed models CBT-1 -- the
-- common first-stage objective test every candidate takes regardless of trade -- since CBT-2
-- (Part B is trade-specific and qualifying only) and the subsequent Aptitude Test aren't MCQ-based
-- in the same common way.
--
-- Real RRB ALP CBT-1 structure: 75 single-best-response MCQs (4 options, exactly one correct),
-- 1 mark each (75 total), -1/3 (0.33) for a wrong answer, 60 minutes -- Mathematics (20 Q),
-- General Intelligence and Reasoning (25 Q, the largest single share), General Science (20 Q),
-- and General Awareness on Current Affairs (10 Q). Similar in spirit to RRB NTPC and RRB Group D
-- (see their exam_patterns files) but with reasoning weighted higher and current affairs lower,
-- reflecting ALP's own real weightage. Treat these numbers, and the chapter weightages below, as
-- a reasonable curated starting point rather than an official Railway Recruitment Board
-- publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'RRB ALP',
  75,
  75,
  60,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 1, "incorrectMarks": -0.33}'::jsonb,
  '[
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Number System and Simplification", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Percentage and Ratio", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Profit Loss and Interest", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Time Speed Distance and Mensuration", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15}
      ]
    },
    {
      "name": "General Intelligence and Reasoning",
      "chapters": [
        {"name": "Analogies and Classification", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Series and Coding-Decoding", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Non-Verbal Reasoning", "weightagePercent": 25, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Blood Relations and Direction Sense", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
      ]
    },
    {
      "name": "General Science",
      "chapters": [
        {"name": "Physics Basics", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Chemistry Basics", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Biology Basics", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Everyday Science and Technology", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15}
      ]
    },
    {
      "name": "General Awareness on Current Affairs",
      "chapters": [
        {"name": "Indian History and Culture", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Indian Polity and Geography", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Current Affairs and Sports", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Railways General Knowledge", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15}
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
