package by.botyanov.resourcepool;

import by.botyanov.resourcepool.exception.PoolException;
import by.botyanov.resourcepool.impl.synchronizer.SynchronizedResourcePool;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws PoolException {
        ResourcePool<Thread> threadResourcePool = new SynchronizedResourcePool<>(Thread.class, 10);
        for (int i = 0; i < 15; i++) {
            System.out.println(threadResourcePool.fetch(5, TimeUnit.SECONDS));
        }
    }

}
