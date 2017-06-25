package by.botyanov.resourcepool.impl.collection;

import by.botyanov.resourcepool.exception.PoolException;
import by.botyanov.resourcepool.ResourcePool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class LinkedBlockingQueueResourcePool<T> implements ResourcePool<T> {

    private BlockingQueue<T> free;
    private BlockingQueue<T> busy;

    private AtomicBoolean closed = new AtomicBoolean(true);

    public LinkedBlockingQueueResourcePool(Class<T> clazz, int initialSize) throws PoolException {
        free = new LinkedBlockingQueue<>(initialSize);
        busy = new LinkedBlockingQueue<>(initialSize);
        try {
            for (int i = 0; i < initialSize; i++) {
                free.add(clazz.newInstance());
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new PoolException("Can't initialize pool", e);
        }
        closed.set(false);
    }

    @Override
    public T fetch() throws PoolException {
        if (closed.get()) {
            throw new PoolException("Pool is already closed!");
        }
        try {
            T resource = free.take();
            if (resource != null) {
                busy.add(resource);
            }
            return resource;
        } catch (InterruptedException e) {
            throw new PoolException("Can't take a resource from pool", e);
        }
    }

    @Override
    public T fetch(long timeout, TimeUnit unit) throws PoolException {
        if (closed.get()) {
            throw new PoolException("Pool is already closed!");
        }
        try {
            T resource = free.poll(timeout, unit);
            if (resource == null) {
                throw new PoolException("Can't take a resource from pool");
            }
            busy.add(resource);
            return resource;
        } catch (InterruptedException e) {
            throw new PoolException("Can't take a resource from pool with given timeout", e);
        }
    }

    @Override
    public T fetchOrNull(long timeout, TimeUnit unit) {
        if (closed.get()) {
            return null;
        }
        try {
            return free.poll(timeout, unit);
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public void release(T pooled) {
        if (busy.remove(pooled)) {
            free.add(pooled);
        }
    }

    @Override
    public void close() {
        closed.set(true);
    }

}
