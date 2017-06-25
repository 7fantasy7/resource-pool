package by.botyanov.resourcepool;

import by.botyanov.resourcepool.exception.PoolException;

import java.util.concurrent.TimeUnit;

public interface ResourcePool<T> {

    T fetch() throws PoolException;

    T fetch(long timeout, TimeUnit unit) throws PoolException;

    T fetchOrNull(long timeout, TimeUnit unit);

    void release(T pooled);

    void close() throws PoolException;

}
