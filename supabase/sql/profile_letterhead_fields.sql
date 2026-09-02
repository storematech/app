-- Adds two more optional PDF letterhead fields alongside the existing business_name/address/business_logo:
-- website and a registration/license number. Both null by default — no letterhead change for any
-- account that hasn't filled them in (same "all null = old behavior" pattern as address/business_logo).
alter table public.profiles
  add column if not exists website text,
  add column if not exists registration_number text;
