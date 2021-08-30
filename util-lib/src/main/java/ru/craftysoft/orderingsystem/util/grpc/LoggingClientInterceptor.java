package ru.craftysoft.orderingsystem.util.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.craftysoft.orderingsystem.util.uuid.UuidUtils;

import static java.util.Optional.ofNullable;
import static ru.craftysoft.orderingsystem.util.http.HttpHeader.X_B3_SPAN_ID;
import static ru.craftysoft.orderingsystem.util.http.HttpHeader.X_B3_TRACE_ID;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.SPAN_ID;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.TRACE_ID;

public class LoggingClientInterceptor implements ClientInterceptor {

    private final Metadata extraMetadata;
    private final JsonFormat.Printer printer = JsonFormat.printer();

    private static final String GRPC_REQUEST_ID = "grpcRequestId";

    private static final Logger requestLogger = LoggerFactory.getLogger("ru.craftysoft.orderingsystem.util.grpc.client.request");
    private static final Logger responseLogger = LoggerFactory.getLogger("ru.craftysoft.orderingsystem.util.grpc.client.response");

    public LoggingClientInterceptor(Metadata extraMetadata) {
        this.extraMetadata = extraMetadata;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        var newCall = next.newCall(method, callOptions);
        return new LoggingClientCall<>(newCall, UuidUtils.generateDefaultUuid(), method);
    }

    private class LoggingClientCall<ReqT, RespT> extends ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT> {

        private final String grpcRequestId;
        private final MethodDescriptor<ReqT, RespT> method;
        private final String spanId;
        private final String traceId;

        protected LoggingClientCall(ClientCall<ReqT, RespT> delegate, String grpcRequestId, MethodDescriptor<ReqT, RespT> method) {
            super(delegate);
            this.grpcRequestId = grpcRequestId;
            this.method = method;
            this.spanId = ofNullable(extraMetadata)
                    .map(a -> a.get(Metadata.Key.of(X_B3_SPAN_ID, Metadata.ASCII_STRING_MARSHALLER)))
                    .orElse(null);
            this.traceId = ofNullable(extraMetadata)
                    .map(a -> a.get(Metadata.Key.of(X_B3_TRACE_ID, Metadata.ASCII_STRING_MARSHALLER)))
                    .orElse(null);
        }

        @Override
        public void sendMessage(ReqT message) {
            if (requestLogger.isDebugEnabled() && message instanceof MessageOrBuilder messageOrBuilder) {
                var oldMdc = MDC.getCopyOfContextMap();
                MDC.put(GRPC_REQUEST_ID, grpcRequestId);
                MDC.put(SPAN_ID, spanId);
                MDC.put(TRACE_ID, traceId);
                try {
                    var json = printer.print(messageOrBuilder);
                    requestLogger.debug("method={}\nrequest={}", method.getBareMethodName(), json);
                } catch (InvalidProtocolBufferException e) {
                    requestLogger.error("LoggingClientInterceptor.sendMessage.thrown ошибка при преобразовании в JSON", e);
                } catch (Exception e) {
                    requestLogger.error("LoggingClientInterceptor.sendMessage.thrown", e);
                } finally {
                    MDC.setContextMap(oldMdc);
                }
            }
            super.sendMessage(message);
        }

        @Override
        public void start(Listener<RespT> responseListener, Metadata headers) {
            var listener = new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {
                @Override
                public void onMessage(RespT message) {
                    if (responseLogger.isDebugEnabled() && message instanceof MessageOrBuilder messageOrBuilder) {
                        var oldMdc = MDC.getCopyOfContextMap();
                        MDC.put(GRPC_REQUEST_ID, grpcRequestId);
                        MDC.put(SPAN_ID, spanId);
                        MDC.put(TRACE_ID, traceId);
                        try {
                            var json = printer.print(messageOrBuilder);
                            responseLogger.debug("method={}\nresponse={}", method.getBareMethodName(), json);
                        } catch (InvalidProtocolBufferException e) {
                            requestLogger.error("LoggingClientInterceptor.sendMessage.thrown ошибка при преобразовании в JSON", e);
                        } catch (Exception e) {
                            requestLogger.error("LoggingClientInterceptor.sendMessage.thrown", e);
                        } finally {
                            MDC.setContextMap(oldMdc);
                        }
                    }
                    super.onMessage(message);
                }
            };
            ofNullable(extraMetadata).ifPresent(headers::merge);
            super.start(listener, headers);
        }
    }
}
