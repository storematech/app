-- supabase/sql/scheduled_marketing_email_cron.sql
--
-- SQL alternative to setting the cron schedule in Supabase Dashboard > Edge Functions >
-- scheduled-marketing-email > Cron. Uses pg_cron to fire once an hour, and pg_net (same
-- net.http_post() pattern as notify_tool_submission_triggers.sql) to actually call the function —
-- pg_cron alone can't make HTTP requests, it only runs SQL on a schedule.
--
-- The Authorization header below is the project's public anon key (same one already shipped in
-- the Android app's build.gradle.kts, and the same one notify_tool_submission_triggers.sql already
-- uses) — never a secret, it only satisfies Supabase's platform-level "is this a signed request"
-- check. The function does its own privileged work internally with its own
-- SUPABASE_SERVICE_ROLE_KEY.
--
-- Run this once in the Supabase SQL Editor. Safe to re-run — cron.schedule() with the same job
-- name replaces the existing job rather than duplicating it.

create extension if not exists pg_cron;
create extension if not exists pg_net;

select cron.schedule(
  'scheduled-marketing-email-hourly',
  '0 * * * *', -- top of every hour
  $$
  select net.http_post(
    url := 'https://wybjydjifaahelwfzawj.supabase.co/functions/v1/scheduled-marketing-email',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Ind5Ymp5ZGppZmFhaGVsd2Z6YXdqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDQzODk5NDUsImV4cCI6MjA1OTk2NTk0NX0.qAxPEfEU7EQlqlKAQZ1FF1Z926C_j6iUoa_2eqF2-dE'
    ),
    body := '{}'::jsonb
  );
  $$
);

-- Housekeeping / verification queries --------------------------------------------------------

-- List every scheduled cron job in this project (confirm it's there and check jobid).
-- select * from cron.job;

-- Check recent run history for this job (success/failure, response, timing) — replace <jobid>
-- with the id from cron.job above, or filter by jobname directly:
-- select * from cron.job_run_details
-- where jobid = (select jobid from cron.job where jobname = 'scheduled-marketing-email-hourly')
-- order by start_time desc
-- limit 20;

-- To stop the job later:
-- select cron.unschedule('scheduled-marketing-email-hourly');
