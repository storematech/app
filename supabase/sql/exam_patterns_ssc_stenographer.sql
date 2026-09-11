-- supabase/sql/exam_patterns_ssc_stenographer.sql
--
-- Seeds 'SSC Stenographer' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). SSC Stenographer (Grade C and D) recruits
-- stenographers for central government ministries/departments. This seed models the objective
-- Computer Based Examination -- the only MCQ stage; the Skill Test (shorthand dictation and
-- transcription) that follows isn't MCQ-based.
--
-- Real SSC Stenographer CBE structure: 200 single-best-response MCQs (4 options, exactly one
-- correct), 1 mark each (200 total), -0.25 for a wrong answer, 120 minutes -- 50 questions each
-- from General Intelligence and Reasoning and General Awareness, and 100 questions (half the
-- paper) from English Language and Comprehension, reflecting how central the role's English
-- proficiency requirement is. Treat these numbers, and the chapter weightages below, as a
-- reasonable curated starting point rather than an official SSC publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'SSC Stenographer',
  200,
  200,
  120,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 1, "incorrectMarks": -0.25}'::jsonb,
  '[
    {
      "name": "General Intelligence and Reasoning",
      "chapters": [
        {"name": "Analogies and Classification", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Series and Coding-Decoding", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Non-Verbal Reasoning", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Blood Relations and Direction Sense", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
      ]
    },
    {
      "name": "General Awareness",
      "chapters": [
        {"name": "Indian History and Culture", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Indian Polity and Geography", "weightagePercent": 25, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "General Science", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Current Affairs and Static GK", "weightagePercent": 25, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15}
      ]
    },
    {
      "name": "English Language and Comprehension",
      "chapters": [
        {"name": "Grammar and Error Spotting", "weightagePercent": 15, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Vocabulary (Synonyms, Antonyms, Spelling)", "weightagePercent": 12, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Cloze Test and Fill in the Blanks", "weightagePercent": 12, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Reading Comprehension", "weightagePercent": 13, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Sentence Improvement and Rearrangement", "weightagePercent": 13, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "One-Word Substitution", "weightagePercent": 12, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Idioms and Phrases", "weightagePercent": 11, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15},
        {"name": "Active-Passive and Direct-Indirect Speech", "weightagePercent": 12, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
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
