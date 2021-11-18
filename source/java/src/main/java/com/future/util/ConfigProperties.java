package com.future.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class ConfigProperties {

    private String properiesName = "";
    private Properties properties = new Properties();

    public ConfigProperties(String fileName) {
        this.properiesName = fileName;
    }

    protected void loadFromFile() {
        InputStream in = null;
        InputStreamReader ireader = null;
        try {
            String filePath = IOUtil.getClassLoaderResourcePath(properiesName);
            in = new FileInputStream(filePath);
            // 解决读非UTF-8编码的配置文件时，出现的中文乱码问题
            ireader = new InputStreamReader(in, "UTF-8");
            properties.load(ireader);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtil.close(ireader);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public int getIntProperty(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }
}
