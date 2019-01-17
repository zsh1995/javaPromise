package com.mrzsh;

import java.text.MessageFormat;

import static org.junit.Assert.fail;

/**
 * @program: javapromise
 * @description: assert helper
 * @author: Mr.zsh
 * @create: 2019-01-15 15:13
 **/
public class AssertHelper {

    public static void assertLongIn(long actual, long... intervals) {
        if(intervals.length == 0) fail("no interval");
        if(intervals.length >= 1) {
            if(actual < intervals[0]) fail(MessageFormat.format("{0} is smaller then interval left {1}",
                    actual, intervals[0]));
        }
        if(intervals.length >= 2) {
            if(actual > intervals[1]) fail(MessageFormat.format("{0} is larger then interval right {1}",
                    actual, intervals[1]));
        }
    }
}
