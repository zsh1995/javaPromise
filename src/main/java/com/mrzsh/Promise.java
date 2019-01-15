package com.mrzsh;

import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

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
                });
            }
        });
        return p;
    }

    private Consumer<T> handler;
    private void setHandler(Consumer<T> handler) {
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
        return then(onFulfilled, null);
    }

    public <R> Promise<R> then(AsycTask<T, R> onFulfilled , AsycTask<Throwable, R> onReject) {
        // outer promise
        Promise<R> result = Promise.pending();
        setHandler((val) -> {
            compose(Promise::isFulfilled, result, createAsycTaskConsumer(onFulfilled, val));
        });
        if(onReject != null) {
            setExceptionHandler((error)->{
                compose(Promise::isRejected, result, createAsycTaskConsumer(onReject, error));
            });
        }
        return result;
    }

    /**
     * accept a asycconsumer(no return value)
     * @param onFulfilled
     * @param <R>
     * @return
     */
    public <R> Promise<R> then(AsycConsumer<T> onFulfilled) {
        return then(adpatToAsyc(onFulfilled));
    }

    public <R> Promise<R> then(Function<T, R> onFulfilled) {
        return then(adaptFunc(onFulfilled));
    }

    private <R> void compose(Predicate<Promise<T>> condition,
                                   Promise<R> result,
                                   Consumer<Promise<R>> consumer) {
        if(condition.test(this)) {
            consumer.accept(result);
        } else {
            result.fail(this.cause());
        }
    }

    private <T, R> Consumer<Promise<R>> createAsycTaskConsumer(AsycTask<T, R> event, T val){
        return (Promise<R> result)->{
            Promise<R> res;
            try {
                res = event.asycAccept(val);
            } catch (Throwable e) {
                result.fail(e);
                return;
            }
            if(res == null) {
                res = Promise.<R>resolve(null);
            }
            // make the inner promise linked with outter promise
            res.setHandler(result.resolve);
            res.setExceptionHandler(result.reject);
        };
    }

    private <T, R> AsycTask<T, R> adpatToAsyc(AsycConsumer<T> src) {
        return (T val) -> {
            src.consume(val);
            return null;
        };
    }

    private <T, R> AsycTask<T, R> adpatToAsyc(Function<T, R> src) {
        return (T val) -> Promise.resolve(src.apply(val));
    }


    private <T, R> AsycTask<T, R> adaptFunc(Function<T, R> src) {
        return (T val) -> Promise.resolve(src.apply(val));
    }

    public <R> Promise<R> acatch(AsycTask<Throwable, R> onFail) {
        // todo 异常处理
        Promise<R> result = Promise.pending();
        setExceptionHandler((throwable)-> {
            compose(Promise::isRejected, result, createAsycTaskConsumer(onFail, throwable));
        });
        return result;
    }

    public <R> Promise<R> acatch(AsycConsumer<Throwable> onFail) {
        return acatch(adpatToAsyc(onFail));
    }
    public <R> Promise<R> acatch(Function<Throwable, R> onFail) {
        return acatch(adpatToAsyc(onFail));
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
        }).then((AsycConsumer<Object>) (val)-> {
            System.out.println("phase 2 , val = " + val);
            throw new Exception("i am error");
        }).acatch((throwable)->{
            System.out.println(throwable.getMessage());
        }).then((val)->{
            if(val == null)
                System.out.println("is null");
        });

    }

}
