package com.future.concurrent.api;

import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 定时调度执行器 API 演示
 *
 * @author future
 */
@Slf4j
@SuppressWarnings("unused")
public class ScheduledExecutorApi {

    /**
     * Timer 简单易用
     * 由于所有任务都是由同一个线程来调度，因此所有任务都是串行执行的.
     * 同一时间只能有一个任务在执行.
     * 前一个任务发生异常，会直接结束整个线程。
     * 前一个任务的延迟或异常都将会影响到之后的任务。
     */
    static class TimerDemo {
        Timer timer = new Timer();

        void run() {
            TimerTask task1 = new TimerTask() {
                @Override
                public void run() {
                    log.debug("timer1 run...");
                    try {
                        TimeUnit.SECONDS.sleep(2);
                        throw new NullPointerException();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };

            TimerTask task2 = new TimerTask() {
                @Override
                public void run() {
                    log.debug("timer2 run...");
                }
            };

            timer.schedule(task1, 1000);
            timer.schedule(task2, 1000);
        }
    }

    /**
     * Scheduled 可控制线程执行数量。
     * 更重要的是前一个任务内部若发生了异常，不会影响到第二个任务的执行。
     */
    static class ScheduledThreadDemo {
        ScheduledExecutorService service = Executors.newScheduledThreadPool(2);

        void run() {
            service.schedule(()->{
                log.debug("timer1 run...");
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, 1, TimeUnit.SECONDS);
            service.schedule(() -> log.debug("timer2 run..."), 1, TimeUnit.SECONDS);

            service.scheduleAtFixedRate(()->{
                log.debug("at run...");
                try {
                    // 任务处理时间比间隔时间 1s 要长，则会在任务执行完之后马上运行.
                    // 结论：每隔 2s 执行一次
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, 1, 1, TimeUnit.SECONDS);

            // 任务与任务之间的间隔时间。
            service.scheduleWithFixedDelay(()->{
                log.debug("with run...");
                try {
                    // 无论任务处理完多久，都会再加上一个间隔时间，再运行第二次。
                    // 结论：每隔 3s 执行一次。
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }, 1, 1, TimeUnit.SECONDS);
        }
    }

    static class RegularTask {
        void run() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime thursday = now.with(DayOfWeek.THURSDAY).withHour(0).withMinute(0).withSecond(0).withNano(0);
            // find next thursday
            if (now.compareTo(thursday) > 0) {
                thursday = thursday.plusWeeks(1);
            }
            log.debug("now is {}, thursday is {}", now, thursday);

            long initialDelay = Duration.between(now, thursday).toMillis();
            long period = 7 * 24 * 3600;
            ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);
            scheduledThreadPool.scheduleAtFixedRate(() -> log.debug("task running..."), initialDelay, period, TimeUnit.SECONDS);
        }
    }

    public static void main(String[] args) {
        TimerDemo demo = new TimerDemo();
        demo.run();
        new ScheduledThreadDemo().run();
    }
}
