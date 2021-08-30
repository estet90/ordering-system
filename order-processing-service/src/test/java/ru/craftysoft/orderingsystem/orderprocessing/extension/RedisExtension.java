package ru.craftysoft.orderingsystem.orderprocessing.extension;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ru.craftysoft.orderingsystem.orderprocessing.testcontainer.RedisContainer;

public class RedisExtension implements BeforeEachCallback, AfterEachCallback {
    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        RedisContainer.INSTANCE.start();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        RedisContainer.INSTANCE.stop();
    }
}
