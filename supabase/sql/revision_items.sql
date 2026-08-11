-- supabase/sql/revision_items.sql
--
-- Backs the "Revision" feature: a teacher can bookmark an individual question (from the Question
-- Performance screen, formerly "Quiz Analysis") for later review, then manage those bookmarks —
-- search/filter, bulk delete, bulk mark-as-done, and build a new quiz from a selection ("Create A
-- Re-Test") — from the Revision screen off the More menu.
--
-- Run this once in the Supabase SQL Editor. Safe to re-run (create table if not exists,
-- create or replace policy would need a drop first, so this only creates once).

create table if not exists revision_items (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  question_id uuid not null references questions(id) on delete cascade,
  -- The quiz this question was bookmarked from, for the "filter by quiz" list on the Revision
  -- screen. Kept even if that quiz is later deleted (set null) rather than dropping the bookmark.
  quiz_id uuid references quizzes(id) on delete set null,
  status text not null default 'pending', -- 'pending' | 'done'
  created_at timestamptz not null default now(),
  -- A question is either bookmarked or not, per user — re-adding an already-bookmarked question
  -- is a no-op rather than a duplicate row.
  unique (user_id, question_id)
);

alter table revision_items enable row level security;

create policy "Users manage their own revision items"
  on revision_items for all
  using (auth.uid() = user_id)
  with check (auth.uid() = user_id);
