package com.mrzsh;

import org.junit.Before;
import org.junit.Test;

import static com.mrzsh.AssertHelper.assertLongIn;
import static org.junit.Assert.*;

public class TimerTest {

    Timer timer;


    @Before
    public void init() {
        timer = Timer.singleThreadTimer();
    }

    @Test
    public void setTimeout() throws InterruptedException {
        MutableLong elapsed = MutableLong.hold(System.currentTimeMillis());
        timer.setTimeout((val)->{
            elapsed.subBy(System.currentTimeMillis());
        }, 10, 1000);
        timer.waitShutdown();
        assertLongIn(elapsed.get(), 1000, 1010);
    }

    @Test
    public void setInterval() {

    }

    @Test
    public void clearTimeout() {
    }

    @Test
    public void clearInterval() {
    }

    @Test
    public void asycTimeout() throws InterruptedException {
        String[] hold = new String[1];
        timer.waitStart();
        Promise.resolve(100);
        MutableLong elapsed = MutableLong.hold(System.currentTimeMillis());
        timer.asycTimeout("123", 1000)
                .then((val)->{
                    elapsed.subBy(System.currentTimeMillis());
                    hold[0] = val;
                });
        timer.waitShutdown();
        assertLongIn(elapsed.get(), 1000, 1010);
        assertEquals(hold[0], "123");

    }
}