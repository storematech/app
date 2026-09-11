-- supabase/sql/exam_patterns_ssc_gd.sql
--
-- Seeds 'SSC-GD Constable' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). SSC GD Constable recruits for the
-- General Duty constable posts in CAPFs (BSF, CRPF, CISF, SSB, ITBP, etc.), Assam Rifles, and
-- SSF. This seed models the single-stage Computer Based Test (CBT) -- the only MCQ stage; the
-- Physical Efficiency/Standard Tests (PET/PST) and detailed medical exam that follow aren't
-- MCQ-based.
--
-- Real SSC GD Constable CBT structure: 80 single-best-response MCQs (4 options, exactly one
-- correct), 20 questions each from General Intelligence and Reasoning, General Knowledge and
-- General Awareness, Elementary Mathematics, and English/Hindi, 2 marks each (160 total), -0.25
-- for a wrong answer, 60 minutes. Pitched at a 10th-standard level, similar to SSC-MTS (see
-- exam_patterns_ssc_mts.sql) -- entry-level, physically-oriented recruitment rather than a
-- graduate-level clerical exam. Treat these numbers, and the chapter weightages below, as a
-- reasonable curated starting point rather than an official SSC publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'SSC-GD Constable',
  80,
  160,
  60,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 2, "incorrectMarks": -0.25}'::jsonb,
  '[
    {
      "name": "General Intelligence and Reasoning",
      "chapters": [
        {"name": "Analogies and Classification", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Series and Coding-Decoding", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Blood Relations and Direction Sense", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Non-Verbal Reasoning", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15}
      ]
    },
    {
      "name": "General Knowledge and General Awareness",
      "chapters": [
        {"name": "Indian History and Culture", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Indian Polity and Geography", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "General Science", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Current Affairs and Sports", "weightagePercent": 25, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10}
      ]
    },
    {
      "name": "Elementary Mathematics",
      "chapters": [
        {"name": "Number System and Simplification", "weightagePercent": 30, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10},
        {"name": "Percentage and Ratio", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Profit Loss and Simple Interest", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Mensuration and Basic Geometry", "weightagePercent": 20, "easyPercent": 45, "mediumPercent": 45, "hardPercent": 10}
      ]
    },
    {
      "name": "English",
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
