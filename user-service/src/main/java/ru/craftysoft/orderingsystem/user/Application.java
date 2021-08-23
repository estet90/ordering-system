package ru.craftysoft.orderingsystem.user;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Application {

    public static void main(String[] args) {
        var component = DaggerApplicationComponent.builder().build();
        var server = component.grpcServer();
        try {
            server.start();
        } catch (IOException e) {
            log.error("Ошибка при запуске сервера", e);
            try {
                server.shutdown().awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                log.error("Ошибка при остановке сервера", ex);
            }
        }
        log.info("Сервер запущен");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.shutdown().awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error("Ошибка при остановке сервера", e);
            }
            log.info("Сервер остановлен");
        }));
        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            log.error("Ошибка при остановке сервера", e);
        }
    }

}
