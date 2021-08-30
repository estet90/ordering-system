DROP DATABASE IF EXISTS ordering_system;
DROP USER IF EXISTS ordering_system;

CREATE USER ordering_system WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    CREATEROLE
    INHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'ordering_system';

CREATE DATABASE ordering_system
    WITH OWNER = ordering_system
    ENCODING = 'UTF8'
    CONNECTION LIMIT = -1;

GRANT CONNECT ON DATABASE ordering_system TO ordering_system;
