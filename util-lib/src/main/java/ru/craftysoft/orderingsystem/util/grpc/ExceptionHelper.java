package ru.craftysoft.orderingsystem.util.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.ProtoUtils;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import ru.craftysoft.orderingsystem.util.error.exception.BaseException;
import ru.craftysoft.orderingsystem.util.error.exception.InvocationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.grpc.Status.UNAVAILABLE;
import static lombok.AccessLevel.PRIVATE;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withMdc;

@NoArgsConstructor(access = PRIVATE)
public class ExceptionHelper {

    public static <T extends Message> StatusRuntimeException messageToException(T message) {
        var responseKey = ProtoUtils.keyForProto(message);
        var metadata = new Metadata();
        metadata.put(responseKey, message);
        return Status.INTERNAL.asRuntimeException(metadata);
    }

    public static Metadata.Key<byte[]> buildKey(Class<?> clazz) {
        return Metadata.Key.of(clazz.getName().toLowerCase() + Metadata.BINARY_HEADER_SUFFIX, Metadata.BINARY_BYTE_MARSHALLER);
    }

    public static <T> BaseException mapException(Logger log,
                                                 String point,
                                                 Map<String, String> mdc,
                                                 Throwable throwable,
                                                 Function<StatusRuntimeException, T> errorResponseBuilder,
                                                 Function<Throwable, InvocationException> invocationExceptionFactory,
                                                 Function<Throwable, InvocationException> retryableExceptionFactory,
                                                 BiConsumer<T, BaseException> baseExceptionFiller) {
        if (throwable instanceof StatusRuntimeException statusRuntimeException) {
            if (UNAVAILABLE.getCode().equals(statusRuntimeException.getStatus().getCode())) {
                return retryableExceptionFactory.apply(throwable)
                        .setOriginalCode(UNAVAILABLE.getCode().toString());
            }
            var exception = invocationExceptionFactory.apply(throwable);
            T errorResponse;
            try {
                errorResponse = errorResponseBuilder.apply(statusRuntimeException);
            } catch (Exception e) {
                withMdc(mdc, () -> log.error("{}.mapException.thrown", point, e));
                return exception;
            }
            baseExceptionFiller.accept(errorResponse, exception);
            return exception;
        }
        return retryableExceptionFactory.apply(throwable);
    }

    @Nullable
    @SneakyThrows
    public static <T extends Message> T extractErrorResponse(@Nonnull StatusRuntimeException exception,
                                                             Metadata.Key<byte[]> key,
                                                             InvalidProtocolBufferExceptionFunction<byte[], T> parser) {
        byte[] bytes = Optional.of(exception)
                .map(StatusRuntimeException::getTrailers)
                .map(trailers -> trailers.get(key))
                .orElse(null);
        return bytes != null
                ? parser.apply(bytes)
                : null;
    }

    @FunctionalInterface
    public interface InvalidProtocolBufferExceptionFunction<IN, OUT> {

        OUT apply(IN param) throws InvalidProtocolBufferException;

    }

}
