-- V19: Seed default product categories for marketplace
-- Provides initial flat taxonomy for products.

INSERT INTO categories (id, name, slug, active, created_at, updated_at, version) VALUES
    (UUID_TO_BIN(UUID()), 'Books', 'books', b'1', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
    (UUID_TO_BIN(UUID()), 'Electronics', 'electronics', b'1', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
    (UUID_TO_BIN(UUID()), 'Clothing', 'clothing', b'1', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
    (UUID_TO_BIN(UUID()), 'Furniture', 'furniture', b'1', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
    (UUID_TO_BIN(UUID()), 'Accessories', 'accessories', b'1', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
    (UUID_TO_BIN(UUID()), 'Stationery', 'stationery', b'1', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
    (UUID_TO_BIN(UUID()), 'Sports & Fitness', 'sports-fitness', b'1', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0),
    (UUID_TO_BIN(UUID()), 'Other', 'other', b'1', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), 0);
