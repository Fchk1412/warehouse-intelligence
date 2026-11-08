-- Supports purchase-order history lookups for a given product.
CREATE INDEX idx_purchase_order_items_product
    ON purchase_order_items (product_id);

-- A RECEIVED order must have a receipt timestamp, and no other status may have one.
ALTER TABLE purchase_orders
    ADD CONSTRAINT chk_purchase_orders_received_at
        CHECK (
            (status = 'RECEIVED' AND received_at IS NOT NULL)
            OR
            (status <> 'RECEIVED' AND received_at IS NULL)
        );

