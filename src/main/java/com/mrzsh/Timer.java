package com.mrzsh;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * @program: javapromise
 * @description:
 * @author: Mr.zsh
 * @create: 2019-01-02 16:30
 **/
public class Timer {

    ExecutorService pool;

    DelayQueue<DelayedTask<?>> delayTasks;

    private AtomicLong along = new AtomicLong(1000);

    private AtomicLong intervalId = new AtomicLong(1000);

    private Map<Long, DelayedTask<?>> timingMap = Collections.synchronizedMap(new HashMap<>());

    private Map<Long, Boolean> intervalMap = Collections.synchronizedMap(new HashMap<>());

    private ReentrantLock lock = new ReentrantLock();

    private Condition condition = lock.newCondition();


    int threadNum;

    private static class DelayedTask<T> implements Delayed {
        private Consumer<T> delayedConsumer;

        private T arg;

        private  boolean isCanceled = false;

        long targetTime;

        public DelayedTask(Consumer<T> consumer, T arg, int ms) {
            delayedConsumer = consumer;
            this.arg = arg;
            targetTime = System.currentTimeMillis() + ms;
        }

        public void invoke() {
//            System.out.println("invoke");
            if(!isCanceled) delayedConsumer.accept(arg);
        }

        public void cancel() {
            this.isCanceled = true;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long delta = targetTime - System.currentTimeMillis();
            return unit == TimeUnit.MILLISECONDS ? delta : unit.convert(delta, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            if(o instanceof DelayedTask) {
                if( o == this) return 0;
                DelayedTask d = (DelayedTask) o;
                long delay1 = this.targetTime;
                long delay2 = d.targetTime;
                return Long.compare(delay1, delay2);
            } else {
                return 1;
            }
        }
    }

    public static Timer singleThreadTimer() {
        return new Timer(1);
    }

    public void shutdownNow() {
        try {
            runFlag = false;
            boolean flag = pool.awaitTermination(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void waitShutdown() throws InterruptedException {
        try {
            lock.lock();
            pool.shutdown();
            condition.await();
            shutdownNow();
        } finally {
            lock.unlock();
        }
    }

    volatile boolean runFlag = true;
    private void start() {
        System.out.println("start the service");
        pool.submit(() -> {
           while(runFlag && !Thread.interrupted()) {
               try {
                   if (delayTasks.size() == 0 && pool.isShutdown()) {
                       break;
                   }
                   DelayedTask<?> task = delayTasks.poll(10, TimeUnit.MILLISECONDS);
                   if(task != null) {
                       if( threadNum == 1) {
                           task.invoke();
                       } else {
                           pool.submit(()->{
                               task.invoke();
                           });
                       }
                   }
               } catch (InterruptedException e) {
                   runFlag = false;
                   e.printStackTrace();
               }
           }
           // notify other thread to continue
            try {
                lock.lock();
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        });
    }

    private Timer(int threadNum) {
        pool = Executors.newFixedThreadPool(threadNum);
        delayTasks = new DelayQueue<>();
        this.threadNum = threadNum;
        start();
    }

    public <T> long setTimeout(Consumer<T> func, T arg, int milliseconds) {
        try {
            DelayedTask<?> task = new DelayedTask<>(func, arg, milliseconds);
            task.delayedConsumer.andThen((val)-> timingMap.remove(task));
            while(timingMap.containsKey(along.incrementAndGet())) {
            }
            timingMap.put(along.get(), task);
            delayTasks.offer(task);
            return along.get();
        } finally {
//            System.out.println(delayTasks.size());
        }
    }

    public <T> long setInterval(Consumer<T> func, T arg, int milliseconds) {
        while(intervalMap.containsKey(intervalId.incrementAndGet())) {
        }
        intervalMap.put(intervalId.get(), true);
        callIntervalTask(intervalId.get(), func, arg, milliseconds);
        return intervalId.get();
    }

    public boolean clearTimeout(long id) {
        if(!timingMap.containsKey(id)) return false;
        DelayedTask<?> task = timingMap.get(id);
        task.cancel();
        return true;
    }

    public boolean clearInterval(long id) {
        intervalMap.put(id, false);
        return false;
    }

    private <T> void callIntervalTask(long id, Consumer<T> func, T arg, int milliseconds) {
        if(intervalMap.get(id) == false) {
            return;
        }
        setTimeout( (args)-> {
            func.accept(arg);
            callIntervalTask(id, func, arg, milliseconds);
        }, null, milliseconds);
    }

    public <T> Promise<T> asycTimeout(T val, int ms) {
        return  new Promise<T>((resolv, reject)->{
            setTimeout((v)->{
                resolv.accept(v);
            }, val, ms);
        });
    }
    public <T> Promise<T> asycInterval(T val, int ms) {
        return new Promise<>((resolv, reject)->{
            // todo timer task
        });
    }

    public static void main(String[] args) {
        Timer timer = singleThreadTimer();
        System.out.println(1);
        new Promise<Integer>((res, reject)-> {
            timer.setTimeout((val)-> {
                System.out.println(val);
                res.accept(100);
            }, 10, 1000);
        }).then((AsycTask<Integer, Integer>) (val) -> {
                    return new Promise<Integer>((res, reject) -> {
                        timer.setTimeout((ival) -> {
                            System.out.println(ival);
                            res.accept(101);
                        }, val, 1000);
                    });
                }
        ).then((ival)->{
            System.out.println(ival);
            long iid = timer.setInterval((val)->{
                System.out.println(val+1);
            }, 10, 1000);
            timer.setTimeout((val)-> {
                timer.clearInterval(iid);
            }, 10, 2900);
        });
        long id = timer.setTimeout((val)-> {
            System.out.println(100000);
        }, 1000, 10000);
        timer.clearTimeout(id);
        //timer.shutdownNow();
//        timer.<Integer>asycTimeout(10, 3000)
//                .then((val)->{
//                    System.out.println(val);
//                    return timer.asycTimeout(100,3000);
//                })
//                .then((val)->{
//                    System.out.println(val);
//                    return null;
//                });
    }

}
