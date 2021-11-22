package com.future.netty.chat.server.handler;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.future.netty.chat.common.message.Message;
import com.future.netty.chat.common.message.Message.MsgType;
import com.future.netty.chat.server.action.LoginAction;
import com.future.netty.chat.server.action.ServerAction;
import com.future.netty.chat.server.session.Session;
import com.future.netty.chat.server.session.SessionManager;
import com.future.util.ConfigProperties;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

@Sharable
public class ServerActionDispatcher extends ChannelInboundHandlerAdapter {

    private static ActionConfiguration sConfiguration = new ActionConfiguration();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 判断类型
        Message pkg = (Message) msg;
        if (pkg.getMessageType() == MsgType.LOGIN_REQ) {
            // 登录具备状态，没有会话，登录成功建立会话，登录失败，记录失败策略。
            // 需要单独处理。
            // 理想情况：未登录的用户处于 半连接队列。登录成功的则进入全连接队列。
            // 两个队列享受不同的资源策略。
            new LoginAction(ctx.channel()).execute(null, pkg);
        } else {
            ServerAction action = sConfiguration.getAction(pkg.getMessageType());
            if (action == null) {
                super.channelRead(ctx, msg);
                return;
            }
            Session session = SessionManager.getInstance().getSession(ctx.channel());
            action.execute(session, pkg);
        }
    }

    private static class ActionConfiguration extends ConfigProperties {
        private Map<Message.MsgType, ServerAction> mActionMap = new EnumMap<>(Message.MsgType.class);

        public ActionConfiguration() {
            super("chat_server.properties");
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
                    mActionMap.put(Message.MsgType.valueOf(key), (ServerAction) forName.newInstance());
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        public ServerAction getAction(Message.MsgType msgType) {
            return mActionMap.get(msgType);
        }

    }
}
