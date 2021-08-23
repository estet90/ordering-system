DROP SCHEMA IF EXISTS users CASCADE;
DROP SCHEMA IF EXISTS customers CASCADE;
DROP SCHEMA IF EXISTS executors CASCADE;
DROP SCHEMA IF EXISTS orders CASCADE;

DO
$$
    BEGIN
        IF EXISTS(SELECT FROM pg_roles WHERE rolname = 'users') THEN
            EXECUTE 'REASSIGN OWNED BY users TO postgres;';
            EXECUTE 'DROP OWNED BY users;';
        END IF;
        IF EXISTS(SELECT FROM pg_roles WHERE rolname = 'customers') THEN
            EXECUTE 'REASSIGN OWNED BY customers TO postgres;';
            EXECUTE 'DROP OWNED BY customers;';
        END IF;
        IF EXISTS(SELECT FROM pg_roles WHERE rolname = 'executors') THEN
            EXECUTE 'REASSIGN OWNED BY executors TO postgres;';
            EXECUTE 'DROP OWNED BY executors;';
        END IF;
        IF EXISTS(SELECT FROM pg_roles WHERE rolname = 'orders') THEN
            EXECUTE 'REASSIGN OWNED BY orders TO postgres;';
            EXECUTE 'DROP OWNED BY orders;';
        END IF;
    END
$$;

DROP USER IF EXISTS users;
DROP USER IF EXISTS customers;
DROP USER IF EXISTS executors;
DROP USER IF EXISTS orders;