-- Standalone created_at indexes for the admin analytics/dashboard counters.
--
-- AdminAnalyticsService issues bare "created_at > ?" range counts
-- (Product/Order/Review/ChatMessage countByCreatedAtAfter) for the recent-activity
-- metrics. Every existing composite index on these tables LEADS with another column
-- (status / category_id / product_id / conversation_id), so a predicate on created_at
-- alone cannot use them and falls back to a full scan that grows with table size.
-- A leading-created_at index makes those range counts index-only.

CREATE INDEX idx_products_created_at ON products (created_at);
CREATE INDEX idx_orders_created_at ON orders (created_at);
CREATE INDEX idx_reviews_created_at ON reviews (created_at);
CREATE INDEX idx_chat_messages_created_at ON chat_messages (created_at);
