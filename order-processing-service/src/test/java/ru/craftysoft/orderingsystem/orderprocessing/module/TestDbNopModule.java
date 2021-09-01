package ru.craftysoft.orderingsystem.orderprocessing.module;

import dagger.Module;
import dagger.Provides;
import ru.craftysoft.orderingsystem.util.db.DbHelper;

import javax.inject.Singleton;

@Module
public class TestDbNopModule {

    @Provides
    @Singleton
    static DbHelper dbHelper() {
        return new DbHelper(null);
    }

}
