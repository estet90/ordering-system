CREATE TABLE users.users
(
    id       BIGSERIAL
        CONSTRAINT users_pk PRIMARY KEY NOT NULL,
    login    VARCHAR(100)
        CONSTRAINT users_login_unq NOT NULL UNIQUE,
    password VARCHAR(24)                NOT NULL,
    roles VARCHAR(50)[] NOT NULL
);