INSERT INTO orders.orders (name, price, customer_id, executor_id, status)
VALUES ('test order1', 100.00, (SELECT id FROM users.users WHERE login = 'customer1'), null, 'active'),
       ('test order2', 200.00, (SELECT id FROM users.users WHERE login = 'customer2'), null, 'active');