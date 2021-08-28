INSERT INTO customers.customers (user_id, balance)
VALUES ((SELECT id FROM users.users WHERE login = 'customer1'), 1000.0),
       ((SELECT id FROM users.users WHERE login = 'customer2'), 2000.0);