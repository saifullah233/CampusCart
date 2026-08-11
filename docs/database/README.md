# CampusCart Backend — Database & Migrations

## 1. Engine & conventions

- **MySQL 8** is the only datastore. **Flyway owns the schema**; migrations under
  `src/main/resources/db/migration` apply automatically on application boot.
- Hibernate runs with `ddl-auto=validate` — startup fails if entities and schema diverge.
- All `TIMESTAMP`/`DATETIME` values are stored and read in **UTC**.
- **`BaseEntity` convention** (every table): `id BINARY(16)` UUID primary key,
  `created_at`/`updated_at DATETIME(6) NOT NULL`, `version BIGINT NOT NULL` (optimistic lock).
- **Enums** are stored as `VARCHAR` and validated in the application/domain layer (portable,
  no DB-specific enum types).
- Migrations are **additive and immutable** — never edit an applied migration; add a new one.

Tests apply the exact same migrations on a **Testcontainers MySQL 8** instance, so the schema
under test equals the shipped schema.

---

## 2. Migrations (V1–V15)

| Version | Name | Adds |
|---|---|---|
| V1 | geo_and_institutions | `cities`, `colleges`, `college_email_domains` + FK/unique indexes |
| V2 | users | core `users` (unique email, college FK) |
| V3 | categories | flat `categories` (unique name + slug) |
| V4 | user_auth_columns | `users`: `password_hash`, `email_verified`, `role`, `status` (+ role/status indexes) |
| V5 | refresh_tokens | hashed rotating refresh-token sessions |
| V6 | user_registration_fields | `users`: city, phone, account type, optional college |
| V7 | otp_challenges | hashed OTP challenges + abuse-control state |
| V8 | products_and_images | `products`, `product_images` + marketplace indexes |
| V9 | wishlist_cart_orders_payments | `wishlist_items`, `cart_items`, `orders`, `order_items`, `payments` |
| V10 | chat_notifications_moderation | `conversations`, `chat_messages`, `user_blocks`, `chat_reports`, `product_likes`, `notifications` |
| V11 | reported_users | `chat_reports.reported_user_id` + index (user-report target) |
| V12 | reviews_and_product_reports | `reviews`; `chat_reports.reported_product_id` + index |
| V13 | admin_reference_data_and_audit_logs | active flags on `cities`/`colleges`/`categories`; `audit_logs` |
| V14 | login_rate_limits | `login_rate_limits` (hashed identity key) |
| V15 | analytics_created_at_indexes | standalone `created_at` indexes on `products`/`orders`/`reviews`/`chat_messages` |

Running them: `docker compose up -d mysql`, then start the app (Flyway migrates on boot).
No manual step is required.

---

## 3. Table inventory (23 tables + `flyway_schema_history`)

### Geo & institutions
- **cities** — name+state unique; `active` flag (V13).
- **colleges** — `city_id` FK; unique name per city; `active` flag.
- **college_email_domains** — globally unique `domain` → `college_id` FK (drives student email validation).

### Users & auth
- **users** — unique `email`, unique `phone_number`; `full_name`; optional `college_id` FK; `city_id` FK;
  `role`, `status`, `account_type`, `email_verified`, `password_hash` (BCrypt). Indexes on `status`, `role`, `city_id`, `college_id`.
- **refresh_tokens** — `token_hash` (SHA-256) **unique**; `user_id` FK; `expires_at`, `revoked_at`, `replaced_by`. Indexes on `user_id`, `expires_at`.
- **otp_challenges** — `destination_hash` (SHA-256), `code_hash` (BCrypt), channel/purpose, `expires_at`, `attempt_count`, `next_resend_at`, `verified_at`. Index on `(destination_hash, created_at)`.
- **login_rate_limits** — `identity_hash` (SHA-256) **unique**; `window_started_at`, `failure_count`, `locked_until`. Index on `locked_until`.

### Catalog
- **categories** — unique `name` + `slug`; `active` flag.

### Products
- **products** — `seller_id`/`college_id`/`city_id`/`category_id` FKs; `title`, `description`, `price`,
  `product_type`, `selling_reach`, `status`, `quantity`. Composite indexes:
  `(status,selling_reach,city_id,created_at)`, `(status,selling_reach,college_id,created_at)`,
  `(category_id,status,created_at)`, `(product_type,status,created_at)`, `(price,status,created_at)`,
  `(seller_id,status,created_at)`, and standalone `created_at` (V15).
- **product_images** — `product_id` FK; storage key/URL/type/size; index `(product_id, created_at)`.
- **product_likes** — `user_id`/`product_id` FKs; index `(product_id, created_at)`.

### Commerce
- **wishlist_items** — `(user_id, product_id)` unique; index `(user_id, created_at)`.
- **cart_items** — `(user_id, product_id)` unique; index `(user_id, created_at)`.
- **orders** — `buyer_id` FK; `total_amount`, `status`. Indexes `(buyer_id, created_at)`, `(status, created_at)`, standalone `created_at` (V15).
- **order_items** — `order_id`/`product_id`/`seller_id` FKs; snapshot `product_title`/`unit_price`/`quantity`/`line_total`; `(order_id, product_id)` unique; indexes `(order_id)`, `(seller_id, created_at)`, `(product_id)`.
- **payments** — `order_id` FK **unique** (one payment per order); `amount`, `status` (default `NOT_CONNECTED`), provider fields; index `(status, created_at)`.

### Chat / notifications / moderation
- **conversations** — `buyer_id`/`seller_id`/`product_id` FKs; `last_message_at`. Indexes `(buyer_id, updated_at)`, `(seller_id, updated_at)`, `(product_id)`.
- **chat_messages** — `conversation_id`/`sender_id` FKs; type/content/image fields; `moderation_status`; `read_at`. Indexes `(conversation_id, created_at)`, `(conversation_id, sender_id, read_at)`, standalone `created_at` (V15).
- **user_blocks** — blocker/blocked FKs (unique pair).
- **chat_reports** — `reporter_id` FK; nullable `conversation_id`/`message_id`/`reported_user_id` (V11)/`reported_product_id` (V12); `status`, `reason`, `reviewed_by`/`reviewed_at`. Indexes `(status, created_at)`, `(conversation_id, created_at)`, `(reported_user_id, created_at)`, `(reported_product_id, created_at)`.
- **notifications** — `user_id` FK; `type`, `title`, `content`, `data_json`, `read_at`. Indexes `(user_id, created_at)`, `(user_id, read_at, created_at)`.

### Reviews, admin & audit
- **reviews** — `reviewer_id`/`reviewed_user_id`/`product_id`/`order_id` FKs; `rating` (1–5), `text`, `status`, moderation fields. Indexes `(product_id, status, created_at)`, `(reviewed_user_id, status)`, `(order_id)`, standalone `created_at` (V15).
- **audit_logs** — `actor_id` FK; `action`, `target_type`/`target_id`. Indexes `(actor_id, created_at)`, `(target_type, target_id, created_at)`.

---

## 4. Index strategy

- **Foreign keys are indexed**, and hot filter/sort paths use composite indexes that lead
  with the filter column and end with `created_at` (the default sort), so listing endpoints
  are index-ordered.
- **Security lookups** are indexed on their hashed keys (`refresh_tokens.token_hash` unique,
  `otp_challenges.destination_hash`, `login_rate_limits.identity_hash` unique).
- **V15 standalone `created_at` indexes** (products/orders/reviews/chat_messages) exist
  specifically for the admin analytics range counters (`countByCreatedAtAfter`): the composite
  indexes all *lead* with another column, so a bare `created_at > ?` predicate could not use
  them and would otherwise scan the largest tables.
- Product keyword search uses `LIKE '%kw%'` (leading wildcard, non-sargable); acceptable at
  current scale, a FULLTEXT index would be the next step for a very large catalog.

---

## 5. Backups & operations

- Standard MySQL 8 backup/restore applies (`mysqldump`/snapshots). The schema is fully
  reproducible from Flyway; a fresh database migrates to the current version on first boot.
- Never hand-edit the schema in production — add a new `V16+` migration and deploy.
