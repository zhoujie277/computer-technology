package com.future.concurrent;

import java.util.concurrent.TimeUnit;

/**
 * 演示 JVM 关闭之后 shutdown hook 的回调
 * IDE 中停止按钮是会执行 shutdown hook 的。
 * 但是其在 kill -9 是不会执行该 hook 的。
 * kill -15 正常终止，可以执行该 hook
 * <p>
 * HUP     1    终端断线
 * INT       2    中断（同 Ctrl + C）
 * QUIT    3    退出（同 Ctrl + \）
 * TERM    15    终止
 * KILL      9    强制终止
 * CONT   18    继续（与STOP相反， fg/bg命令）
 * STOP    19    暂停（同 Ctrl + Z）
 */
@SuppressWarnings("all")
class ShutdownHookAPI {

    void start() throws InterruptedException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("shutdown jvm");
        }));
        while (true) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ShutdownHookAPI api = new ShutdownHookAPI();
        api.start();
    }
}
