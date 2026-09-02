-- supabase/sql/exam_patterns_jee_advanced.sql
--
-- Seeds 'JEE Advanced' into exam_patterns (see exam_patterns.sql for the table itself and how
-- generate-full-test's research phase checks it first). Same 16 chapters as the existing 'JEE
-- Main' seed (JEE Advanced covers the same core syllabus, just at a harder level), but with a
-- tougher easy/medium/hard split reflecting that.
--
-- JEE Advanced structure note: the real exam runs two papers (Paper 1 and Paper 2, both
-- compulsory) with several distinct question types per paper (single-correct MCQ, multi-correct
-- MCQ, numerical-answer, and occasionally matching-type), each with its own marking scheme. This
-- schema models one combined structure rather than two separate papers -- treat these numbers,
-- like JEE Main's, as a reasonable curated starting point rather than an official NTA/IIT
-- publication.
--
-- Run this once in the Supabase SQL Editor, after exam_patterns.sql.

insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'JEE Advanced',
  54,
  180,
  180,
  '["mcq", "numerical"]'::jsonb,
  '{"enabled": true, "correctMarks": 3, "incorrectMarks": -1}'::jsonb,
  '[
    {
      "name": "Physics",
      "chapters": [
        {"name": "Mechanics (Kinematics, Laws of Motion, Work-Energy-Power, Rotational Motion, Gravitation)", "weightagePercent": 25, "easyPercent": 15, "mediumPercent": 45, "hardPercent": 40},
        {"name": "Electrodynamics (Electrostatics, Current Electricity, Magnetism, EMI, AC)", "weightagePercent": 25, "easyPercent": 10, "mediumPercent": 45, "hardPercent": 45},
        {"name": "Modern Physics (Atoms, Nuclei, Dual Nature of Matter, Semiconductors)", "weightagePercent": 15, "easyPercent": 15, "mediumPercent": 40, "hardPercent": 45},
        {"name": "Heat and Thermodynamics", "weightagePercent": 10, "easyPercent": 15, "mediumPercent": 45, "hardPercent": 40},
        {"name": "Optics (Ray and Wave)", "weightagePercent": 10, "easyPercent": 15, "mediumPercent": 45, "hardPercent": 40},
        {"name": "Waves and Oscillations", "weightagePercent": 8, "easyPercent": 15, "mediumPercent": 45, "hardPercent": 40},
        {"name": "Properties of Matter and Fluids", "weightagePercent": 7, "easyPercent": 20, "mediumPercent": 45, "hardPercent": 35}
      ]
    },
    {
      "name": "Chemistry",
      "chapters": [
        {"name": "Physical Chemistry (Mole Concept, Thermodynamics, Equilibrium, Electrochemistry, Chemical Kinetics, Solutions)", "weightagePercent": 35, "easyPercent": 15, "mediumPercent": 45, "hardPercent": 40},
        {"name": "Organic Chemistry (GOC, Hydrocarbons, Alcohols-Phenols-Ethers, Carbonyl Compounds, Amines, Biomolecules, Polymers)", "weightagePercent": 35, "easyPercent": 15, "mediumPercent": 40, "hardPercent": 45},
        {"name": "Inorganic Chemistry (Periodic Table, Chemical Bonding, Coordination Compounds, p/d/f-Block Elements, Metallurgy)", "weightagePercent": 30, "easyPercent": 20, "mediumPercent": 45, "hardPercent": 35}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Algebra (Complex Numbers, Quadratic Equations, Sequences and Series, Permutations-Combinations, Binomial Theorem, Matrices and Determinants)", "weightagePercent": 30, "easyPercent": 10, "mediumPercent": 45, "hardPercent": 45},
        {"name": "Calculus (Limits-Continuity-Differentiability, Application of Derivatives, Integration, Differential Equations)", "weightagePercent": 30, "easyPercent": 10, "mediumPercent": 40, "hardPercent": 50},
        {"name": "Coordinate Geometry (Straight Lines, Circles, Conic Sections)", "weightagePercent": 15, "easyPercent": 15, "mediumPercent": 45, "hardPercent": 40},
        {"name": "Trigonometry", "weightagePercent": 8, "easyPercent": 15, "mediumPercent": 45, "hardPercent": 40},
        {"name": "Vectors and 3D Geometry", "weightagePercent": 10, "easyPercent": 15, "mediumPercent": 45, "hardPercent": 40},
        {"name": "Statistics and Probability", "weightagePercent": 7, "easyPercent": 15, "mediumPercent": 45, "hardPercent": 40}
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
