package com.future.netty.chat.client.handler;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.future.netty.chat.client.Cookie;
import com.future.netty.chat.client.action.ClientAction;
import com.future.netty.chat.common.message.Message;
import com.future.util.ConfigProperties;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class ClientActionDispatcher extends ChannelInboundHandlerAdapter {

    private static ActionConfiguration sConfiguration = new ActionConfiguration();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object pkg) throws Exception {
        Message msg = (Message) pkg;
        // 判断类型
        ClientAction action = sConfiguration.getAction(msg.getMessageType());
        if (action == null) {
            super.channelRead(ctx, msg);
            return;
        }
        action.execute(Cookie.getCookie(), msg);
    }

    private static class ActionConfiguration extends ConfigProperties {
        private Map<Message.MsgType, ClientAction> mActionMap = new EnumMap<>(Message.MsgType.class);

        public ActionConfiguration() {
            super("chat_client.properties");
            loadFromFile();
            loadActions();
        }

        public void loadActions() {
            String pkg = mProperties.getProperty("package");
            Set<Object> keySet = mProperties.keySet();
            for (Object obj : keySet) {
                String key = obj.toString();
                String className = pkg + "." + mProperties.getProperty(key);
                if (key.equals("package") || !className.endsWith("Action"))
                    continue;
                Class<?> forName;
                try {
                    forName = Class.forName(className);
                    mActionMap.put(Message.MsgType.valueOf(key), (ClientAction) forName.newInstance());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        public ClientAction getAction(Message.MsgType msgType) {
            return mActionMap.get(msgType);
        }

    }
}
