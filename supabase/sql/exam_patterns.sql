-- supabase/sql/exam_patterns.sql
--
-- Cache/curated-data table for the Full Test feature's "research" phase (generate-full-test's
-- runResearch). Research asks an AI to describe an exam's real structure (question count, marks,
-- duration, negative marking, subject -> chapter weightage/difficulty) — that's stable information
-- that almost never changes exam-to-exam call, so paying an AI call for it every single time is
-- pure waste and, per the "high demand" investigation, also the thing quietly competing with
-- Full Test's own "generate" phase for the same shared provider rate limits.
--
-- The lookup order in generate-full-test is now: check this table first (by exam_name, case-
-- insensitive) — instant, free, zero AI/rate-limit exposure. Only if the exam isn't here yet does
-- it fall back to a live AI research call, and that result is then cached back into this table
-- (source = 'ai_generated') so the SAME exam is never AI-researched twice. 'curated' rows (like
-- JEE Main below) are ones we've hand-verified against the real exam pattern and are never
-- overwritten by an AI cache-write.
--
-- Run this once in the Supabase SQL Editor. Safe to re-run (create table if not exists).

create table if not exists exam_patterns (
  id uuid primary key default gen_random_uuid(),
  exam_name text not null unique,
  total_questions integer not null,
  total_marks integer not null,
  duration_minutes integer not null,
  question_types jsonb not null default '[]'::jsonb,
  -- { "enabled": boolean, "correctMarks": number, "incorrectMarks": number } | null
  negative_marking jsonb,
  -- [{ "name": string, "chapters": [{ "name", "weightagePercent", "easyPercent", "mediumPercent", "hardPercent" }] }]
  subjects jsonb not null,
  source text not null default 'curated' check (source in ('curated', 'ai_generated')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- Lookups are case-insensitive (users/AI won't always type/return the exact same casing).
create index if not exists exam_patterns_exam_name_lower_idx on exam_patterns (lower(exam_name));

-- No policies on purpose — only ever read/written by generate-full-test via the service-role key,
-- same reasoning as ai_generation_log/full_test_generation_log.
alter table exam_patterns enable row level security;

-- ---- Seed data: JEE Main (hand-curated, not AI-generated) --------------------------------------
-- JEE Main (Paper 1, B.E./B.Tech): 3 subjects, each 20 MCQs (Section A) + 10 numerical-value
-- questions (Section B) of which only 5 must be attempted -> 75 attempted questions total.
-- +4 for every correct answer; -1 for a wrong MCQ, no negative marking on numerical questions.
-- 300 total marks, 180 minutes. Chapter weightages below are approximate, based on well-known
-- JEE Main pattern analysis (not an official NTA publication) — a reasonable curated starting
-- point, same spirit as what the AI research phase would otherwise estimate on its own.
insert into exam_patterns (exam_name, total_questions, total_marks, duration_minutes, question_types, negative_marking, subjects, source)
values (
  'JEE Main',
  75,
  300,
  180,
  '["mcq", "numerical"]'::jsonb,
  '{"enabled": true, "correctMarks": 4, "incorrectMarks": -1}'::jsonb,
  '[
    {
      "name": "Physics",
      "chapters": [
        {"name": "Mechanics (Kinematics, Laws of Motion, Work-Energy-Power, Rotational Motion, Gravitation)", "weightagePercent": 25, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Electrodynamics (Electrostatics, Current Electricity, Magnetism, EMI, AC)", "weightagePercent": 25, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Modern Physics (Atoms, Nuclei, Dual Nature of Matter, Semiconductors)", "weightagePercent": 15, "easyPercent": 30, "mediumPercent": 45, "hardPercent": 25},
        {"name": "Heat and Thermodynamics", "weightagePercent": 10, "easyPercent": 35, "mediumPercent": 45, "hardPercent": 20},
        {"name": "Optics (Ray and Wave)", "weightagePercent": 10, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Waves and Oscillations", "weightagePercent": 8, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Properties of Matter and Fluids", "weightagePercent": 7, "easyPercent": 35, "mediumPercent": 45, "hardPercent": 20}
      ]
    },
    {
      "name": "Chemistry",
      "chapters": [
        {"name": "Physical Chemistry (Mole Concept, Thermodynamics, Equilibrium, Electrochemistry, Chemical Kinetics, Solutions)", "weightagePercent": 35, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Organic Chemistry (GOC, Hydrocarbons, Alcohols-Phenols-Ethers, Carbonyl Compounds, Amines, Biomolecules, Polymers)", "weightagePercent": 35, "easyPercent": 30, "mediumPercent": 45, "hardPercent": 25},
        {"name": "Inorganic Chemistry (Periodic Table, Chemical Bonding, Coordination Compounds, p/d/f-Block Elements, Metallurgy)", "weightagePercent": 30, "easyPercent": 40, "mediumPercent": 45, "hardPercent": 15}
      ]
    },
    {
      "name": "Mathematics",
      "chapters": [
        {"name": "Algebra (Complex Numbers, Quadratic Equations, Sequences and Series, Permutations-Combinations, Binomial Theorem, Matrices and Determinants)", "weightagePercent": 30, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Calculus (Limits-Continuity-Differentiability, Application of Derivatives, Integration, Differential Equations)", "weightagePercent": 30, "easyPercent": 20, "mediumPercent": 50, "hardPercent": 30},
        {"name": "Coordinate Geometry (Straight Lines, Circles, Conic Sections)", "weightagePercent": 15, "easyPercent": 25, "mediumPercent": 50, "hardPercent": 25},
        {"name": "Trigonometry", "weightagePercent": 8, "easyPercent": 35, "mediumPercent": 50, "hardPercent": 15},
        {"name": "Vectors and 3D Geometry", "weightagePercent": 10, "easyPercent": 30, "mediumPercent": 50, "hardPercent": 20},
        {"name": "Statistics and Probability", "weightagePercent": 7, "easyPercent": 35, "mediumPercent": 45, "hardPercent": 20}
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
