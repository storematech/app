alter table public.quizzes
  add column if not exists is_offline_exam boolean not null default false;
