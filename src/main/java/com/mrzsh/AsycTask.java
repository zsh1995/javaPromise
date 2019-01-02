package com.mrzsh;


public interface AsycTask <T, R> {

    Promise<R> asycdo(T val);
}
