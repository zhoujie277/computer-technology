package com.future.io.nio.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import com.future.io.nio.NIOConfig;
import com.future.util.IOUtil;

/**
 * FileChannel 只能工作在阻塞模式下。
 * 基于 NIO 的文件拷贝实验
 */
public class FileNIOCopyDemo {

    private static final int BUFSIZE = 1024 * 20;

    public void copyFile(String src, String dst) {
        File srcFile = new File(src);
        File dstFile = new File(dst);
        if (!srcFile.exists()) {
            System.out.println("file not exists:" + src);
            return;
        }
        ;
        if (!dstFile.exists()) {
            try {
                dstFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        long startTime = System.currentTimeMillis();
        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        try {
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(dstFile);
            inChannel = fis.getChannel();
            outChannel = fos.getChannel();
            int length = -1;
            ByteBuffer buffer = ByteBuffer.allocate(BUFSIZE);
            while ((length = inChannel.read(buffer)) != -1) {
                System.out.println("从通道里读入的字节数为：" + length);
                // 转换写模式
                buffer.flip();
                // 将缓冲区的数据读出来，往通道里写。
                int outLength = outChannel.write(buffer);
                System.out.println("往通道里写出的字节数为：" + outLength);
                // 清空缓冲区，转换为写模式
                buffer.clear();
            }
            long endTime = System.currentTimeMillis();
            System.out.println("copyFile cost time " + (endTime - startTime) + " ms.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtil.close(outChannel);
            IOUtil.close(fos);
            IOUtil.close(inChannel);
            IOUtil.close(fis);
        }
    }

    public void start() {
        String srcPath = NIOConfig.getFileSrcPath();
        String dstPath = NIOConfig.getFileDstPath();
        System.out.println("srcPath = " + srcPath);
        System.out.println("dstPath = " + dstPath);
        copyFile(srcPath, dstPath);
    }

    public static void main(String[] args) {
        FileNIOCopyDemo demo = new FileNIOCopyDemo();
        demo.start();
    }

}