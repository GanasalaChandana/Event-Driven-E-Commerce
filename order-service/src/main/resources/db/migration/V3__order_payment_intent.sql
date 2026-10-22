-- Remember which Stripe payment paid for the order so a cancellation can refund it.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_intent_id VARCHAR(100);
