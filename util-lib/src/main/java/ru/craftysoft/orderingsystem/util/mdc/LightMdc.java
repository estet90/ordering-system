package ru.craftysoft.orderingsystem.util.mdc;

import org.slf4j.spi.MDCAdapter;

import java.util.Map;
import java.util.stream.Stream;

public enum LightMdc implements MDCAdapter {
    INSTANCE;
    private final ThreadLocal<Map<String, String>> mdc = ThreadLocal.withInitial(Map::of);

    @Override
    public void put(String key, String val) {
        var oldEntries = mdc.get().entrySet()
                .stream()
                .filter(e -> !e.getKey().equals(key));
        var newValue = val == null ? "null" : val;
        var newEntries = Stream.concat(oldEntries, Stream.of(Map.entry(key, newValue)))
                .toArray(Map.Entry[]::new);
        mdc.set(Map.ofEntries(newEntries));
    }

    @Override
    public String get(String key) {
        return mdc.get().get(key);
    }

    @Override
    public void remove(String key) {
        var newEntries = mdc.get().entrySet()
                .stream()
                .filter(e -> !e.getKey().equals(key))
                .toArray(Map.Entry[]::new);
        mdc.set(Map.ofEntries(newEntries));
    }

    @Override
    public void clear() {
        mdc.set(Map.of());
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
        return mdc.get();
    }

    @Override
    public void setContextMap(Map<String, String> contextMap) {
        mdc.set(contextMap);
    }
}
