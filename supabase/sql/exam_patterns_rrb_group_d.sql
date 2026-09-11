-- supabase/sql/exam_patterns_rrb_group_d.sql
--
-- Seeds 'RRB Group D' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). RRB Group D (Level-1 posts) recruits for
-- Track Maintainer, Helper/Assistant, Pointsman, and similar entry-level technical/support roles
-- across Indian Railways. This seed models the single-stage Computer Based Test (CBT) -- the only
-- MCQ stage; the Physical Efficiency Test (PET) that follows for shortlisted candidates isn't
-- MCQ-based.
--
-- Real RRB Group D CBT structure: 100 single-best-response MCQs (4 options, exactly one
-- correct), 1 mark each (100 total), -1/3 (0.33) for a wrong answer, 90 minutes -- Mathematics
-- (25 Q), General Intelligence and Reasoning (30 Q), General Science (25 Q), and General
-- Awareness and Current Affairs (20 Q). Pitched at a 10th-standard level, similar to SSC-GD
-- Constable (see exam_patterns_ssc_gd.sql) -- entry-level recruitment rather than a clerical or
-- graduate-level exam, but with a distinct, heavier General Science share reflecting the
-- technical/track-maintenance nature of these posts. Treat these numbers, and the chapter
-- weightages below, as a reasonable curated starting point rather than an official Railway
-- Recruitment Board publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'RRB Group D',
  100,
  100,
  90,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 1, "incorrectMarks": -0.33}'::jsonb,
  '[
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Number System and Simplification", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Percentage and Ratio", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Profit Loss and Simple Interest", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Mensuration and Time Work", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15}
      ]
    },
    {
      "name": "General Intelligence and Reasoning",
      "chapters": [
        {"name": "Analogies and Classification", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Series and Coding-Decoding", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Non-Verbal Reasoning", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Blood Relations and Direction Sense", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 45, "hardPercent": 20}
      ]
    },
    {
      "name": "General Science",
      "chapters": [
        {"name": "Physics Basics", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Chemistry Basics", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Biology Basics", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Everyday Science and Technology", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10}
      ]
    },
    {
      "name": "General Awareness and Current Affairs",
      "chapters": [
        {"name": "Indian History and Culture", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Indian Polity and Geography", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Current Affairs and Sports", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Railways General Knowledge", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10}
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
