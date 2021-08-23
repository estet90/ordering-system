package ru.craftysoft.orderingsystem.util.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static java.util.Optional.ofNullable;
import static ru.craftysoft.orderingsystem.util.http.HttpHeader.X_B3_SPAN_ID;
import static ru.craftysoft.orderingsystem.util.http.HttpHeader.X_B3_TRACE_ID;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.*;
import static ru.craftysoft.orderingsystem.util.mdc.MdcUtils.withContext;

public class LoggingServerInterceptor implements ServerInterceptor {

    private static final Logger requestLogger = LoggerFactory.getLogger("ru.craftysoft.orderingsystem.util.grpc.server.request");
    private static final Logger responseLogger = LoggerFactory.getLogger("ru.craftysoft.orderingsystem.util.grpc.server.response");

    private final JsonFormat.Printer printer = JsonFormat.printer();

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        var traceId = ofNullable(headers.get(Metadata.Key.of(X_B3_TRACE_ID, ASCII_STRING_MARSHALLER)))
                .orElseGet(UuidUtils::generateDefaultUuid);
        var parentId = ofNullable(headers.get(Metadata.Key.of(X_B3_SPAN_ID, ASCII_STRING_MARSHALLER)))
                .orElse("null");
        var spanId = UuidUtils.generateDefaultUuid();
        var context = Context.current().withValues(
                TRACE_ID_KEY, traceId,
                SPAN_ID_KEY, spanId,
                PARENT_ID_KEY, parentId
        );
        var listener = next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void sendMessage(RespT message) {
                if (responseLogger.isDebugEnabled() && message instanceof MessageOrBuilder messageOrBuilder) {
                    try {
                        var json = printer.print(messageOrBuilder);
                        withContext(context, () -> responseLogger.debug("method={}\nresponse={}", call.getMethodDescriptor().getBareMethodName(), json));
                    } catch (InvalidProtocolBufferException e) {
                        withContext(context, () -> responseLogger.error("Ошибка при преобразовании сообщения в JSON", e));
                    } catch (Exception e) {
                        withContext(context, () -> requestLogger.error("Необработанная ошибка", e));
                    }
                }
                super.sendMessage(message);
            }

            @Override
            public void sendHeaders(Metadata responseMetadata) {
                ofNullable(SPAN_ID_KEY.get(context))
                        .ifPresent(spanId -> responseMetadata.put(Metadata.Key.of(X_B3_SPAN_ID, ASCII_STRING_MARSHALLER), spanId));
                ofNullable(TRACE_ID_KEY.get(context))
                        .ifPresent(traceId -> responseMetadata.put(Metadata.Key.of(X_B3_TRACE_ID, ASCII_STRING_MARSHALLER), traceId));
                super.sendHeaders(responseMetadata);
            }
        }, headers);

        var forwardingServerCallListener = new ForwardingServerCallListener<ReqT>() {
            @Override
            protected ServerCall.Listener<ReqT> delegate() {
                return listener;
            }

            @Override
            public void onMessage(ReqT message) {
                if (requestLogger.isDebugEnabled() && message instanceof MessageOrBuilder messageOrBuilder) {
                    try {
                        var json = printer.print(messageOrBuilder);
                        withContext(context, () -> requestLogger.debug("method={}\nrequest={}", call.getMethodDescriptor().getBareMethodName(), json));
                    } catch (InvalidProtocolBufferException e) {
                        withContext(context, () -> requestLogger.error("ошибка при преобразовании сообщения в JSON", e));
                    } catch (Exception e) {
                        withContext(context, () -> requestLogger.error("Необработанная ошибка", e));
                    }
                }
                super.onMessage(message);
            }
        };
        var previous = context.attach();
        try {
            return new ContextualizedServerCallListener<>(forwardingServerCallListener, context);
        } finally {
            context.detach(previous);
        }
    }
}
