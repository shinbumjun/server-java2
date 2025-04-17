

DELETE FROM order_product;
DELETE FROM orders;
DELETE FROM product;
-- DELETE FROM best_seller;
DELETE FROM user_coupon;
DELETE FROM point_history;
DELETE FROM point;
DELETE FROM user;




-- 1. Insert into `product` table (No dependencies)
INSERT INTO product (product_name, description, price, stock, created_at, updated_at) VALUES
('Macbook Pro', 'Apple Laptop', 2000000, 50, NOW(), NOW()),
('iPhone 12', 'Apple Smartphone', 1200000, 100, NOW(), NOW()),
('Samsung Galaxy S21', 'Samsung Smartphone', 900000, 75, NOW(), NOW()),
('Dell XPS 13', 'Dell Laptop', 1500000, 30, NOW(), NOW()),
('LG OLED TV', 'LG OLED Television', 2500000, 20, NOW(), NOW()),
('Sony WH-1000XM4', 'Sony Noise Cancelling Headphones', 450000, 150, NOW(), NOW()),
('Apple Watch Series 6', 'Apple Smart Watch', 700000, 200, NOW(), NOW()),
('Samsung Galaxy Buds Pro', 'Wireless Earbuds', 150000, 100, NOW(), NOW()),
('Bose QuietComfort 35 II', 'Bose Noise Cancelling Headphones', 400000, 50, NOW(), NOW()),
('Nintendo Switch', 'Nintendo Game Console', 350000, 25, NOW(), NOW());



-- 2. Insert into `user` table (자동 증가를 활용하여 id 값을 명시하지 않음)
INSERT INTO user (created_at, updated_at) VALUES
(NOW(), NOW()),
(NOW(), NOW()),
(NOW(), NOW()),
(NOW(), NOW()),
(NOW(), NOW()),
(NOW(), NOW()),
(NOW(), NOW()),
(NOW(), NOW()),
(NOW(), NOW()),
(NOW(), NOW());




-- 4. Insert into `orders` table (User IDs and Coupon IDs must exist)
INSERT INTO orders (user_id, user_coupon_id, is_coupon_applied, total_amount, status, created_at, updated_at) VALUES
(1, 1, TRUE, 1500000, 'NOT_PAID', NOW(), NOW()),
(2, 2, TRUE, 2000000, 'PAID', NOW(), NOW()),
(3, NULL, FALSE, 1200000, 'CANCEL', NOW(), NOW()),
(4, 3, TRUE, 1800000, 'NOT_PAID', NOW(), NOW()),
(5, 4, TRUE, 2500000, 'PAID', NOW(), NOW()),
(6, NULL, FALSE, 2200000, 'PAID', NOW(), NOW()),
(7, 5, TRUE, 2100000, 'NOT_PAID', NOW(), NOW()),
(8, NULL, FALSE, 1700000, 'CANCEL', NOW(), NOW()),
(9, 6, TRUE, 2600000, 'PAID', NOW(), NOW()),
(10, 7, TRUE, 2300000, 'NOT_PAID', NOW(), NOW());


-- 6. Insert into `order_product` table (Orders must exist)
INSERT INTO order_product (product_id, orders_id, amount, quantity, created_at, updated_at) VALUES
(1, 1, 1000000, 1, NOW(), NOW()),
(2, 1, 1200000, 1, NOW(), NOW()),
(3, 2, 900000, 1, NOW(), NOW()),
(4, 2, 1500000, 1, NOW(), NOW()),
(5, 3, 2500000, 1, NOW(), NOW()),
(6, 4, 450000, 1, NOW(), NOW()),
(7, 4, 700000, 1, NOW(), NOW()),
(8, 5, 150000, 2, NOW(), NOW()),
(9, 6, 400000, 1, NOW(), NOW()),
(10, 7, 350000, 1, NOW(), NOW());


-- 7. Insert into `point` table (User IDs must exist)
INSERT INTO point (user_id, balance, created_at, updated_at) VALUES
(1, 500000, NOW(), NOW()),
(2, 1000000, NOW(), NOW()),
(3, 200000, NOW(), NOW()),
(4, 3000000, NOW(), NOW()),
(5, 1500000, NOW(), NOW()),
(6, 100000, NOW(), NOW()),
(7, 500000, NOW(), NOW()),
(8, 2000000, NOW(), NOW()),
(9, 250000, NOW(), NOW()),
(10, 1000000, NOW(), NOW());


-- 8. Insert into `point_history` table (Point IDs must exist)
INSERT INTO point_history (point_id, amount, balance, type, created_at, updated_at) VALUES
(1, 100000, 600000, '충전', NOW(), NOW()),
(2, 500000, 1500000, '충전', NOW(), NOW()),
(3, -100000, 100000, '사용', NOW(), NOW()),
(4, 1500000, 4500000, '충전', NOW(), NOW()),
(5, 500000, 2000000, '충전', NOW(), NOW()),
(6, -20000, 80000, '사용', NOW(), NOW()),
(7, 250000, 750000, '충전', NOW(), NOW()),
(8, 1000000, 3000000, '충전', NOW(), NOW()),
(9, -50000, 200000, '사용', NOW(), NOW()),
(10, 200000, 1200000, '충전', NOW(), NOW());





ALTER TABLE coupon MODIFY COLUMN id BIGINT(20) NOT NULL AUTO_INCREMENT;


-- 3. Insert into `coupon` table (자동 증가를 활용하여 id 값을 명시하지 않음)
INSERT INTO coupon (coupon_name, discount_value, discount_type, start_date, end_date, stock, created_at, updated_at)
VALUES
    ('10% 할인 쿠폰', 10.00, 'RATE', '2025-04-01', '2025-04-30', 100, NOW(), NOW()),
    ('5000원 할인 쿠폰', 5000.00, 'AMOUNT', '2025-04-01', '2025-04-30', 200, NOW(), NOW()),
    ('20% 할인 쿠폰', 20.00, 'RATE', '2025-05-01', '2025-05-31', 150, NOW(), NOW()),
    ('10,000원 할인 쿠폰', 10000.00, 'AMOUNT', '2025-05-01', '2025-05-31', 250, NOW(), NOW()),
    ('30% 할인 쿠폰', 30.00, 'RATE', '2025-06-01', '2025-06-30', 300, NOW(), NOW()),
    ('15,000원 할인 쿠폰', 15000.00, 'AMOUNT', '2025-06-01', '2025-06-30', 350, NOW(), NOW()),
    ('50% 할인 쿠폰', 50.00, 'RATE', '2025-07-01', '2025-07-31', 200, NOW(), NOW()),
    ('5,000원 할인 쿠폰', 5000.00, 'AMOUNT', '2025-07-01', '2025-07-31', 400, NOW(), NOW()),
    ('25% 할인 쿠폰', 25.00, 'RATE', '2025-08-01', '2025-08-31', 450, NOW(), NOW()),
    ('7,500원 할인 쿠폰', 7500.00, 'AMOUNT', '2025-08-01', '2025-08-31', 500, NOW(), NOW());


-- 5. Insert into `user_coupon` table (User IDs and Coupon IDs must exist)
INSERT INTO user_coupon (user_id, coupon_id, is_used, issued_at, expired_at, created_at, updated_at)
VALUES
    (1, 1, TRUE, '2025-04-01', '2025-04-30', NOW(), NOW()),
    (2, 2, FALSE, '2025-04-01', '2025-04-30', NOW(), NOW()),
    (3, 3, TRUE, '2025-05-01', '2025-05-31', NOW(), NOW()),
    (4, 4, FALSE, '2025-05-01', '2025-05-31', NOW(), NOW()),
    (5, 5, TRUE, '2025-06-01', '2025-06-30', NOW(), NOW()),
    (6, 6, FALSE, '2025-06-01', '2025-06-30', NOW(), NOW()),
    (7, 7, TRUE, '2025-07-01', '2025-07-31', NOW(), NOW()),
    (8, 8, FALSE, '2025-07-01', '2025-07-31', NOW(), NOW()),
    (9, 9, TRUE, '2025-08-01', '2025-08-31', NOW(), NOW()),
    (10, 10, FALSE, '2025-08-01', '2025-08-31', NOW(), NOW());


# -- 9. Insert into `best_seller` table (No dependencies)
# INSERT INTO best_seller (product_id, product_name, description, stock, sales, created_at, updated_at) VALUES
# (1, 'Macbook Pro', 'Apple Laptop', 50, 100, NOW(), NOW()),
# (2, 'iPhone 12', 'Apple Smartphone', 100, 150, NOW(), NOW()),
# (3, 'Samsung Galaxy S21', 'Samsung Smartphone', 75, 120, NOW(), NOW()),
# (4, 'Dell XPS 13', 'Dell Laptop', 30, 80, NOW(), NOW()),
# (5, 'LG OLED TV', 'LG OLED Television', 20, 50, NOW(), NOW()),
# (6, 'Sony WH-1000XM4', 'Sony Noise Cancelling Headphones', 150, 200, NOW(), NOW()),
# (7, 'Apple Watch Series 6', 'Apple Smart Watch', 200, 250, NOW(), NOW()),
# (8, 'Samsung Galaxy Buds Pro', 'Wireless Earbuds', 100, 150, NOW(), NOW()),
# (9, 'Bose QuietComfort 35 II', 'Bose Noise Cancelling Headphones', 50, 70, NOW(), NOW()),
# (10, 'Nintendo Switch', 'Nintendo Game Console', 25, 40, NOW(), NOW());