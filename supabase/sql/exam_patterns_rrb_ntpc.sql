-- supabase/sql/exam_patterns_rrb_ntpc.sql
--
-- Seeds 'RRB NTPC' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). RRB NTPC (Non-Technical Popular
-- Categories) recruits for clerical, station master, and similar non-technical posts across
-- Indian Railways. This seed models CBT-1 -- the common first-stage objective test every
-- candidate takes, regardless of the specific post applied for -- since CBT-2 (post-specific,
-- more advanced) and the Typing Skill Test/Aptitude Test that follow for some posts aren't
-- MCQ-based in the same common way.
--
-- Real RRB NTPC CBT-1 structure: 100 single-best-response MCQs (4 options, exactly one correct),
-- 1 mark each (100 total), -1/3 (0.33) for a wrong answer, 90 minutes -- Mathematics (30 Q),
-- General Intelligence and Reasoning (30 Q), and General Awareness (40 Q). Treat these numbers,
-- and the chapter weightages below, as a reasonable curated starting point rather than an
-- official Railway Recruitment Board publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'RRB NTPC',
  100,
  100,
  90,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 1, "incorrectMarks": -0.33}'::jsonb,
  '[
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Number System and Simplification", "weightagePercent": 20, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Percentage and Ratio", "weightagePercent": 20, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Profit Loss and Simple/Compound Interest", "weightagePercent": 20, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Time Speed Distance and Time and Work", "weightagePercent": 20, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Mensuration and Geometry", "weightagePercent": 20, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15}
      ]
    },
    {
      "name": "General Intelligence and Reasoning",
      "chapters": [
        {"name": "Analogies and Classification", "weightagePercent": 20, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Series and Coding-Decoding", "weightagePercent": 20, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Non-Verbal Reasoning", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Blood Relations and Direction Sense", "weightagePercent": 20, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Syllogism and Statement-Conclusion", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25}
      ]
    },
    {
      "name": "General Awareness",
      "chapters": [
        {"name": "Indian History and Culture", "weightagePercent": 17, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Indian Polity and Geography", "weightagePercent": 17, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "General Science", "weightagePercent": 17, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Current Affairs and Sports", "weightagePercent": 17, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Indian Economy", "weightagePercent": 16, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Railways General Knowledge", "weightagePercent": 16, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15}
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
