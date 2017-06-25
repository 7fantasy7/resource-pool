package by.botyanov.resourcepool.impl.synchronizer;

import by.botyanov.resourcepool.ResourcePool;
import by.botyanov.resourcepool.exception.PoolException;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SynchronizedResourcePool<T> implements ResourcePool<T> {

    private Queue<T> queue;
    private Queue<T> busy;

    private AtomicBoolean closed = new AtomicBoolean(true);

    public SynchronizedResourcePool(Class<T> clazz, int initialSize) throws PoolException {
        queue = new LinkedList<>();
        busy = new LinkedList<>();
        try {
            for (int i = 0; i < initialSize; i++) {
                queue.add(clazz.newInstance());
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new PoolException("Can't initialize pool", e);
        }
        closed.set(false);
    }

    @Override
    public synchronized T fetch() throws PoolException {
        if (closed.get()) {
            throw new PoolException("Pool is already closed!");
        }
        return getAvailableResource();
    }

    @Override
    public synchronized T fetch(long timeout, TimeUnit unit) throws PoolException {
        if (closed.get()) {
            throw new PoolException("Pool is already closed!");
        }
        long endNanos = System.nanoTime() + unit.toNanos(timeout);
        T resource = null;
        while (System.nanoTime() < endNanos && resource == null) {
            resource = fetch();
        }
        if (resource != null) {
            return resource;
        } else {
            throw new PoolException("Can't take a resource from pool with given timeout");
        }
    }

    @Override
    public synchronized T fetchOrNull(long timeout, TimeUnit unit) {
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
            return resource;
        } else {
            return null;
        }
    }

    private T getAvailableResource() {
        if (closed.get()) {
            return null;
        }
        T resource = queue.poll();
        if (resource != null) {
            busy.add(resource);
        }
        return resource;
    }

    @Override
    public synchronized void release(T pooled) {
        if (busy.remove(pooled)) {
            queue.add(pooled);
        }
    }

    @Override
    public synchronized void close() {
        closed.set(true);
    }

}
