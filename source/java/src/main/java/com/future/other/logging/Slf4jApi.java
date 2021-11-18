package com.future.other.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jApi {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public void run() {
        LOGGER.error("error");
        LOGGER.warn("warn");
        LOGGER.info("info");
        LOGGER.debug("debug");
        LOGGER.trace("trace");

        String name = "future";
        Integer age = 18;
        LOGGER.info("用户:{},{}", name, age);
    }

    public static void main(String[] args) {
        new Slf4jApi().run();
    }

}
