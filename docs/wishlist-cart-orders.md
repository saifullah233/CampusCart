# Wishlist, Cart, Orders, and Payment Foundation

Part 6 adds user-scoped wishlist and cart APIs, transactional checkout, order
management, and a deferred payment-provider boundary. All endpoints below require a
Bearer access token.

## Wishlist

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/wishlist/{productId}` | Add an active, server-discoverable product. |
| `DELETE` | `/api/v1/wishlist/{productId}` | Remove a product. |
| `GET` | `/api/v1/wishlist?page=0&size=20` | List the authenticated user's wishlist. |
| `GET` | `/api/v1/wishlist/check/{productId}` | Check membership. |

The authenticated principal supplies the user identity. A database unique constraint on
`(user_id, product_id)` backs the application duplicate check.

## Cart

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/cart/items` | Add `{ "productId": "...", "quantity": 1 }`. |
| `PATCH` | `/api/v1/cart/items/{productId}` | Replace a line quantity. |
| `DELETE` | `/api/v1/cart/items/{productId}` | Remove a line. |
| `GET` | `/api/v1/cart` | Return lines, totals, availability, and checkout readiness. |

Only active products with sufficient stock can be added or updated. A cart response marks
stale or sold lines as unavailable; checkout rejects the entire cart until those lines are
removed or become available again. A user cannot purchase their own listing.

## Orders

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/orders` | Reserve every available cart line and create one order. |
| `GET` | `/api/v1/orders?page=0&size=20` | Buyer order history. |
| `GET` | `/api/v1/orders/seller?page=0&size=20` | Seller order management view. |
| `GET` | `/api/v1/orders/{orderId}` | Buyer, involved seller, or admin detail. |
| `PATCH` | `/api/v1/orders/{orderId}/status` | Apply a permitted status transition. |
| `POST` | `/api/v1/orders/{orderId}/cancel` | Buyer/admin cancellation shortcut. |
| `POST` | `/api/v1/orders/{orderId}/complete` | Buyer/admin completion shortcut. |

Order statuses and transitions are:

```text
PLACED -> ACCEPTED -> SHIPPED -> DELIVERED -> COMPLETED
   |          |
   +----------+-> CANCELLED
PLACED -> REJECTED
```

The buyer can cancel from `PLACED` or `ACCEPTED`, and complete after delivery. An
involved seller can accept, reject, ship, or mark delivery. Admins can moderate through
the same domain transition rules. Rejection or cancellation restores reserved stock.

Checkout runs in one database transaction. It locks the buyer's cart lines, then locks
products in UUID order, validates current status and quantity, reserves stock, writes
seller/product/title/price snapshots to order items, creates the payment record, and
clears the cart. Any validation failure rolls back the reservations and order writes.
Product rows retain optimistic versions as an additional consistency check.

## Payment Foundation

Creating an order creates a payment record with status `NOT_CONNECTED`. The provider
boundary is `PaymentGateway`; the default `UnavailablePaymentGateway` returns
`PAYMENT_INTEGRATION_UNAVAILABLE` from:

`POST /api/v1/payments/orders/{orderId}/initialize`

No transaction, capture, or successful payment is fabricated. A real provider adapter
must return a provider-confirmed `PaymentInitialization` before the payment record can
move to a provider status.

## Verification

The Part 6 API tests cover duplicate wishlist prevention, cart quantity and availability
validation, unauthorized order access/modification, seller and buyer transitions,
cancellation stock restoration, and the deferred payment response. The schema test also
validates V9 and the wishlist, cart, order, order-item, and payment tables.
