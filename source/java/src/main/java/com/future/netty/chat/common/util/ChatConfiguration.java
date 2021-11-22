package com.future.netty.chat.common.util;

import com.future.netty.chat.common.message.Message;
import com.future.util.ConfigProperties;

/**
 * IM 公共配置
 * 
 * @author future
 */
public class ChatConfiguration {

    /**
     * maxFrameLength=1024 lengthFieldOffset=12 lengthFieldLength=4
     * lengthAdjustment=0 initialBytesToStrip=0 codec=proto server.ip=192.168.31.236
     * server.port=9100
     */

    private static class Properties extends ConfigProperties {

        public Properties() {
            super("chat.properties");
            loadFromFile();
        }
    }

    private ChatConfiguration() {
    }

    private static Properties sProperties = new Properties();

    public static String getProperty(String key) {
        return sProperties.getProperty(key);
    }

    public static int getIntProperty(String key) {
        return sProperties.getIntProperty(key);
    }

    public static Message.Codec getCodecType() {
        String string = getProperty("codec");
        return Message.Codec.valueOf(string);
    }

    public static int getPort() {
        return getIntProperty("server.port");
    }

    public static String getServerHost() {
        return getProperty("server.ip");
    }
}
