-- V20: Add display_order and is_cover to product_images for ordered multi-image listings.

ALTER TABLE product_images
    ADD COLUMN display_order INT NOT NULL DEFAULT 0,
    ADD COLUMN is_cover BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_product_images_product_order
    ON product_images (product_id, display_order);
