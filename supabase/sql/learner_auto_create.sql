-- supabase/sql/learner_auto_create.sql
--
-- Lets an anonymous quiz-taking session (the student is never authenticated as the quiz creator)
-- trigger learner-roster creation on that CREATOR's `learners` table without granting anonymous
-- callers any RLS access to `learners` or `user_settings` directly — mirrors the existing
-- get_class_ai_summary()/get_quiz_ai_summary() pattern (see classes.sql/quiz_ai_summary.sql) of a
-- narrow `security definer` function as the only door between an anonymous caller and another
-- account's data.
--
-- The "enabled" check reads user_settings directly (setting_key = 'learner_auto_create',
-- setting_value = {"enabled": true|false}) — same generic table SettingsRepository.kt already
-- reads/writes; no new settings table.
--
-- Run this once in the Supabase SQL Editor.

create or replace function auto_create_learner_if_enabled(p_creator_id uuid, p_name text, p_email text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  is_enabled boolean;
begin
  if p_email is null or p_email = '' then
    return;
  end if;

  select coalesce((setting_value->>'enabled')::boolean, false)
  into is_enabled
  from user_settings
  where user_id = p_creator_id and setting_key = 'learner_auto_create';

  if not coalesce(is_enabled, false) then
    return;
  end if;

  if exists (select 1 from learners where created_by = p_creator_id and email = lower(p_email)) then
    return;
  end if;

  insert into learners (student_id, name, email, created_by)
  values (
    'STU' || floor(extract(epoch from now()) * 1000)::text,
    coalesce(nullif(trim(p_name), ''), split_part(p_email, '@', 1)),
    lower(p_email),
    p_creator_id
  );
end;
$$;

grant execute on function auto_create_learner_if_enabled(uuid, text, text) to anon, authenticated;
