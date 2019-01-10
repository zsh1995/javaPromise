package com.mrzsh;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class PromiseTest {

    Timer timer;

    @Before
    public void initTimer() {
        timer = Timer.singleThreadTimer();
    }

    @Test
    public void resolve() {

    }

    @Test
    public void all() throws InterruptedException {
        long start = System.currentTimeMillis();
        Promise.all(Arrays.asList(
                timer.asycTimeout(1,1000),
                timer.asycTimeout(2,2000),
                timer.asycTimeout(3,3000),
                timer.asycTimeout(4,4000)
        ))
                .then((val)->{
                    System.out.println(val);
                    assertTrue(val.equals(Arrays.asList(1,2,3,4)));
                    return null;
                });
        timer.waitShutdown();
        long elapsed = System.currentTimeMillis() - start;
        assertEquals(true, elapsed >= 4000L && elapsed < 4500L);
    }

    @Test
    public void race() throws InterruptedException {
        int[] value= new int[1];
        long start = System.currentTimeMillis();
        Promise.race(Arrays.asList(
                timer.asycTimeout(1,1000),
                timer.asycTimeout(2,2000),
                timer.asycTimeout(3,3000),
                timer.asycTimeout(4,4000)
        ))
        .then((val)->{
            value[0] = val;

            return null;
        });
        timer.waitShutdown();
        long elapsed = System.currentTimeMillis() - start;
        assertEquals(1, value[0]);
//        assertEquals(true, elapsed >= 1000L && elapsed < 2000L);
    }

    @Test
    public void then() throws InterruptedException {
        int[] holders = new int[2];
        long start = System.currentTimeMillis();
        timer.asycTimeout(1, 1000)
                .then((val)->{
                    holders[0] = val;
                    return timer.asycTimeout(2, 1000);
                })
                .then((val)->{
                    holders[1] = val;
                    return null;
                });
        timer.waitShutdown();
        long elapsed = System.currentTimeMillis() - start;
        assertArrayEquals(holders, new int[] {1,2});
        assertEquals(true, elapsed >= 2000L && elapsed < 2500L);
    }

    @Test
    public void acatch() throws InterruptedException {
        String[] holder = new String[1];
        timer.asycTimeout(1, 1000)
                .then((val)->{
                    throw new Exception("erro");
                })
                .acatch((erro)->{
                    holder[0] = erro.getMessage();
                    return null;
                });
        timer.waitShutdown();
        assertEquals(holder[0], "erro");
    }
}