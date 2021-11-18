package com.future.io.oio;

import com.future.util.ConfigProperties;

public class OIOConfig {
    private static class OIOProperties extends ConfigProperties {

        public OIOProperties() {
            super("oio.properties");
            loadFromFile();
        }
    }

    private static OIOProperties sProperties = new OIOProperties();

    public static int getBufferSize() {
        return sProperties.getIntProperty("buffer.size");
    }

    public static String getServerIP() {
        return sProperties.getProperty("server.ip");
    }

    public static int getServerPort() {
        return sProperties.getIntProperty("server.port");
    }
}
