INSERT INTO executors.executors (user_id, balance)
VALUES ((SELECT id FROM users.users WHERE login = 'executor1'), 1000.0),
       ((SELECT id FROM users.users WHERE login = 'executor2'), 2000.0);