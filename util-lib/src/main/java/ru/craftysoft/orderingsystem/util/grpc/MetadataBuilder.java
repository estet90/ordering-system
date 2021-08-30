package ru.craftysoft.orderingsystem.util.grpc;

import io.grpc.Metadata;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;
import static ru.craftysoft.orderingsystem.util.http.HttpHeader.X_B3_SPAN_ID;
import static ru.craftysoft.orderingsystem.util.http.HttpHeader.X_B3_TRACE_ID;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.SPAN_ID;
import static ru.craftysoft.orderingsystem.util.mdc.MdcKey.TRACE_ID;

@NoArgsConstructor(access = PRIVATE)
public class MetadataBuilder {

    public static Metadata build(@Nullable Map<String, String> mdc) {
        var metadata = new Metadata();
        if (mdc == null) {
            return metadata;
        }
        ofNullable(mdc.get(TRACE_ID)).ifPresent(value -> metadata.put(Metadata.Key.of(X_B3_TRACE_ID, Metadata.ASCII_STRING_MARSHALLER), value));
        ofNullable(mdc.get(SPAN_ID)).ifPresent(value -> metadata.put(Metadata.Key.of(X_B3_SPAN_ID, Metadata.ASCII_STRING_MARSHALLER), value));
        return metadata;
    }

}
