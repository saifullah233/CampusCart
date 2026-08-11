# Part 8: Reviews, Reports, Administration, and Analytics

Part 8 adds completed-transaction reviews, one report lifecycle for all supported
targets, role-protected administration, moderation, audit logs, and dashboard statistics.

## Reviews

Create a review after the authenticated buyer has a `COMPLETED` order containing the
product. The server derives the reviewer from the JWT principal and derives the seller,
product, and order item from persistence.

- `POST /api/v1/reviews`
- `GET /api/v1/reviews/products/{productId}`
- `GET /api/v1/reviews/sellers/{sellerId}`
- `GET /api/v1/reviews/me`
- `GET /api/v1/reviews/{reviewId}`
- `GET /api/v1/admin/reviews?status=PENDING`
- `PATCH /api/v1/admin/reviews/{reviewId}`

Ratings are from 1 through 5. Review text is required and bounded. A review cannot be
created without a completed interaction, for an unrelated product or seller, by the
seller, or more than once for the same reviewer/order/product tuple. New reviews are
`PENDING`; only `APPROVED` reviews are visible in public product and seller lists.
Administrators may approve, hide, or reject reviews.

## Reports

The existing chat report model is the single report architecture. It supports product,
user, conversation, and message/content targets:

- `POST /api/v1/reports/products/{productId}`
- `POST /api/v1/reports/users/{targetUserId}`
- `POST /api/v1/conversations/{conversationId}/report`
- `GET /api/v1/admin/reports?status={status}`
- `PATCH /api/v1/admin/reports/{reportId}`
- `GET /api/v1/admin/chat-reports`
- `PATCH /api/v1/admin/chat-reports/{reportId}`

New reports begin in `PENDING`. The canonical lifecycle is:
`PENDING -> UNDER_REVIEW -> RESOLVED` or `DISMISSED`. Active reports are
`PENDING` and `UNDER_REVIEW` (with `OPEN` retained for legacy rows). Active duplicate
reports are rejected; closed reports can be submitted again. Users cannot set report
status, report themselves, report inaccessible content, or report their own product.

## Administration and moderation

Every `/api/v1/admin/**` endpoint requires `ROLE_ADMIN`. Controllers enforce the role,
and service methods validate the authenticated persisted account. Normal users receive
`403`; unauthenticated requests receive `401`.

Administration APIs include:

- `/api/v1/admin/users`: paginated listing/search, detail, suspend, activate
- `/api/v1/admin/cities`: paginated city CRUD and activation state
- `/api/v1/admin/colleges`: paginated college CRUD and activation state
- `/api/v1/admin/categories`: paginated administration listing; category mutation and
  activation are also available under `/api/v1/categories`
- `/api/v1/admin/products`: product queue/detail, reported-product queue, hide, restore,
  and soft remove
- `/api/v1/admin/reviews`: review queue and moderation
- `/api/v1/admin/reports` and `/api/v1/admin/chat-reports`: report queues and lifecycle
- `/api/v1/admin/audit-logs`: paginated administrative audit history

Suspended accounts retain no effective authorities in the JWT filter and are rejected by
the service account-status checks. City, college, and category deactivation preserves
foreign-key relationships; inactive reference data cannot be used for new registrations
or products.

## Dashboard and analytics

- `GET /api/v1/admin/dashboard`
- `GET /api/v1/admin/analytics`

Both return total and active users, total/active/sold products, total/completed orders,
total/active reports, recent products/orders/reviews/messages, marketplace activity, and
the generation timestamp. Recent metrics cover the preceding 30 days. Marketplace
activity is recent product creation plus recent orders plus recent reviews.

## Persistence

`V13__admin_reference_data_and_audit_logs.sql` adds active flags to shared reference
data and creates `audit_logs` with UUID foreign keys, timestamps, versioning, and query
indexes. Existing migrations are unchanged.
