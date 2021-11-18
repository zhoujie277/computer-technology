package com.future.other.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogbackApi {

    public final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public void run() {
        LOGGER.error("error");
        LOGGER.warn("warn");
        LOGGER.info("info");
        LOGGER.debug("debug");
        LOGGER.trace("trace");
    }

    public static void main(String[] args) {
        new LogbackApi().run();
    }

}
