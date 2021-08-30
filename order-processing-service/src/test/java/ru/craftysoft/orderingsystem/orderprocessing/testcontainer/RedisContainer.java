package ru.craftysoft.orderingsystem.orderprocessing.testcontainer;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.testcontainers.containers.GenericContainer;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.redislabs.testcontainers.support.AbstractRedisContainer.REDIS_PORT;

public enum RedisContainer {
    INSTANCE;

    private final AtomicBoolean isStarted = new AtomicBoolean(false);

    Consumer<CreateContainerCmd> cmd = e -> Objects.requireNonNull(e.getHostConfig())
            .withPortBindings(new PortBinding(Ports.Binding.bindPort(REDIS_PORT + 10), new ExposedPort(REDIS_PORT)));
    public final GenericContainer<?> redis = new com.redislabs.testcontainers.RedisContainer()
            .withCreateContainerCmdModifier(cmd);

    public String getHosts() {
        return isStarted.get()
                ? "redis://" + redis.getHost() + ":" + redis.getFirstMappedPort()
                : "redis://localhost:1000";
    }

    public void start() {
        if (!this.isStarted.compareAndSet(false, true)) {
            return;
        }
        redis.start();
    }

    public void stop() {
        if (!this.isStarted.compareAndSet(true, false)) {
            return;
        }
        redis.stop();
    }

    public AtomicBoolean getIsStarted() {
        return isStarted;
    }
}
