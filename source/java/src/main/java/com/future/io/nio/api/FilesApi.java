package com.future.io.nio.api;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

public class FilesApi implements Runnable {

    @Override
    public void run() {
        final AtomicInteger dirCounter = new AtomicInteger();
        final AtomicInteger fileCounter = new AtomicInteger();
        try {
            // 访问者模式
            Files.walkFileTree(Paths.get("/Users/jayzhou/work/owner/code/github"), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    System.out.println("-----" + dir);
                    dirCounter.incrementAndGet();
                    return super.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    System.out.println(file);
                    fileCounter.incrementAndGet();
                    return super.visitFile(file, attrs);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("dircount = " + dirCounter);
        System.out.println("filecount = " + fileCounter);
    }

    public static void main(String[] args) {
        new FilesApi().run();
    }

}
