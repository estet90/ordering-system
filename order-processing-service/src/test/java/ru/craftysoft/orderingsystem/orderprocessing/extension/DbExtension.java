package ru.craftysoft.orderingsystem.orderprocessing.extension;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ru.craftysoft.orderingsystem.orderprocessing.testcontainer.DbContainer;

public class DbExtension implements BeforeEachCallback, AfterEachCallback {
    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        DbContainer.INSTANCE.start();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        DbContainer.INSTANCE.stop();
    }
}
