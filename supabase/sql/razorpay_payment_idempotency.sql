-- supabase/sql/razorpay_payment_idempotency.sql
--
-- Closes a replay hole in verify-razorpay-payment: without this, resubmitting the exact same
-- (still-valid) razorpay_order_id/payment_id/signature repeatedly would extend the same user's
-- license_expired_date again each time, for free. `razorpay_order_id` is the primary key, so the
-- first request to successfully insert a row "claims" that order — any later request for the same
-- order_id hits the unique-violation branch in the function and is treated as already-processed
-- (returns success without granting anything a second time). Same "RLS on, no policies,
-- service-role only" shape as push_notification_log.sql/ai_generation_log.sql.
--
-- Run this once in the Supabase SQL Editor. Safe to re-run (create table if not exists).

create table if not exists processed_razorpay_payments (
  razorpay_order_id text primary key,
  razorpay_payment_id text not null,
  user_id uuid not null references auth.users(id) on delete cascade,
  processed_at timestamptz not null default now()
);

create index if not exists processed_razorpay_payments_user_id_idx on processed_razorpay_payments (user_id);

alter table processed_razorpay_payments enable row level security;
