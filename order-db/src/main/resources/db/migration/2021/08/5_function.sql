CREATE OR REPLACE FUNCTION orders.complete_order(in_order_id BIGINT,
                                                 in_customer_id BIGINT,
                                                 in_customer_balance NUMERIC(12, 2)) RETURNS INT AS
$$
DECLARE
    order_update_result  INT;
    orders_update_result INT;
BEGIN
    UPDATE orders.orders
    SET status = 'complete'::orders.order_status
    WHERE id = in_order_id;
    GET DIAGNOSTICS order_update_result = ROW_COUNT;
    IF order_update_result = 0 THEN
        RETURN 0;
    END IF;
    UPDATE orders.orders
    SET status = 'unavailable'::orders.order_status
    WHERE customer_id = in_customer_id
      AND status = 'active'::orders.order_status
      AND price > in_customer_balance;
    GET DIAGNOSTICS orders_update_result = ROW_COUNT;
    RETURN (order_update_result + orders_update_result);
END;
$$
    LANGUAGE plpgsql;