package com.future.other.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class JULApi {

    private void log(Logger logger) {
        logger.severe("severe");
        logger.warning("warning");
        logger.info("info");
        logger.config("config");
        logger.fine("fine");
        logger.finer("finer");
        logger.finest("finest");
    }

    public void run() throws SecurityException, IOException {
        Logger logger = Logger.getLogger("FutureLogName");
        // 基本使用
        logger.info("hello world");
        logger.log(Level.INFO, "info msg");

        // 占位符使用
        String name = "future";
        Integer age = 13;
        logger.log(Level.INFO, "用户信息：{0}, {1}", new Object[] { name, age });

        // 关闭系统默认配置
        logger.setUseParentHandlers(false);

        // 自定义配置日志级别
        ConsoleHandler consoleHandler = new ConsoleHandler();
        Formatter simpleFormatter = new SimpleFormatter();

        consoleHandler.setFormatter(simpleFormatter);
        logger.addHandler(consoleHandler);

        // 配置文件日志
        FileHandler fileHandler = new FileHandler("jul.log");
        fileHandler.setFormatter(simpleFormatter);
        logger.addHandler(fileHandler);

        // 配置日志具体信息
        logger.setLevel(Level.ALL);
        consoleHandler.setLevel(Level.ALL);
        log(logger);
    }

    public void runRelation() {
        Logger logger1 = Logger.getLogger("com.future");
        Logger logger2 = Logger.getLogger("com");
        System.out.println(logger1.getParent() == logger2);
        System.out.println("logger2 parent:" + logger2.getParent() + ",name=" + logger2.getParent().getName());
    }

    public void loadConfiguration() throws SecurityException, IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("logging.properties");
        LogManager logManager = LogManager.getLogManager();
        logManager.readConfiguration(stream);
        Logger logger = Logger.getLogger(getClass().getName());
        log(logger);
    }

    public static void main(String[] args) {
        JULApi api = new JULApi();
        try {
            api.loadConfiguration();
            // api.run();
            // api.runRelation();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
