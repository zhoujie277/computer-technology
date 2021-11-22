package com.future.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ConfigProperties {

    private String mPropertiesName = "";
    protected Properties mProperties = new Properties();

    public ConfigProperties(String fileName) {
        this.mPropertiesName = fileName;
    }

    protected void loadFromFile() {
        InputStream in = null;
        InputStreamReader ireader = null;
        try {
            String filePath = IOUtil.getClassLoaderResourcePath(mPropertiesName);
            in = new FileInputStream(filePath);
            // 解决读非UTF-8编码的配置文件时，出现的中文乱码问题
            ireader = new InputStreamReader(in, StandardCharsets.UTF_8);
            mProperties.load(ireader);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtil.close(in);
            IOUtil.close(ireader);
        }
    }

    public String getProperty(String key) {
        return mProperties.getProperty(key);
    }

    public int getIntProperty(String key) {
        return Integer.parseInt(mProperties.getProperty(key));
    }
}
