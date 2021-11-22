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
        PING, PONG, LOGIN_REQ, LOGOUT_REQ, CHAT_REQ, GROUP_CREATE_REQ, GROUP_CHAT_REQ, GROUP_JOIN_REQ, GROUP_QUIT_REQ,
        LOGIN_ACK, LOGOUT_ACK, CHAT_ACK, GROUP_CREATE_ACK, GROUP_CHAT_ACK, GROUP_JOIN_ACK, GROUP_QUIT_ACK,
        MESSAGE_NOTIFICATION, RPC_REQ, RPC_ACK;
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
