package com.future.jvm.bytecode;

/**
 * 方法签名的字节码分析器
 *
 * @author future
 */
@SuppressWarnings("all")
class MethodSignBytecode {

    /*
     * run()V
     */
    void run() {

    }

    /*
     * run(Ljava/lang/String;)Ljava/lang/String;
     */
    public String run(String s) {
        return s;
    }

    /*
     * run(I)I
     */
    public int run(int i) {
        return i;
    }

    /*
     * run(J)J
     */
    public static long run(long i) {
        return i;
    }

    /*
     * run(Ljava/lang/Integer;)[I
     */
    public int[] run(Integer i) {
        return null;
    }
}
