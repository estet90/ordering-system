package ru.craftysoft.orderingsystem.util.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@Module
public class JacksonModule {

    @Provides
    @Singleton
    static Jackson jackson(Set<Consumer<ObjectMapper>> customizers) {
        var mapper = new ObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .findAndRegisterModules();
        for (var customizer : customizers) {
            customizer.accept(mapper);
        }
        return new Jackson(mapper);
    }

    @Provides
    @ElementsIntoSet
    static Set<Consumer<ObjectMapper>> customizers() {
        return new HashSet<>();
    }

}
