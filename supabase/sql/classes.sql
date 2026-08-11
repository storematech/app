-- supabase/sql/classes.sql
--
-- Backs the "Class" feature: a class groups multiple existing quizzes so a teacher can see
-- combined performance instead of checking each quiz separately. The "Class AI Summary" here
-- follows the exact same rule as quiz_ai_summary.sql — it's plain SQL aggregation cached in a
-- table, presented to users with AI branding, no AI/ML call anywhere.
--
-- Run this once in the Supabase SQL Editor. Safe to re-run (create table if not exists,
-- create or replace function/policy would need a drop first — policies below use
-- `drop policy if exists` so this whole script is safely re-runnable).

create table if not exists classes (
  id uuid primary key default gen_random_uuid(),
  created_by uuid not null references auth.users(id) on delete cascade,
  name text not null,
  description text,
  created_at timestamptz not null default now()
);

alter table classes enable row level security;

drop policy if exists "Users manage their own classes" on classes;
create policy "Users manage their own classes"
  on classes for all
  using (auth.uid() = created_by)
  with check (auth.uid() = created_by);

-- Many-to-many: a quiz can belong to more than one class, and linking never duplicates the quiz
-- row itself — it's just a join row.
create table if not exists class_quizzes (
  id uuid primary key default gen_random_uuid(),
  class_id uuid not null references classes(id) on delete cascade,
  quiz_id uuid not null references quizzes(id) on delete cascade,
  created_at timestamptz not null default now(),
  unique (class_id, quiz_id)
);

alter table class_quizzes enable row level security;

drop policy if exists "Users manage links for their own classes" on class_quizzes;
create policy "Users manage links for their own classes"
  on class_quizzes for all
  using (exists (select 1 from classes where classes.id = class_id and classes.created_by = auth.uid()))
  with check (exists (select 1 from classes where classes.id = class_id and classes.created_by = auth.uid()));

create table if not exists class_ai_summaries (
  class_id uuid primary key references classes(id) on delete cascade,
  quiz_count integer not null default 0,
  participant_count integer not null default 0,
  average_score numeric not null default 0,
  best_quiz_title text,
  best_quiz_score numeric,
  worst_quiz_title text,
  worst_quiz_score numeric,
  computed_at timestamptz not null default now()
);

-- No policies on purpose — only ever read/written by the security definer function below,
-- same reasoning as quiz_ai_summaries.
alter table class_ai_summaries enable row level security;

create or replace function get_class_ai_summary(p_class_id uuid)
returns setof class_ai_summaries
language plpgsql
security definer
set search_path = public
as $$
declare
  live_count integer;
  cached class_ai_summaries;
  v_quiz_count integer;
  v_participant_count integer;
  v_average_score numeric;
  v_best_title text;
  v_best_score numeric;
  v_worst_title text;
  v_worst_score numeric;
  result class_ai_summaries;
begin
  if not exists (select 1 from classes where id = p_class_id and created_by = auth.uid()) then
    raise exception 'Not authorized';
  end if;

  select count(*) into live_count
  from quiz_responses r
  join class_quizzes cq on cq.quiz_id = r.quiz_id
  where cq.class_id = p_class_id and r.completed = true;

  select * into cached from class_ai_summaries where class_id = p_class_id;

  -- Same cheap staleness check as get_quiz_ai_summary: if the completed-response count across
  -- this class's quizzes hasn't moved since last computed, the cached row is still accurate.
  if cached.class_id is not null and cached.participant_count = live_count then
    return next cached;
    return;
  end if;

  select count(*) into v_quiz_count from class_quizzes where class_id = p_class_id;

  select count(*), coalesce(avg(r.score), 0)
  into v_participant_count, v_average_score
  from quiz_responses r
  join class_quizzes cq on cq.quiz_id = r.quiz_id
  where cq.class_id = p_class_id and r.completed = true;

  select q.title, per_quiz.avg_score
  into v_best_title, v_best_score
  from (
    select r.quiz_id, avg(r.score) as avg_score
    from quiz_responses r
    join class_quizzes cq on cq.quiz_id = r.quiz_id
    where cq.class_id = p_class_id and r.completed = true
    group by r.quiz_id
  ) per_quiz
  join quizzes q on q.id = per_quiz.quiz_id
  order by per_quiz.avg_score desc
  limit 1;

  select q.title, per_quiz.avg_score
  into v_worst_title, v_worst_score
  from (
    select r.quiz_id, avg(r.score) as avg_score
    from quiz_responses r
    join class_quizzes cq on cq.quiz_id = r.quiz_id
    where cq.class_id = p_class_id and r.completed = true
    group by r.quiz_id
  ) per_quiz
  join quizzes q on q.id = per_quiz.quiz_id
  order by per_quiz.avg_score asc
  limit 1;

  insert into class_ai_summaries as t (
    class_id, quiz_count, participant_count, average_score,
    best_quiz_title, best_quiz_score, worst_quiz_title, worst_quiz_score, computed_at
  )
  values (
    p_class_id, v_quiz_count, v_participant_count, v_average_score,
    v_best_title, v_best_score, v_worst_title, v_worst_score, now()
  )
  on conflict (class_id) do update set
    quiz_count = excluded.quiz_count,
    participant_count = excluded.participant_count,
    average_score = excluded.average_score,
    best_quiz_title = excluded.best_quiz_title,
    best_quiz_score = excluded.best_quiz_score,
    worst_quiz_title = excluded.worst_quiz_title,
    worst_quiz_score = excluded.worst_quiz_score,
    computed_at = excluded.computed_at
  returning * into result;

  return next result;
  return;
end;
$$;
