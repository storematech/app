-- supabase/sql/exam_patterns_ssc_mts.sql
--
-- Seeds 'SSC-MTS' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). SSC MTS (Multi-Tasking Staff, Group C)
-- recruits for lower-level clerical/support government posts. Its syllabus is pitched at a 10th
-- standard level -- notably easier and less technical than SSC-CGL (see exam_patterns_ssc_cgl.sql).
--
-- Real SSC MTS structure: a single Computer Based Exam run in two back-to-back sessions on exam
-- day -- Session I (Numerical Aptitude, 20 Q; Reasoning Ability & Problem Solving, 20 Q; 45
-- minutes) and Session II (General Awareness, 25 Q; English Language & Comprehension, 25 Q; 45
-- minutes) -- 90 single-best-response MCQs total, 3 marks each (270 total), -1 mark for a wrong
-- answer. Treat these numbers, and the subject/chapter weightages below, as a reasonable curated
-- starting point rather than an official SSC publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'SSC-MTS',
  90,
  270,
  90,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 3, "incorrectMarks": -1}'::jsonb,
  '[
    {
      "name": "Numerical Aptitude",
      "chapters": [
        {"name": "Number System and Simplification", "weightagePercent": 30, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Percentage and Ratio", "weightagePercent": 30, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Profit Loss and Simple/Compound Interest", "weightagePercent": 20, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Mensuration and Basic Geometry", "weightagePercent": 20, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10}
      ]
    },
    {
      "name": "Reasoning Ability and Problem Solving",
      "chapters": [
        {"name": "Analogy and Classification", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Series and Coding-Decoding", "weightagePercent": 30, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Blood Relations and Direction Sense", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Non-Verbal Reasoning", "weightagePercent": 20, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15}
      ]
    },
    {
      "name": "General Awareness",
      "chapters": [
        {"name": "Indian History and Culture", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Indian Polity and Geography", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Indian Economy and General Science", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Current Affairs and Static GK", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10}
      ]
    },
    {
      "name": "English Language and Comprehension",
      "chapters": [
        {"name": "Grammar and Error Spotting", "weightagePercent": 30, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Vocabulary (Synonyms, Antonyms, Spelling)", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Fill in the Blanks and Cloze Test", "weightagePercent": 20, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Reading Comprehension", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15}
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
