CREATE TABLE executors.executors
(
    id      BIGSERIAL
        CONSTRAINT executors_pk PRIMARY KEY NOT NULL,
    user_id BIGINT                          NOT NULL,
    balance NUMERIC(12, 2)                  NOT NULL
);