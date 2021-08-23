package ru.craftysoft.orderingsystem.util.grpc;

import io.grpc.Context;
import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;

public class ContextualizedServerCallListener<ReqT> extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
    private final Context context;

    public ContextualizedServerCallListener(ServerCall.Listener<ReqT> delegate, Context context) {
        super(delegate);
        this.context = context;
    }

    @Override
    public void onMessage(ReqT message) {
        var previous = context.attach();
        try {
            super.onMessage(message);
        } finally {
            context.detach(previous);
        }
    }

    @Override
    public void onHalfClose() {
        var previous = context.attach();
        try {
            super.onHalfClose();
        } finally {
            context.detach(previous);
        }
    }

    @Override
    public void onCancel() {
        var previous = context.attach();
        try {
            super.onCancel();
        } finally {
            context.detach(previous);
        }
    }

    @Override
    public void onComplete() {
        var previous = context.attach();
        try {
            super.onComplete();
        } finally {
            context.detach(previous);
        }
    }

    @Override
    public void onReady() {
        var previous = context.attach();
        try {
            super.onReady();
        } finally {
            context.detach(previous);
        }
    }
}
