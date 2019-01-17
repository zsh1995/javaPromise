package com.mrzsh;


public interface AsycTask <T, R> {

    Promise<R> asycAccept(T val);
}
