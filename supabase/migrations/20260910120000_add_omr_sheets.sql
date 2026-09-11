-- Offline OMR Answer Sheet Scanning — foundation layer.
-- Stores the printable bubble-sheet layout generated for a quiz (fiducial marker positions, bubble
-- centers/radii per option, question order) so a later scan/grade pass can map detected marks back
-- to the exact question/option that produced them. One row per generated sheet; a quiz can be
-- re-exported (e.g. after editing questions), so `getLatestLayout` always reads the newest row for
-- a quiz rather than assuming a single row per quiz.
--
-- Not applied automatically — run this manually via the Supabase SQL Editor, same as every other
-- migration/schema change in this repo (see supabase/sql/*.sql for prior examples).
create table if not exists public.omr_sheets (
  id uuid primary key default gen_random_uuid(),
  quiz_id uuid not null references public.quizzes(id) on delete cascade,
  created_by uuid not null,
  page_width numeric not null,
  page_height numeric not null,
  layout jsonb not null,
  created_at timestamptz not null default now()
);

create index if not exists omr_sheets_quiz_id_idx on public.omr_sheets(quiz_id);

alter table public.omr_sheets enable row level security;

create policy "Owners can read their own omr sheets" on public.omr_sheets
  for select using (created_by = auth.uid());

create policy "Owners can insert their own omr sheets" on public.omr_sheets
  for insert with check (created_by = auth.uid());
