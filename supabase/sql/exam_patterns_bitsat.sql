-- supabase/sql/exam_patterns_bitsat.sql
--
-- Seeds 'BITSAT' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). BITSAT (Birla Institute of Technology and
-- Science Admission Test) is a computer-based engineering entrance exam, distinct from both JEE
-- and NEET: it adds English Proficiency and Logical Reasoning sections alongside Physics,
-- Chemistry, and Mathematics.
--
-- Real BITSAT structure: single-best-response MCQs (4 options, exactly one correct -- no
-- multi-correct or numerical-answer format), +3 for a correct answer, -1 for an incorrect one, no
-- marks for unattempted, 3 hours, computer-based (this seed does not model the optional 12 bonus
-- questions offered if a candidate finishes early). Treat these numbers, and the subject/chapter
-- weightages below, as a reasonable curated starting point rather than an official BITS Pilani
-- publication -- actual section sizes have varied slightly by year.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'BITSAT',
  130,
  390,
  180,
  '["mcq"]'::jsonb,
  '{"enabled": true, "correctMarks": 3, "incorrectMarks": -1}'::jsonb,
  '[
    {
      "name": "Physics",
      "chapters": [
        {"name": "Mechanics", "weightagePercent": 30, "easyPercent": 25, "mediumPercent": 55, "hardPercent": 20},
        {"name": "Electrodynamics", "weightagePercent": 30, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Heat, Thermodynamics and Waves", "weightagePercent": 20, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Optics and Modern Physics", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25}
      ]
    },
    {
      "name": "Chemistry",
      "chapters": [
        {"name": "Physical Chemistry", "weightagePercent": 35, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Organic Chemistry", "weightagePercent": 35, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Inorganic Chemistry", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "States of Matter and Environmental Chemistry", "weightagePercent": 5, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Algebra and Trigonometry", "weightagePercent": 30, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Calculus", "weightagePercent": 35, "easyPercent": 20, "mediumPercent": 50, "hardPercent": 30},
        {"name": "Coordinate Geometry", "weightagePercent": 20, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Vectors, 3D Geometry and Statistics", "weightagePercent": 15, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25}
      ]
    },
    {
      "name": "English Proficiency",
      "chapters": [
        {"name": "Grammar and Usage", "weightagePercent": 60, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Vocabulary and Reading Comprehension", "weightagePercent": 40, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15}
      ]
    },
    {
      "name": "Logical Reasoning",
      "chapters": [
        {"name": "Verbal Reasoning", "weightagePercent": 50, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Nonverbal and Analytical Reasoning", "weightagePercent": 50, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20}
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
