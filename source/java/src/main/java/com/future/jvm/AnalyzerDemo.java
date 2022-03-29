package com.future.jvm;

import java.io.IOException;

/**
 * 性能工具分析演示 demo
 */
@SuppressWarnings("unused")
class AnalyzerDemo {

    static class Holder {
        final byte[] data = new byte[1 << 14];
    }

    static class MemoryDistribution {
        static Holder holder = new Holder();
        Holder instance = new Holder();

        void foo() throws IOException {
            Holder local = new Holder();
            // 此处断点，查看每个对象的地址及内存分布。
            System.out.println("done");
            System.in.read();
        }
    }

    /**
     * 禁用压缩指针。以便用 JHSDB 查看。（据《深入理解Java虚拟机》中，JHSDB 对压缩指针有缺陷）
     * -XX:-UseCompressedOops
     * -XX:+UseSerialGC 为了方便
     */
    public static void main(String[] args) throws IOException {
        MemoryDistribution mb = new MemoryDistribution();
        mb.foo();
    }
}
