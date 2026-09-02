-- Four more optional PDF letterhead fields, deliberately separate from the account's own
-- phone_number/email columns (those are login/OTP/quiz-taking contact info, not necessarily what an
-- account wants printed on a public letterhead) — a dedicated business phone/email plus a tagline and
-- GST/tax ID. All null by default, same "all null = old behavior" pattern as every other letterhead field.
alter table public.profiles
  add column if not exists letterhead_phone text,
  add column if not exists letterhead_email text,
  add column if not exists tagline text,
  add column if not exists gst_number text;
