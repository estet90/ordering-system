CREATE SCHEMA users;

CREATE USER users WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    INHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'users';

GRANT CONNECT ON DATABASE ordering_system TO users;

CREATE SCHEMA customers;

CREATE USER customers WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    INHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'customers';

GRANT CONNECT ON DATABASE ordering_system TO customers;

CREATE SCHEMA executors;

CREATE USER executors WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    INHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'executors';

GRANT CONNECT ON DATABASE ordering_system TO executors;

CREATE SCHEMA orders;

CREATE USER orders WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    INHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'orders';

GRANT CONNECT ON DATABASE ordering_system TO orders;