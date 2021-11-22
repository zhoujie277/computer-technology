package com.future.netty.chat.common.message;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.future.util.ConfigProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString(of = { "sequence", "sessionId" })
public abstract class Message implements Serializable {
    /**
     * 魔数，可以通过配置获取
     */
    public static final short MAGIC_CODE = 0x86;

    /**
     * 版本号
     */
    public static final short VERSION_CODE = 0x01;

    /**
     * 消息编码方式
     */
    public enum Codec {
        JAVA, GSON, PROTO_BUF
    }

    /**
     * 消息类型
     */
    public enum MsgType {
        PING(0), PONG(1), LOGIN_REQ(2), LOGOUT_REQ(3), CHAT_REQ(4), GROUP_CREATE_REQ(5), GROUP_CHAT_REQ(6),
        GROUP_JOIN_REQ(7), GROUP_QUIT_REQ(8), LOGIN_ACK(1002), LOGOUT_ACK(1003), CHAT_ACK(1004), GROUP_CREATE_ACK(1005),
        GROUP_CHAT_ACK(1006), GROUP_JOIN_ACK(1007), GROUP_QUIT_ACK(1008), MESSAGE_NOTIFICATION(100), RPC_REQ(300),
        RPC_ACK(301);

        private int value;

        private MsgType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    // 消息类型 1：纯文本 2：音频 3：视频 4：地理位置 5：其他
    public enum ContentType {
        TEXT, AUDIO, VIDEO, POS, OTHER;
    }

    protected long sequence;
    protected String sessionId;

    public abstract MsgType getMessageType();

    private static MessageConfiguration sConfiguration = new MessageConfiguration();

    @SuppressWarnings("unchecked")
    public static Class<? extends Message> getMessageClass(Message.MsgType msgType) {
        return (Class<? extends Message>) sConfiguration.getClass(msgType);
    }

    private static class MessageConfiguration extends ConfigProperties {
        Map<Message.MsgType, Class<?>> mClassMap = new EnumMap<>(Message.MsgType.class);

        public MessageConfiguration() {
            super("chat_message.properties");
            loadFromFile();
            loadClasses();
        }

        public void loadClasses() {
            String pkgName = mProperties.getProperty("package");
            Set<Object> keySet = mProperties.keySet();
            for (Object obj : keySet) {
                String key = obj.toString();
                String className = mProperties.getProperty(key);
                if (key.equals("package"))
                    continue;
                try {
                    Class<?> clazz = Class.forName(pkgName + "." + className);
                    mClassMap.put(Message.MsgType.valueOf(key), clazz);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        public Class<?> getClass(Message.MsgType msgType) {
            return mClassMap.get(msgType);
        }
    }

}
