package ru.craftysoft.orderingsystem.util.http;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class HttpHeader {

    public static final String X_B3_TRACE_ID = "X-B3-TraceId";
    public static final String X_B3_SPAN_ID = "X-B3-SpanId";

}
