package com.future.util;

import java.io.*;

@SuppressWarnings("unused")
public class FileUtils {

    public static byte[] readFile(String srcPath, byte[] bytes) {
        File file = new File(srcPath);
        try (FileInputStream in = new FileInputStream(file)) {
            int n = in.read(bytes);
            return bytes;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    public static void writeFile(String dstPath, byte[] bytes) {
        File file = new File(dstPath);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }
}
