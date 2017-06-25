package by.botyanov.resourcepool.impl.synchronizer;

import by.botyanov.resourcepool.ResourcePool;
import by.botyanov.resourcepool.exception.PoolException;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockConditionResourcePool<T> implements ResourcePool<T> {

    private Queue<T> free;
    private Queue<T> busy;

    private AtomicBoolean closed = new AtomicBoolean(true);

    private final Lock lock = new ReentrantLock();

    private final Condition isResourceAvailableSignal = lock.newCondition();
    private final Condition noResourcesInUseSignal = lock.newCondition();

    public LockConditionResourcePool(Class<T> clazz, int initialSize) throws PoolException {
        free = new LinkedList<>();
        busy = new LinkedList<>();
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
            lock.lock();
            while (free.isEmpty()) {
                isResourceAvailableSignal.await();
            }
            return getAvailableResource();
        } catch (InterruptedException e) {
            throw new PoolException("Can't take a resource from pool", e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public T fetch(long timeout, TimeUnit unit) throws PoolException {
        if (closed.get()) {
            throw new PoolException("Pool is already closed!");
        }
        long tryLockStart = System.currentTimeMillis();
        boolean gotLock = false;
        try {
            T resource = null;
            if (gotLock = lock.tryLock(timeout, unit)) {
                long getLockTime = System.currentTimeMillis() - tryLockStart;
                if (waitForResource(timeout - getLockTime, unit)) {
                    resource = getAvailableResource();
                }
            }
            return resource;
        } catch (InterruptedException e) {
            throw new PoolException("Can't take a resource from pool with given timeout", e);
        } finally {
            if (gotLock) {
                lock.unlock();
            }
        }
    }

    @Override
    public T fetchOrNull(long timeout, TimeUnit unit) {
        if (closed.get()) {
            return null;
        }
        long tryLockStart = System.currentTimeMillis();
        boolean gotLock = false;
        try {
            T resource = null;
            if (gotLock = lock.tryLock(timeout, unit)) {
                long getLockTime = System.currentTimeMillis() - tryLockStart;
                if (waitForResource(timeout - getLockTime, unit)) {
                    resource = getAvailableResource();
                }
            }
            return resource;
        } catch (InterruptedException e) {
            return null;
        } finally {
            if (gotLock) {
                lock.unlock();
            }
        }
    }

    private boolean waitForResource(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return !free.isEmpty() || isResourceAvailableSignal.await(timeout, timeUnit);
    }

    private T getAvailableResource() {
        if (closed.get()) {
            return null;
        }
        T resource = free.poll();
        if (resource != null) {
            busy.add(resource);
        }
        return resource;
    }

    @Override
    public void release(T pooled) {
        try {
            lock.lock();
            if (busy.remove(pooled) && free.add(pooled)) {
                isResourceAvailableSignal.signalAll();
                if (busy.isEmpty()) noResourcesInUseSignal.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws PoolException {
        if (closed.get()) {
            return;
        }
        try {
            lock.lock();
            if (!busy.isEmpty()) {
                noResourcesInUseSignal.await();
            }
            closed.set(true);
        } catch (InterruptedException e) {
            throw new PoolException("Interrupted while closing pool", e);
        } finally {
            lock.unlock();
        }
    }

}
