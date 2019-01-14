package com.mrzsh;

public interface AsycConsumer<T> {

    void consume(T val) throws Exception;

}
