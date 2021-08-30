package ru.craftysoft.orderingsystem.orderprocessing.module;

import dagger.Module;
import dagger.Provides;
import ru.craftysoft.orderingsystem.util.properties.PropertyResolver;

import javax.inject.Singleton;

@Module
public class TestPropertyModule {

    @Provides
    @Singleton
    static PropertyResolver propertyResolver() {
        return new PropertyResolver("classpath:/application-test.properties");
    }

}
