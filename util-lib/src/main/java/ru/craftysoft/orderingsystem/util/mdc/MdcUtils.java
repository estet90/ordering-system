package ru.craftysoft.orderingsystem.util.mdc;

import io.grpc.Context;
import org.slf4j.MDC;

import java.util.Map;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.*;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.PARENT_ID;

public class MdcUtils {

    public static Runnable withMdc(Runnable runnable) {
        var mdc = MDC.getCopyOfContextMap();
        return () -> {
            var oldMdc = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                runnable.run();
            } finally {
                MDC.setContextMap(oldMdc);
            }
        };
    }

    public static <U> Supplier<U> withMdc(Supplier<U> supplier) {
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        return () -> {
            var oldMdc = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                return supplier.get();
            } finally {
                MDC.setContextMap(oldMdc);
            }
        };
    }

    public static void withContext(Context context, Runnable action) {
        var map = MDC.getCopyOfContextMap();
        try {
            ofNullable(TRACE_ID_KEY.get(context)).ifPresent(value -> MDC.put(TRACE_ID, value));
            ofNullable(SPAN_ID_KEY.get(context)).ifPresent(value -> MDC.put(SPAN_ID, value));
            ofNullable(PARENT_ID_KEY.get(context)).ifPresent(value -> MDC.put(PARENT_ID, value));
            action.run();
        } finally {
            if (map != null) {
                MDC.setContextMap(map);
            }
        }
    }

    public static <T> T withContext(Context context, Supplier<T> action) {
        var map = MDC.getCopyOfContextMap();
        try {
            ofNullable(TRACE_ID_KEY.get(context)).ifPresent(value -> MDC.put(TRACE_ID, value));
            ofNullable(SPAN_ID_KEY.get(context)).ifPresent(value -> MDC.put(SPAN_ID, value));
            ofNullable(PARENT_ID_KEY.get(context)).ifPresent(value -> MDC.put(PARENT_ID, value));
            return action.get();
        } finally {
            if (map != null) {
                MDC.setContextMap(map);
            }
        }
    }

}
