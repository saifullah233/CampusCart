# Product Marketplace API

Part 5 adds products, categories, discovery policy, pagination, and product image
management. Product condition and audience are independent fields:

- `productType`: `NEW` or `SECOND_HAND`
- `sellingReach`: `MY_CAMPUS`, `OTHER_COLLEGES`, or `PUBLIC`

The backend never accepts seller, city, college, status, or role from the client. Seller
identity comes from the verified JWT. A student's college and city are copied from the
persisted user association; a community seller has a city and no college.

## Product Endpoints

All product endpoints require a bearer access token.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/products` | Create an active product. |
| `GET` | `/api/v1/products/{id}` | Read a product only when the viewer may discover it, or owns it. |
| `GET` | `/api/v1/products` | Search and page through server-visible products. |
| `PATCH` | `/api/v1/products/{id}` | Update seller-owned details; admins may moderate. |
| `DELETE` | `/api/v1/products/{id}` | Soft-delete a product. |
| `POST` | `/api/v1/products/{id}/sold` | Mark sold and set quantity to zero. |
| `POST` | `/api/v1/products/{id}/activate` | Activate an inactive product. |
| `POST` | `/api/v1/products/{id}/deactivate` | Deactivate an active product. |
| `POST` | `/api/v1/products/{id}/images` | Upload one validated image as multipart field `file`. |
| `DELETE` | `/api/v1/products/{id}/images/{imageId}` | Delete an owned product image from storage and metadata. |

Create/update fields are `categoryId`, `title`, `description`, `price`, `productType`,
`sellingReach`, and optional `quantity` (default `1` on create). Product details include
seller, college, city, category, status, timestamps, version, and image metadata.

## Discovery Policy

`scope` on `GET /api/v1/products` is one of:

- `MY_COLLEGE`: products associated with the viewer's college, including products whose
  reach is `MY_CAMPUS`, `OTHER_COLLEGES`, or `PUBLIC`.
- `NEARBY_COLLEGES`: active `OTHER_COLLEGES` and `PUBLIC` products in the viewer's city.
- `COMMUNITY_MARKETPLACE`: community-seller products that are public, or other-college
  products in the viewer's city.
- `ALL_PRODUCTS`: public products everywhere, other-college products in the viewer's
  city, and campus products for the viewer's college.

Non-owners can discover active products only. Sellers can see their own lifecycle states
for management, while admins can moderate products according to their role. The same
policy is applied to product details, so a hidden product cannot be fetched by guessing
its ID.

Supported filters are `keyword`, `categoryId`, `productType`, `sellingReach`, `collegeId`,
`cityId`, `minPrice`, `maxPrice`, and `status`. Pagination uses zero-based `page` and
`size` (maximum 50). Sorting is allow-listed, for example `createdAt,desc`, `price,asc`,
`title,asc`, or `updatedAt,desc`.

## Images

Uploads require ownership or admin moderation before storage is called. The API accepts
JPEG, PNG, and WEBP files only, verifies their magic bytes, limits each file to 5 MB,
and limits a product to 8 images. When enabled, Cloudinary stores images in a server-
owned product folder using authenticated delivery. Cloudinary credentials are supplied
through `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, and `CLOUDINARY_API_SECRET`; no
credentials are committed. With `CLOUDINARY_ENABLED=false`, the application remains
startable but image upload returns a safe configuration error until storage is enabled.

## Categories

`GET /api/v1/categories` and `GET /api/v1/categories/{id}` list/read categories.
Category create/update/delete endpoints are restricted to `ROLE_ADMIN`. A category
assigned to any product cannot be deleted.
