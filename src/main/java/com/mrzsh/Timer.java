package com.mrzsh;

import java.util.List;
import java.util.concurrent.*;
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

    int threadNum;

    static class DelayedTask<T> implements Delayed {
        private Consumer<T> delayedConsumer;

        private T arg;

        long targetTime;

        public DelayedTask(Consumer<T> consumer, T arg, int ms) {
            delayedConsumer = consumer;
            this.arg = arg;
            targetTime = System.currentTimeMillis() + ms;
        }

        public void invoke() {
//            System.out.println("invoke");
            delayedConsumer.accept(arg);
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

    public void shutdownGraceful() {
        try {
            runFlag = false;
            boolean flag = pool.awaitTermination(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    boolean runFlag = true;
    public void start() {
        System.out.println("start the service");
        pool.submit(() -> {
           while(runFlag && !Thread.interrupted()) {
               try {
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
        });
    }

    private Timer(int threadNum) {
        pool = Executors.newFixedThreadPool(threadNum);
        delayTasks = new DelayQueue<>();
        this.threadNum = threadNum;
        start();
    }

    public <T> int setTimeout(Consumer<T> func, T arg, int milliseconds) {
        try {
            return delayTasks.offer(new DelayedTask<T>(func, arg, milliseconds)) ? 1 : 0;
        } finally {
//            System.out.println(delayTasks.size());
        }
    }

    public static void main(String[] args) {
        Timer timer = singleThreadTimer();
        System.out.println(1);
        new Promise<Integer>((res, reject)-> {
            timer.setTimeout((val)-> {
                System.out.println(val);
                res.accept(100);
            }, 10, 1000);
        }).then((val) -> {
            return new Promise<Integer>((res, reject)-> {
                timer.setTimeout((ival) -> {
                    System.out.println(ival);
                }, val, 1000);
            });
        });
        //timer.shutdownGraceful();
    }

}
