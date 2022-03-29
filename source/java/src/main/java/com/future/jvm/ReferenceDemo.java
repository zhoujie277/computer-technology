package com.future.jvm;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * -Xmx16m
 * -XX:+PrintGCDetails -verbose:gc
 */
@Slf4j
@SuppressWarnings("all")
class ReferenceDemo {

    private static final int TwoM = 1 << 21;

    /**
     * will throw Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
     */
    private static void strong() throws IOException {
        List<byte[]> list = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            byte[] buf = new byte[TwoM];
            list.add(buf);
            log.debug("size={}, data={}", list.size(), String.valueOf(buf));
        }
        System.in.read();
    }

    private static void soft() throws IOException {
        List<SoftReference<byte[]>> list = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            byte[] buf = new byte[TwoM];
            SoftReference<byte[]> reference = new SoftReference<>(buf);
            list.add(reference);
            log.debug("size={}, data={}", list.size(), String.valueOf(reference.get()));
        }
        log.debug("#### after allocate ####");
        for (SoftReference<byte[]> reference : list) {
            log.debug("reference={}", String.valueOf(reference.get()));
        }
    }

    private static void weak() throws IOException {
        List<WeakReference<byte[]>> list = new ArrayList<>();
        ReferenceQueue queue = new ReferenceQueue();
        for (int i = 0; i < 6; i++) {
            byte[] buf = new byte[TwoM];
            WeakReference<byte[]> reference = new WeakReference<>(buf, queue);
            list.add(reference);
            printList(list);
        }
        log.debug("#### after allocate ####");
        printList(list);

        Reference poll;
        while ((poll = queue.poll()) != null) {
            list.remove(poll);
        }
        log.debug("#### after release ####");
        printList(list);
    }

    private static void printList(List<? extends Reference<byte[]>> list) {
        for (Reference<byte[]> reference : list) {
            System.out.print(String.valueOf(reference.get()) + "\t");
        }
        System.out.println();
    }


    public static void main(String[] args) throws IOException {
//        soft();
        weak();
    }
}
