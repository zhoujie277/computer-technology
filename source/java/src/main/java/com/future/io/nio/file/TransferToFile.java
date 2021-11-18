package com.future.io.nio.file;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import com.future.util.IOUtil;

/**
 * transferTo 技术 底层用了零拷贝技术
 */
@SuppressWarnings("all")
public class TransferToFile implements Runnable {

    @Override
    public void run() {
        FileChannel from = null;
        FileChannel to = null;
        try {
            from = new FileInputStream(IOUtil.getClassLoaderResourcePath("nio.properties")).getChannel();
            to = new FileOutputStream(IOUtil.getClassLoaderResourcePath("tmp.txt")).getChannel();
            // 底层会利用操作系统的零拷贝技术. 
            // transferto 一次传输大小限制：2G，可多次传输
            from.transferTo(0, from.size(), to);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtil.close(from);
            IOUtil.close(to);
        }
    }

    public static void main(String[] args) {
        new TransferToFile().run();
    }
}
