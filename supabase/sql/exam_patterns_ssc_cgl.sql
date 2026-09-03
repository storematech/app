-- supabase/sql/exam_patterns_ssc_cgl.sql
--
-- Seeds 'SSC-CGL' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). SSC CGL (Staff Selection Commission
-- Combined Graduate Level Examination) recruits for Group B/C government posts. This seed models
-- Tier I -- the objective MCQ stage most commonly practiced -- since Tier II varies by post and
-- Tier III/IV (descriptive writing, skill/typing tests) aren't MCQ-based.
--
-- Real SSC CGL Tier I structure: 100 single-best-response MCQs (4 options, exactly one correct),
-- 25 questions each from General Intelligence & Reasoning, General Awareness, Quantitative
-- Aptitude, and English Comprehension, 2 marks each (200 total), -0.5 for a wrong answer, 60
-- minutes. This is one of the more stable, well-documented exam patterns (unlike some of the
-- other state/institute exams already seeded), but chapter-level weightages below are still a
-- reasonable curated approximation rather than an official SSC publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'SSC-CGL',
  100,
  200,
  60,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 2, "incorrectMarks": -0.5}'::jsonb,
  '[
    {
      "name": "General Intelligence and Reasoning",
      "chapters": [
        {"name": "Verbal Reasoning (Analogies, Classification, Series)", "weightagePercent": 30, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Non-Verbal and Figural Reasoning", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Logical Reasoning (Syllogism, Statements and Conclusions)", "weightagePercent": 25, "easyPercent": 20, "mediumPercent": 55, "hardPercent": 25},
        {"name": "Analytical Reasoning (Coding-Decoding, Blood Relations, Direction Sense)", "weightagePercent": 25, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25}
      ]
    },
    {
      "name": "General Awareness",
      "chapters": [
        {"name": "Indian History and Culture", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 55, "hardPercent": 15},
        {"name": "Indian Polity and Geography", "weightagePercent": 25, "easyPercent": 25, "mediumPercent": 55, "hardPercent": 20},
        {"name": "Indian Economy", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 55, "hardPercent": 20},
        {"name": "General Science and Current Affairs", "weightagePercent": 30, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
      ]
    },
    {
      "name": "Quantitative Aptitude",
      "chapters": [
        {"name": "Arithmetic (Percentage, Profit-Loss, SI-CI, Ratio)", "weightagePercent": 35, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Algebra", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 55, "hardPercent": 20},
        {"name": "Geometry and Mensuration", "weightagePercent": 25, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Trigonometry and Data Interpretation", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 55, "hardPercent": 20}
      ]
    },
    {
      "name": "English Comprehension",
      "chapters": [
        {"name": "Grammar and Error Spotting", "weightagePercent": 30, "easyPercent": 25, "mediumPercent": 55, "hardPercent": 20},
        {"name": "Vocabulary (Synonyms, Antonyms, One-word Substitution)", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 55, "hardPercent": 15},
        {"name": "Cloze Test and Fill in the Blanks", "weightagePercent": 20, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Reading Comprehension and Para Jumbles", "weightagePercent": 25, "easyPercent": 25, "mediumPercent": 55, "hardPercent": 20}
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
