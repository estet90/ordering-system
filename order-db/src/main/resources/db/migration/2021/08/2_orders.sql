CREATE TABLE orders.orders
(
    id          BIGSERIAL
        CONSTRAINT orders_pk PRIMARY KEY NOT NULL,
    name        VARCHAR(100)             NOT NULL,
    price       NUMERIC(12, 2)           NOT NULL,
    customer_id BIGINT                   NOT NULL,
    executor_id BIGINT,
    status      orders.order_status      NOT NULL
);