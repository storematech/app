-- supabase/sql/quiz_ai_summary.sql
--
-- Backs the "AI Summary" shown on the Quiz List and Quiz Detail View screens. Despite the name,
-- there is no AI/ML involved anywhere here — it's plain SQL aggregation over quiz_responses /
-- quiz_answer_details, cached in quiz_ai_summaries so repeat views are a single cheap row read
-- instead of re-aggregating every time.
--
-- Run this once in the Supabase SQL Editor. Safe to re-run (create table if not exists,
-- create or replace function).

create table if not exists quiz_ai_summaries (
  quiz_id uuid primary key references quizzes(id) on delete cascade,
  participant_count integer not null default 0,
  average_score numeric not null default 0,
  median_score numeric not null default 0,
  avg_correct_count numeric not null default 0,
  avg_wrong_count numeric not null default 0,
  avg_time_seconds integer,
  confidence_level text not null default 'neutral',
  computed_at timestamptz not null default now()
);

-- No policies on purpose — this table is only ever read/written by the security definer
-- function below, so RLS-with-no-policies default-denies any direct PostgREST access to it.
alter table quiz_ai_summaries enable row level security;

create or replace function get_quiz_ai_summary(p_quiz_id uuid)
-- setof, not a bare row type: PostgREST returns a plain JSON object for a function that returns
-- a single composite row, but every client call in this app (decodeSingle()) expects the normal
-- array-wrapped shape PostgREST uses for everything else — setof gets that same [ {...} ] shape.
returns setof quiz_ai_summaries
language plpgsql
security definer
set search_path = public
as $$
declare
  live_count integer;
  cached quiz_ai_summaries;
  v_participant_count integer;
  v_average_score numeric;
  v_median_score numeric;
  v_avg_correct numeric;
  v_avg_wrong numeric;
  v_avg_time_seconds integer;
  v_below_50 integer;
  v_mid integer;
  v_above_65 integer;
  v_confidence text;
  result quiz_ai_summaries;
begin
  if not exists (select 1 from quizzes where id = p_quiz_id and created_by = auth.uid()) then
    raise exception 'Not authorized';
  end if;

  select count(*) into live_count
  from quiz_responses
  where quiz_id = p_quiz_id and completed = true;

  select * into cached from quiz_ai_summaries where quiz_id = p_quiz_id;

  -- Cheap staleness check: if the participant count hasn't moved since last computed, the
  -- cached row is still accurate — return it without re-aggregating.
  if cached.quiz_id is not null and cached.participant_count = live_count then
    return next cached;
    return;
  end if;

  select
    count(*),
    coalesce(avg(score), 0),
    coalesce(percentile_cont(0.5) within group (order by score), 0),
    avg(extract(epoch from (completed_at - started_at)))::integer,
    count(*) filter (where score < 50),
    count(*) filter (where score >= 50 and score < 65),
    count(*) filter (where score >= 65)
  into
    v_participant_count, v_average_score, v_median_score, v_avg_time_seconds,
    v_below_50, v_mid, v_above_65
  from quiz_responses
  where quiz_id = p_quiz_id and completed = true;

  select
    coalesce(avg(correct_count), 0),
    coalesce(avg(wrong_count), 0)
  into v_avg_correct, v_avg_wrong
  from (
    select
      r.id,
      count(*) filter (where d.is_correct = true) as correct_count,
      count(*) filter (where d.is_correct = false) as wrong_count
    from quiz_responses r
    left join quiz_answer_details d on d.response_id = r.id
    where r.quiz_id = p_quiz_id and r.completed = true
    group by r.id
  ) per_response;

  -- Whichever score band has the plurality of participants; ties (including the 0-participant
  -- case, where all three counts are 0) fall through to 'neutral'.
  v_confidence := case
    when v_below_50 > v_mid and v_below_50 > v_above_65 then 'negative'
    when v_above_65 > v_mid and v_above_65 > v_below_50 then 'positive'
    else 'neutral'
  end;

  insert into quiz_ai_summaries as t (
    quiz_id, participant_count, average_score, median_score,
    avg_correct_count, avg_wrong_count, avg_time_seconds, confidence_level, computed_at
  )
  values (
    p_quiz_id, v_participant_count, v_average_score, v_median_score,
    v_avg_correct, v_avg_wrong, v_avg_time_seconds, v_confidence, now()
  )
  on conflict (quiz_id) do update set
    participant_count = excluded.participant_count,
    average_score = excluded.average_score,
    median_score = excluded.median_score,
    avg_correct_count = excluded.avg_correct_count,
    avg_wrong_count = excluded.avg_wrong_count,
    avg_time_seconds = excluded.avg_time_seconds,
    confidence_level = excluded.confidence_level,
    computed_at = excluded.computed_at
  returning * into result;

  return next result;
  return;
end;
$$;
