package com.mrzsh;

import java.util.ArrayList;
import java.util.List;
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

    private Consumer<Throwable> reject;

    private BiConsumer<Consumer<T>, Consumer<Throwable>> executor;

    private Throwable reason;


    private T pval;

    private Throwable cause() {
        return reason;
    }

    private void finished(T val) {
        if(!isPending()) return;
        pval = val;
        fulfilled();
        if(handler != null) {
            handler.accept(pval);
        }
    }

    private void fulfilled() {
        this.status = Status.fulfilled;
    }

    private void rejected() {
        this.status = Status.rejected;
    }

    private boolean isPending(){
        return this.status == Status.pending;
    }

    private boolean isFulfilled() {
        return this.status == Status.fulfilled;
    }

    private boolean isRejected() {
        return this.status == Status.rejected;
    }

    private Consumer<Throwable> exceptionHandler;
    private void fail(Throwable e) {
        status = Status.rejected;
        reason = e;
        if(exceptionHandler != null) {
            exceptionHandler.accept(reason);
        }
    }

    private Promise() {
        this.resolve = (T val) -> finished(val);
        this.reject = (Throwable excp) -> fail(excp);
    }

    public Promise(BiConsumer<Consumer<T>, Consumer<Throwable>> executor) {
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

    private static class SimpleCountDown{
        int remain;
        int settingVal;
        SimpleCountDown(int init) {
            settingVal = init;
            remain = init;
        }
        boolean countDown() {
            remain--;
            return remain <= 0;
        }
    }
    public static <T> Promise<List<T>> all(List<Promise<T>> args){
        Promise<List<T>> p = new Promise<>((resolv, reject)->{
            List<T> result = new ArrayList<>();
            SimpleCountDown latch = new SimpleCountDown(args.size());
            for(Promise<T> arg : args) {
                arg.then((val)->{
                    result.add(val);
                    if(latch.countDown()) {
                        resolv.accept(result);
                    }
                    return null;
                });
            }
        });
        return p;
    }

    public static <T> Promise<T> race(List<Promise<T>> args){
        Promise<T> p = new Promise<T>((resolv, reject)->{
            for(Promise<T> arg : args) {
                arg.then((val)->{
                    resolv.accept(val);
                    return null;
                });
            }
        });
        return p;
    }

    private Consumer<T> handler;
    private void setHanlder(Consumer<T> handler) {
        if(this.handler == null && this.status == Status.pending)
            this.handler = handler;
        else if(this.status == Status.fulfilled){
            handler.accept(pval);
        }
    }

    private void setExceptionHandler(Consumer<Throwable> handler) {
        if(this.exceptionHandler == null && this.status == Status.pending) {
            this.exceptionHandler = handler;
        } else  if(this.status == Status.rejected) {
            handler.accept(reason);
        }
    }

    public <R> Promise<R> then(AsycTask<T, R> onFulfilled) {
        // outter promise
        Promise<R> result = Promise.pending();
        setHanlder((val) -> {
            if(status == Status.fulfilled) {
                Promise<R> res;
                try {
                    res = onFulfilled.asycdo(pval);
                } catch (Throwable e) {
                    result.fail(e);
                    return;
                }
                if(res == null) {
                    res = Promise.<R>resolve(null);
                }
                // make the inner promise linked with outter promise
                res.setHanlder(result.resolve);
                res.setExceptionHandler(result.reject);
            } else {
                // todo 异常处理
                result.fail(this.cause());
            }
        });
        return result;
    }

    public <R> Promise<R> acatch(AsycTask<Throwable, R> onFail) {
        // todo 异常处理
        Promise<R> result = Promise.pending();
        setExceptionHandler((throwable)-> {
            if(status == Status.rejected) {
                Promise<R> res;
                try {
                    res = onFail.asycdo(throwable);
                } catch (Throwable e) {
                    result.fail(e);
                    return;
                }
                if(res == null) {
                    res = Promise.<R>resolve(null);
                }
                // make the inner promise linked with outter promise
                res.setHanlder(result.resolve);
                res.setExceptionHandler(result.reject);
            } else {
                // todo 异常处理
                result.fail(this.cause());
            }
        });
        return result;
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
//        }).then((val) -> {
//            if(val == null) System.out.println("is null");
//            return null;
//        });
        Promise<?> p1 = new Promise<>((res, rej) -> {
           System.out.println(1);
           res.accept(1);
        }).then((val)-> {
            System.out.println("phase 2 , val = " + val);

            throw new Exception("i am erro");
        }).acatch((throwable)->{
            System.out.println(throwable.getMessage());
            return null;
        }).then((val)->{
            if(val == null)
                System.out.println("is null");
            return null;
        });

    }

}
