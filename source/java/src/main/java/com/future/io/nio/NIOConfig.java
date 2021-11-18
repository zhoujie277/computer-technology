package com.future.io.nio;

import com.future.util.ConfigProperties;

public class NIOConfig {
    private static class NIOProperties extends ConfigProperties {

        public NIOProperties() {
            super("nio.properties");
            loadFromFile();
        }
    }

    private static NIOProperties sProperties = new NIOProperties();

    public static final String KEY_CODEC_TYPE = "codec.type";

    public static String get(String key) {
        return sProperties.getProperty(key);
    }

    public static int getInt(String key) {
        return Integer.parseInt(sProperties.getProperty(key));
    }

    public static String getFileSrcPath() {
        return sProperties.getProperty("file.src.path");
    }

    public static String getFileDstPath() {
        return sProperties.getProperty("file.dest.path");
    }

    public static String getResourceSrcPath() {
        return sProperties.getProperty("file.resource.src.path");
    }

    public static String getResourceDestPath() {
        return sProperties.getProperty("file.resource.dest.path");
    }

    public static String getSocketSendFile() {
        return sProperties.getProperty("client.file");
    }

    public static String getSocketReceiveFile() {
        return sProperties.getProperty("socket.receive.file");
    }

    public static String getServerReceivePath() {
        return sProperties.getProperty("server.receive.path");
    }

    public static int getBufferSize() {
        return sProperties.getIntProperty("buffer.size");
    }

    public static int getBufferSizeLarge() {
        return sProperties.getIntProperty("buffer.size.large");
    }

    public static int getBufferSizeSmall() {
        return sProperties.getIntProperty("buffer.size.small");
    }

    public static String getServerIP() {
        return sProperties.getProperty("server.ip");
    }

    public static int getServerPort() {
        return sProperties.getIntProperty("server.port");
    }

    public static int getServerUdpPort() {
        return sProperties.getIntProperty("server.udp.port");
    }
}
