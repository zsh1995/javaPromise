package com.mrzsh;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @program: javapromise
 * @description:
 * @author: Mr.zsh
 * @create: 2019-01-02 11:54
 **/
public class Promise <T> {
    enum Status {
        pending, fulfilled, rejected
    }

    private Status status = Status.pending;

    private Consumer<T> resolve;

    private Consumer<Exception> reject;

    private BiConsumer<Consumer<T>, Consumer<Exception>> executor;



    private T pval;

    private void finished(T val) {
        pval = val;
        status = Status.fulfilled;
        if(handler != null) {
            handler.accept(pval);
        }
    }

    private void fail() {
        status = Status.rejected;
    }

    private Promise() {
        this.resolve = (T val) -> finished(val);
        this.reject = (Exception excp) -> fail();
    }

    public Promise(BiConsumer<Consumer<T>, Consumer<Exception>> executor) {
        this();
        this.executor = executor;
        executor.accept(resolve, reject);
    }


    public static <T> Promise<T> resolve(T val) {
        return new Promise<T>((res, rej) -> {
            res.accept(val);
        });
    }

    private static <T> Promise<T> pending() {
        return new Promise<>();
    }
    private Consumer<T> handler;
    private void setHanlder(Consumer<T> handler) {
        if(this.handler == null && this.status == Status.pending)
            this.handler = handler;
        else if(this.status == Status.fulfilled){
            handler.accept(pval);
        }
    }

    public <R> Promise<R> then(AsycTask<T, R> onFulfilled) {
        // outter promise
        Promise<R> result = Promise.<R>pending();
        setHanlder((val) -> {
            if(status == Status.fulfilled) {
                Promise<R> res = onFulfilled.asycdo(pval);
                if(res == null) {
                    Promise.<R>resolve(null).setHanlder(result.resolve);
                } else {
                    // make the inner promise linked with outter promise
                    res.setHanlder(result.resolve);

                }
            } else {
                // todo 异常处理
            }
        });
        return result;
    }

    public Promise<T> acatch(Consumer<Exception> aerro) {
        // todo 异常处理
        return null;
    }

    public static void main(String[] args) {
//        Promise<?> p1 = new Promise<>((res, rej)-> {
//            System.out.println("phase 1");
//            res.accept(1);
//        }).then((val) -> {
//            System.out.println("phase 2, preval = " + val);
//            return Promise.resolve("hello");
//        }).then((val) -> {
//            System.out.println("phase 3, preval = " + val);
//            return null;
//        });
        Promise<?> p1 = new Promise<>((res, rej) -> {
           System.out.println(1);
           res.accept(1);
        }).then((val)-> {
            System.out.println("phase 2 , val = " + val);
            return null;
        });

    }

}
