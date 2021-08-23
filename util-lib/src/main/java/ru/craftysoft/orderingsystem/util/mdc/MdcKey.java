package ru.craftysoft.orderingsystem.util.mdc;

import io.grpc.Context;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class MdcKey {

    public static final String OPERATION_NAME = "operationName";
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String PARENT_ID = "parentId";

    public static final Context.Key<String> TRACE_ID_KEY = Context.key(TRACE_ID);
    public static final Context.Key<String> SPAN_ID_KEY = Context.key(SPAN_ID);
    public static final Context.Key<String> PARENT_ID_KEY = Context.key(PARENT_ID);

}
