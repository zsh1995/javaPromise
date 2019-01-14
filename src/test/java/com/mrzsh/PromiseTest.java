package com.mrzsh;

import org.junit.Before;
import org.junit.Test;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.Assert.*;
import java.text.MessageFormat;

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
        MutableLong start = MutableLong.hold(System.currentTimeMillis());
        Promise.all(Arrays.asList(
                timer.asycTimeout(1,1000),
                timer.asycTimeout(2,2000),
                timer.asycTimeout(3,3000),
                timer.asycTimeout(4,4000)
        ))
                .then((val)->{
                    start.subed(System.currentTimeMillis());
                    assertTrue(val.equals(Arrays.asList(1,2,3,4)));
                });
        timer.waitShutdown();
        assertLongIn(start.get() , 4000L, 4010L);
    }

    private static void assertLongIn(long acutal, long... intervals) {
        if(intervals.length == 0) fail("no interval");
        if(intervals.length >= 1) {
            if(acutal < intervals[0]) fail(MessageFormat.format("{0} is smaller then interval left {1}",
                    acutal, intervals[0]));
        }
        if(intervals.length >= 2) {
            if(acutal > intervals[1]) fail(MessageFormat.format("{0} is larger then interval right {1}",
                    acutal, intervals[1]));
        }
    }

    @Test
    public void race() throws InterruptedException {
        MutableLong value = MutableLong.hold(0);
        MutableLong start = MutableLong.hold(System.currentTimeMillis());
        Promise.race(Arrays.asList(
                timer.asycTimeout(1,1000),
                timer.asycTimeout(2,2000),
                timer.asycTimeout(3,3000),
                timer.asycTimeout(4,4000)
        ))
        .then((val)->{
            value.set(val);
            start.subed(System.currentTimeMillis());
        });
        timer.waitShutdown();
        assertEquals(1L, value.get());
        assertLongIn(start.get(), 1000, 1010);
    }

    @Test
    public void then() throws InterruptedException {
        int[] holders = new int[2];
        MutableLong start = MutableLong.hold(System.currentTimeMillis());
        timer.asycTimeout(1, 1000)
                .then((AsycTask<Integer, Integer>) (val)->{
                    holders[0] = val;
                    return timer.asycTimeout(2, 1000);
                })
                .then((val)->{
                    holders[1] = val;
                    start.subed(System.currentTimeMillis());
                });
        timer.waitShutdown();
        assertArrayEquals(holders, new int[] {1,2});
        assertLongIn(start.get(), 2000, 2010);
    }

    @Test
    public void acatch() throws InterruptedException {
        String[] holder = new String[1];
        timer.asycTimeout(1, 1000)
                .then((AsycConsumer<Integer>)(val)->{
                    throw new Exception("erro");
                })
                .acatch((erro)->{
                    holder[0] = erro.getMessage();
                });
        timer.waitShutdown();
        assertEquals(holder[0], "erro");
    }

    @Test
    public void then1() throws InterruptedException{
        String[] holder = new String[1];
        MutableLong test = MutableLong.hold(0);
        timer.asycTimeout(1, 1000)
                .then((Function<Integer, Integer>) (val)->{
                    return 1;
                })
                .then((val)->{
                    if(val == 1){
                        test.set(1);
                    }
                });
        timer.waitShutdown();
        assertEquals(test.get(), 1);

    }
}