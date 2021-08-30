package ru.craftysoft.orderingsystem.util.mdc;

import io.grpc.Context;
import org.slf4j.MDC;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.*;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.*;

public class MdcUtils {

    public static Runnable withMdc(Runnable runnable) {
        var mdc = MDC.getCopyOfContextMap();
        if (mdc == null) {
            return runnable;
        }
        return () -> {
            var oldMdc = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                runnable.run();
            } finally {
                if (oldMdc != null) {
                    MDC.setContextMap(oldMdc);
                }
            }
        };
    }

    public static void withMdc(@Nullable Map<String, String> mdc, Runnable runnable) {
        if (mdc == null) {
            runnable.run();
            return;
        }
        var oldMdc = MDC.getCopyOfContextMap();
        try {
            MDC.setContextMap(mdc);
            runnable.run();
        } finally {
            if (oldMdc != null) {
                MDC.setContextMap(oldMdc);
            }
        }
    }

    public static <U> Supplier<U> withMdc(Supplier<U> supplier) {
        var mdc = MDC.getCopyOfContextMap();
        if (mdc == null) {
            return supplier;
        }
        return () -> {
            var oldMdc = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                return supplier.get();
            } finally {
                if (oldMdc != null) {
                    MDC.setContextMap(oldMdc);
                }
            }
        };
    }

    public static <U> Consumer<U> withMdc(Consumer<U> consumer) {
        var mdc = MDC.getCopyOfContextMap();
        if (mdc == null) {
            return consumer;
        }
        return u -> {
            var oldMdc = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                consumer.accept(u);
            } finally {
                if (oldMdc != null) {
                    MDC.setContextMap(oldMdc);
                }
            }
        };
    }

    public static <IN, OUT> Function<IN, OUT> withMdc(Function<IN, OUT> function) {
        var mdc = MDC.getCopyOfContextMap();
        if (mdc == null) {
            return function;
        }
        return in -> {
            var oldMdc = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                return function.apply(in);
            } finally {
                if (oldMdc != null) {
                    MDC.setContextMap(oldMdc);
                }
            }
        };
    }

    public static <IN1, IN2, OUT> BiFunction<IN1, IN2, OUT> withMdc(BiFunction<IN1, IN2, OUT> function) {
        var mdc = MDC.getCopyOfContextMap();
        if (mdc == null) {
            return function;
        }
        return (in1, in2) -> {
            var oldMdc = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                return function.apply(in1, in2);
            } finally {
                if (oldMdc != null) {
                    MDC.setContextMap(oldMdc);
                }
            }
        };
    }

    public static <IN1, IN2> BiConsumer<IN1, IN2> withMdc(BiConsumer<IN1, IN2> consumer) {
        var mdc = MDC.getCopyOfContextMap();
        return withMdc(mdc, consumer);
    }

    public static <IN1, IN2> BiConsumer<IN1, IN2> withMdc(@Nullable Map<String, String> mdc, BiConsumer<IN1, IN2> consumer) {
        if (mdc == null) {
            return consumer;
        }
        return (in1, in2) -> {
            var oldMdc = MDC.getCopyOfContextMap();
            try {
                MDC.setContextMap(mdc);
                consumer.accept(in1, in2);
            } finally {
                if (oldMdc != null) {
                    MDC.setContextMap(oldMdc);
                }
            }
        };
    }

    public static void withContext(Context context, Runnable action) {
        var oldMdc = MDC.getCopyOfContextMap();
        try {
            mdcInit(context);
            action.run();
        } finally {
            if (oldMdc != null) {
                MDC.setContextMap(oldMdc);
            }
        }
    }

    public static <T> T withContext(Context context, Supplier<T> action) {
        var oldMdc = MDC.getCopyOfContextMap();
        try {
            mdcInit(context);
            return action.get();
        } finally {
            if (oldMdc != null) {
                MDC.setContextMap(oldMdc);
            }
        }
    }

    private static void mdcInit(Context context) {
        ofNullable(OPERATION_NAME_KEY.get(context)).ifPresent(value -> MDC.put(OPERATION_NAME, value));
        ofNullable(TRACE_ID_KEY.get(context)).ifPresent(value -> MDC.put(TRACE_ID, value));
        ofNullable(SPAN_ID_KEY.get(context)).ifPresent(value -> MDC.put(SPAN_ID, value));
        ofNullable(PARENT_ID_KEY.get(context)).ifPresent(value -> MDC.put(PARENT_ID, value));
    }

}
