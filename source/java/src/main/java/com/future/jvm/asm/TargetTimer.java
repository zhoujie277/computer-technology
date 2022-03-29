package com.future.jvm.asm;

@SuppressWarnings("all")
public class TargetTimer {

    public void m() throws Exception {
        Thread.sleep(100);
    }

    /*
     * will into this:
     *     public class TargetTimer {
     *         public static long timer;
     *         public void m() throws Exception {
     *             timer -= System.currentTimeMillis();
     *             Thread.sleep(100);
     *             timer += System.currentTimeMillis();
     *         }
     *     }
     *
     * or
     *     public class TargetTimer {
     *         public static long timer;
     *         public void m() throws Exception {
     *             long t = System.currentTimeMillis();
     *             Thread.sleep(100);
     *             timer += System.currentTimeMillis() - t;
     *         }
     *     }
     */
}
