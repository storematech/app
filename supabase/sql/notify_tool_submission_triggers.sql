-- supabase/sql/notify_tool_submission_triggers.sql
--
-- Wires up push notifications for the five "Tools" (More → Tools) response tables — poll votes,
-- voting ballots, RSVP registrations, feedback submissions, and onboarding submissions — mirroring
-- the exact pg_net-based trigger pattern this project's existing notify_quiz_submission() function
-- already uses for quiz_responses (this project's Dashboard doesn't expose the built-in "Database
-- Webhooks" convenience UI — every notification here is wired up as a hand-written trigger calling
-- net.http_post() directly, same as that one). One function + one trigger per table, each POSTing
-- to the shared notify-tool-submission Edge Function with a `table` field so it knows which one
-- fired — see that function's TOOL_CONFIGS for the full routing.
--
-- The Authorization header below is the project's public anon key (the same one already shipped in
-- the Android app's build.gradle.kts, and the same one notify_quiz_submission itself already uses)
-- — never a secret, it only satisfies Supabase's platform-level "is this a signed request" check.
-- The Edge Function does its own privileged work internally with its own SUPABASE_SERVICE_ROLE_KEY.
--
-- Run this once in the Supabase SQL Editor. Safe to re-run (create-or-replace / drop-if-exists).

create or replace function public.notify_poll_vote()
 returns trigger
 language plpgsql
 security definer
as $function$
begin
  perform net.http_post(
    url := 'https://wybjydjifaahelwfzawj.supabase.co/functions/v1/notify-tool-submission',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Ind5Ymp5ZGppZmFhaGVsd2Z6YXdqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQzODk5NDUsImV4cCI6MjA1OTk2NTk0NX0.qAxPEfEU7EQlqlKAQZ1FF1Z926C_j6iUoa_2eqF2-dE'
    ),
    body := jsonb_build_object('type', 'INSERT', 'table', 'poll_votes', 'record', to_jsonb(NEW))
  );
  return NEW;
end;
$function$;

drop trigger if exists notify_poll_vote_trigger on public.poll_votes;
create trigger notify_poll_vote_trigger
after insert on public.poll_votes
for each row execute function public.notify_poll_vote();


create or replace function public.notify_voting_ballot()
 returns trigger
 language plpgsql
 security definer
as $function$
begin
  perform net.http_post(
    url := 'https://wybjydjifaahelwfzawj.supabase.co/functions/v1/notify-tool-submission',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Ind5Ymp5ZGppZmFhaGVsd2Z6YXdqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQzODk5NDUsImV4cCI6MjA1OTk2NTk0NX0.qAxPEfEU7EQlqlKAQZ1FF1Z926C_j6iUoa_2eqF2-dE'
    ),
    body := jsonb_build_object('type', 'INSERT', 'table', 'voting_ballots', 'record', to_jsonb(NEW))
  );
  return NEW;
end;
$function$;

drop trigger if exists notify_voting_ballot_trigger on public.voting_ballots;
create trigger notify_voting_ballot_trigger
after insert on public.voting_ballots
for each row execute function public.notify_voting_ballot();


create or replace function public.notify_rsvp_registration()
 returns trigger
 language plpgsql
 security definer
as $function$
begin
  perform net.http_post(
    url := 'https://wybjydjifaahelwfzawj.supabase.co/functions/v1/notify-tool-submission',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Ind5Ymp5ZGppZmFhaGVsd2Z6YXdqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQzODk5NDUsImV4cCI6MjA1OTk2NTk0NX0.qAxPEfEU7EQlqlKAQZ1FF1Z926C_j6iUoa_2eqF2-dE'
    ),
    body := jsonb_build_object('type', 'INSERT', 'table', 'rsvp_registrations', 'record', to_jsonb(NEW))
  );
  return NEW;
end;
$function$;

drop trigger if exists notify_rsvp_registration_trigger on public.rsvp_registrations;
create trigger notify_rsvp_registration_trigger
after insert on public.rsvp_registrations
for each row execute function public.notify_rsvp_registration();


create or replace function public.notify_feedback_submission()
 returns trigger
 language plpgsql
 security definer
as $function$
begin
  perform net.http_post(
    url := 'https://wybjydjifaahelwfzawj.supabase.co/functions/v1/notify-tool-submission',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Ind5Ymp5ZGppZmFhaGVsd2Z6YXdqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQzODk5NDUsImV4cCI6MjA1OTk2NTk0NX0.qAxPEfEU7EQlqlKAQZ1FF1Z926C_j6iUoa_2eqF2-dE'
    ),
    body := jsonb_build_object('type', 'INSERT', 'table', 'feedback_submissions', 'record', to_jsonb(NEW))
  );
  return NEW;
end;
$function$;

drop trigger if exists notify_feedback_submission_trigger on public.feedback_submissions;
create trigger notify_feedback_submission_trigger
after insert on public.feedback_submissions
for each row execute function public.notify_feedback_submission();


create or replace function public.notify_onboarding_submission()
 returns trigger
 language plpgsql
 security definer
as $function$
begin
  perform net.http_post(
    url := 'https://wybjydjifaahelwfzawj.supabase.co/functions/v1/notify-tool-submission',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Ind5Ymp5ZGppZmFhaGVsd2Z6YXdqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQzODk5NDUsImV4cCI6MjA1OTk2NTk0NX0.qAxPEfEU7EQlqlKAQZ1FF1Z926C_j6iUoa_2eqF2-dE'
    ),
    body := jsonb_build_object('type', 'INSERT', 'table', 'onboarding_submissions', 'record', to_jsonb(NEW))
  );
  return NEW;
end;
$function$;

drop trigger if exists notify_onboarding_submission_trigger on public.onboarding_submissions;
create trigger notify_onboarding_submission_trigger
after insert on public.onboarding_submissions
for each row execute function public.notify_onboarding_submission();
