package by.botyanov.resourcepool.impl.collection;

import by.botyanov.resourcepool.exception.PoolException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(BlockJUnit4ClassRunner.class)
public class ConcurrentLinkedQueueResourcePoolTest {

    private ConcurrentLinkedQueueResourcePool<Thread> pool;

    @Before
    public void setUp() throws PoolException {
        pool = new ConcurrentLinkedQueueResourcePool<>(Thread.class, 10);
    }

    @Test
    public void testFetch() throws InterruptedException, BrokenBarrierException {
        // given
        final CyclicBarrier gate = new CyclicBarrier(11);
        final Thread[] fetch = new Thread[11];
        final List<Thread> workers = new ArrayList<>();
        final boolean[] exceptionHappened = {false};
        for (int i = 0; i < 11; i++) {
            int iFinal = i;
            Thread thread = new Thread(() -> {
                try {
                    gate.await();
                    Thread fetch1 = pool.fetch();
                    fetch[iFinal] = fetch1;
                } catch (PoolException | InterruptedException | BrokenBarrierException e) {
                    exceptionHappened[0] = true;
                }
            });
            thread.start();
            workers.add(thread);
        }

        // when
        gate.await();
        workers.forEach(Thread::suspend);
//        workers.forEach((thread) -> {
//            try {
//                thread.join();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        });
        Thread.sleep(5000);

        // then
        for (int i = 0; i < 10; i++) {
            assertThat(fetch[i], is(notNullValue()));
        }
        assertThat(fetch[10], is(nullValue()));
        assertThat(exceptionHappened[0], is(false));
    }

    @Test
    public void testFetch1() {

    }

//    @Test
//    public void testFetchOrNull() {
//
//    }
//
//    @Test
//    public void testRelease() {
//
//    }
//
//    @Test
//    public void testClose() {
//
//    }

    @After
    public void tearDown() throws Exception {

    }

}
