package by.botyanov.resourcepool.impl.collection;

import by.botyanov.resourcepool.ResourcePool;
import by.botyanov.resourcepool.exception.PoolException;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConcurrentLinkedQueueResourcePool<T> implements ResourcePool<T> {

    private ConcurrentLinkedQueue<T> free;
    private ConcurrentLinkedQueue<T> busy;

    private AtomicBoolean closed = new AtomicBoolean(true);

    public ConcurrentLinkedQueueResourcePool(Class<T> clazz, int initialSize) throws PoolException {
        free = new ConcurrentLinkedQueue<>();
        busy = new ConcurrentLinkedQueue<>();
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
        T resource = free.poll();
        if (resource != null) {
            busy.add(resource);
            return resource;
        } else {
            throw new PoolException("Can't take a resource from pool with given timeout");
        }
    }

    @Override
    public T fetch(long timeout, TimeUnit unit) throws PoolException {
        if (closed.get()) {
            throw new PoolException("Pool is already closed!");
        }
        long endNanos = System.nanoTime() + unit.toNanos(timeout);
        T resource = null;
        while (System.nanoTime() < endNanos && resource == null) {
            resource = fetch();
        }
        if (resource != null) {
            busy.add(resource);
            return resource;
        } else {
            throw new PoolException("Can't take a resource from pool with given timeout");
        }
    }

    @Override
    public T fetchOrNull(long timeout, TimeUnit unit) {
        if (closed.get()) {
            return null;
        }
        long endNanos = System.nanoTime() + unit.toNanos(timeout);
        T resource = null;
        while (System.nanoTime() < endNanos && resource == null) {
            try {
                resource = fetch();
            } catch (PoolException e) {
                return null;
            }
        }
        if (resource != null) {
            busy.add(resource);
        }
        return resource;
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
