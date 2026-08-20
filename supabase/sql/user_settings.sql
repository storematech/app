-- supabase/sql/user_settings.sql
--
-- Backs generic account-level settings: one reusable table for any per-user preference that
-- should sync across devices, instead of a new table per setting. Each row is one
-- (user_id, setting_key) preference; setting_value is a small jsonb blob whose shape is owned by
-- whichever feature reads/writes that key. First consumer: "report_design" — the PDF export
-- template + accent color, shape {"template": "modern"|"classic", "colorHex": "#RRGGBB"} — see
-- SettingsRepository.kt's getReportDesign()/saveReportDesign() wrapper.
--
-- Run this once in the Supabase SQL Editor. Safe to re-run (create table if not exists,
-- drop policy if exists before create policy).

create table if not exists user_settings (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  setting_key text not null,
  setting_value jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  -- One row per preference per user — saving an existing key updates it in place instead of
  -- accumulating duplicate rows.
  unique (user_id, setting_key)
);

alter table user_settings enable row level security;

drop policy if exists "Users manage their own settings" on user_settings;
create policy "Users manage their own settings"
  on user_settings for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);
