alter table public.quizzes
  add column if not exists starts_at timestamptz null,
  add column if not exists ends_at timestamptz null;
