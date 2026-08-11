# CampusCart Backend — API Reference

All endpoints are under `http://<host>:8080`. Unless noted **Public**, a request needs
`Authorization: Bearer <accessToken>`. Authorization is enforced from the verified token,
never from the request body. See [../security/README.md](../security/README.md).

Interactive docs: **Swagger UI** `/swagger-ui.html`, **OpenAPI JSON** `/v3/api-docs`
(bearer scheme `bearerAuth`).

---

## 1. Conventions

### Response envelope
Every response is an `ApiResponse<T>`:
```json
{ "success": true,  "message": "…", "data": { … },  "error": null }
{ "success": false, "message": "…", "data": null,   "error": { … } }
```
`ApiError`:
```json
{ "code": "VALIDATION_ERROR", "detail": "…", "path": "/api/v1/…",
  "fieldErrors": [ { "field": "price", "message": "must be positive" } ] }
```
Paginated reads return `PageResponse<T>`: `{ content, page, size, totalElements, totalPages, first, last }`.

### Pagination & sorting
`page` (zero-based, default 0), `size` (default 20, **max 50**). Sorting is allow-listed per
endpoint, e.g. products accept `createdAt,desc | price,asc | title,asc | updatedAt,desc | quantity,*`.

### Auth legend
**Public** · **User** (any active account) · **Owner** (resource owner/participant) ·
**Admin** (`ROLE_ADMIN`).

### Public route groups
`/api/v1/ping`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`, `/ws`,
`/api/v1/auth/**`, `/api/v1/otp/**`, `/error`, and `OPTIONS` preflight. Everything else is
authenticated; `/api/v1/admin/**` requires `ROLE_ADMIN`.

---

## 2. Error codes → HTTP status

| Code | HTTP | Code | HTTP |
|---|---|---|---|
| `VALIDATION_ERROR` | 400 | `INVALID_TOKEN` | 401 |
| `MALFORMED_REQUEST` | 400 | `ACCESS_DENIED` | 403 |
| `OTP_INVALID` / `INVALID_IMAGE` / `IMAGE_LIMIT_EXCEEDED` | 400 | `ACCOUNT_NOT_ACTIVE` / `USER_BLOCKED` | 403 |
| `UNSAFE_CONTENT` / `INVALID_REVIEW` / `INVALID_REPORT` | 400 | `RESOURCE_NOT_FOUND` | 404 |
| `AUTHENTICATION_REQUIRED` | 401 | `METHOD_NOT_ALLOWED` | 405 |
| `INVALID_CREDENTIALS` | 401 | `UNSUPPORTED_MEDIA_TYPE` | 415 |
| `CONSTRAINT_VIOLATION` / `DUPLICATE_RESOURCE` | 409 | `LOGIN_RATE_LIMITED` / `OTP_ATTEMPTS_EXCEEDED` / `OTP_COOLDOWN` / `OTP_RATE_LIMITED` | 429 |
| `BUSINESS_RULE_VIOLATION` / `PRODUCT_UNAVAILABLE` | 409 | `MEDIA_STORAGE_UNAVAILABLE` / `PAYMENT_INTEGRATION_UNAVAILABLE` | 503 |
| `CART_EMPTY` / `ORDER_STATE_INVALID` / `OTP_ALREADY_VERIFIED` | 409 | `INTERNAL_ERROR` | 500 |

---

## 3. Ops & health

| Method | Path | Purpose | Auth |
|---|---|---|---|
| GET | `/api/v1/ping` | Liveness | Public |
| GET | `/actuator/health` | Health | Public |

## 4. Auth — `/api/v1/auth` (Public)

| Method | Path | Purpose |
|---|---|---|
| POST | `/register/student` | Register a student (city, college, official email, name, password) → OTP |
| POST | `/register/community` | Register a community user (email, name, city, phone, password) → OTP |
| POST | `/login` | Email + password → access + refresh tokens |
| POST | `/refresh` | Rotate a refresh token → new token pair |
| POST | `/logout` | Revoke the presented refresh token |

## 5. OTP — `/api/v1/otp` (Public)

| Method | Path | Purpose |
|---|---|---|
| POST | `/verify` | Verify a challenge id + numeric code (activates the account) |
| POST | `/resend` | Resend a code after cooldown (supersedes the prior challenge) |

## 6. Users — `/api/v1/users`

| Method | Path | Purpose | Auth |
|---|---|---|---|
| GET | `/me` | Authenticated profile (status/role/city/college server-owned) | User |
| PATCH | `/me` | Update display name only | User |

## 7. Products — `/api/v1/products`

| Method | Path | Purpose | Auth |
|---|---|---|---|
| POST | `` | Create an active product | User |
| GET | `` | Search/page visible products (`scope`, `keyword`, `categoryId`, `productType`, `sellingReach`, `collegeId`, `cityId`, `minPrice`, `maxPrice`, `status`, `page`, `size`, `sort`) | User |
| GET | `/{id}` | Read (only if discoverable/owned; 404 otherwise) | User |
| PATCH | `/{id}` | Update seller-owned fields (admin may moderate) | Owner/Admin |
| DELETE | `/{id}` | Soft-delete | Owner/Admin |
| POST | `/{id}/sold` | Mark sold, quantity → 0 | Owner/Admin |
| POST | `/{id}/activate` · `/{id}/deactivate` | Lifecycle | Owner/Admin |
| POST | `/{id}/images` | Upload one validated image (multipart `file`) | Owner/Admin |
| DELETE | `/{id}/images/{imageId}` | Delete an owned image | Owner/Admin |
| POST/DELETE/GET | `/{productId}/like` | Like / unlike / like status | User |

## 8. Categories — `/api/v1/categories`

| Method | Path | Purpose | Auth |
|---|---|---|---|
| GET | `` · `/{id}` | List / read categories | User |
| POST · PATCH · DELETE | `` · `/{id}` | Create / update / delete (product-referenced delete blocked) | Admin |
| POST | `/{id}/activate` · `/{id}/deactivate` | Activation state | Admin |

## 9. Cart — `/api/v1/cart`

| Method | Path | Purpose | Auth |
|---|---|---|---|
| POST | `/items` | Add `{productId, quantity}` (active, discoverable, in-stock, not own) | User |
| PATCH | `/items/{productId}` | Replace a line quantity | User |
| DELETE | `/items/{productId}` | Remove a line | User |
| GET | `` | Lines, totals, availability, checkout readiness | User |

## 10. Wishlist — `/api/v1/wishlist`

| Method | Path | Purpose | Auth |
|---|---|---|---|
| POST · DELETE | `/{productId}` | Add / remove | User |
| GET | `` | Paginated wishlist | User |
| GET | `/check/{productId}` | Membership check | User |

## 11. Orders — `/api/v1/orders`

| Method | Path | Purpose | Auth |
|---|---|---|---|
| POST | `` | Checkout the cart into one order (locked, transactional) | User |
| GET | `` | Buyer order history | User (buyer) |
| GET | `/seller` | Seller order view | User (seller) |
| GET | `/{orderId}` | Order detail | Buyer / involved Seller / Admin |
| PATCH | `/{orderId}/status` | Apply a permitted transition | Buyer / Seller / Admin |
| POST | `/{orderId}/cancel` · `/{orderId}/complete` | Buyer/Admin shortcuts | Buyer / Admin |

Transitions: `PLACED → ACCEPTED → SHIPPED → DELIVERED → COMPLETED`; `PLACED/ACCEPTED → CANCELLED`;
`PLACED → REJECTED`. Buyers cancel/complete; sellers accept/reject/ship/deliver.

## 12. Payments — `/api/v1/payments`

| Method | Path | Purpose | Auth |
|---|---|---|---|
| POST | `/orders/{orderId}/initialize` | Initialize payment — returns `503 PAYMENT_INTEGRATION_UNAVAILABLE` until a provider is wired | Buyer |

## 13. Chat — `/api/v1/conversations`

| Method | Path | Purpose | Auth |
|---|---|---|---|
| POST | `` | Start/get a conversation for a product | Buyer |
| GET | `` | List conversations (paginated) | Participant |
| GET | `/{conversationId}` | Conversation detail | Participant |
| GET | `/{conversationId}/messages` | Messages (paginated) | Participant |
| POST | `/{conversationId}/messages` | Send text | Participant |
| POST | `/{conversationId}/messages/product` | Share a product | Participant |
| POST | `/{conversationId}/messages/image` | Send an image (multipart, validated + safety-scanned) | Participant |
| POST | `/{conversationId}/read` | Mark read | Participant |
| GET | `/{conversationId}/unread-count` | Unread count | Participant |
| POST | `/{conversationId}/report` | Report a message/conversation | Participant |

## 14. Blocks — `/api/v1/blocks`

| Method | Path | Purpose | Auth |
|---|---|---|---|
| POST · DELETE | `/{userId}` | Block / unblock a user | User |

## 15. Reports — `/api/v1/reports`

| Method | Path | Purpose | Auth |
|---|---|---|---|
| POST | `/users/{targetUserId}` | Report a user | User |
| POST | `/products/{productId}` | Report a product (not your own) | User |

## 16. Reviews — `/api/v1/reviews`

| Method | Path | Purpose | Auth |
|---|---|---|---|
| POST | `` | Create a review (requires a completed order for the product) | Buyer |
| GET | `/products/{productId}` · `/sellers/{sellerId}` | Approved reviews | User |
| GET | `/me` · `/{reviewId}` | Own reviews / one review | User |

## 17. Notifications — `/api/v1/notifications`

| Method | Path | Purpose | Auth |
|---|---|---|---|
| GET | `` | Paginated notifications | User |
| GET | `/unread-count` | Unread count | User |
| PATCH | `/{notificationId}/read` | Mark one read | User |
| POST | `/read-all` | Mark all read | User |

## 18. Admin — `/api/v1/admin/**` (Admin only)

| Method | Path | Purpose |
|---|---|---|
| GET | `/users`, `/users/{id}` | List/search, detail |
| POST | `/users/{id}/suspend`, `/users/{id}/activate` | Account status |
| GET/POST/PATCH | `/cities`, `/cities/{id}`, `/cities/{id}/activate|deactivate` | City reference data |
| GET/POST/PATCH | `/colleges`, `/colleges/{id}`, `/colleges/{id}/activate|deactivate` | College reference data |
| GET | `/categories` | Category admin listing |
| GET | `/products`, `/products/reported`, `/products/{id}` | Product queues/detail |
| POST/DELETE | `/products/{id}/hide`, `/products/{id}/restore`, `/products/{id}` | Moderate/soft-remove |
| GET/PATCH | `/reviews`, `/reviews/{id}` | Review queue / moderate |
| GET/PATCH | `/reports`, `/reports/{id}` | Report queue / lifecycle |
| GET/PATCH | `/chat-reports`, `/chat-reports/{id}` | Chat-report queue / lifecycle |
| GET | `/audit-logs` | Audit history |
| GET | `/dashboard`, `/analytics` | Dashboard + analytics metrics |

---

## 19. WebSocket API (STOMP over `/ws`)

- **Endpoint:** raw WebSocket at `/ws`; allowed origins are the configured CORS origins
  (no wildcard). No SockJS fallback.
- **Auth:** send the JWT on the STOMP **CONNECT** frame (`Authorization: Bearer <token>`);
  the channel interceptor authenticates the connection and authorises each `SUBSCRIBE`.
- **Prefixes:** app destinations `/app`, broker `/topic` + `/queue`, user `/user`.

| Direction | Destination | Purpose |
|---|---|---|
| Client → server | `/app/conversations/{id}/message` | Send a chat message |
| Client → server | `/app/conversations/{id}/typing` | Typing indicator |
| Server → client | `/topic/conversations/{id}` | New messages for a conversation (participants) |
| Server → client | `/user/queue/notifications` | Per-user notification push |

Broker topology and scaling notes: [../architecture/README.md](../architecture/README.md#6-realtime--websocket-topology).
