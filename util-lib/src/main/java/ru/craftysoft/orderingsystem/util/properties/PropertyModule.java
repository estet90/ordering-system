package ru.craftysoft.orderingsystem.util.properties;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class PropertyModule {

    @Provides
    @Singleton
    static PropertyResolver propertyResolver() {
        return new PropertyResolver("classpath:/application.properties");
    }

}
