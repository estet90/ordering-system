CREATE TABLE customers.customers
(
    id      BIGSERIAL
        CONSTRAINT customers_pk PRIMARY KEY NOT NULL,
    user_id BIGINT                          NOT NULL,
    balance NUMERIC(12, 2)                  NOT NULL
);