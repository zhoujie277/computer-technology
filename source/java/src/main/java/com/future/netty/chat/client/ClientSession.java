package com.future.netty.chat.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.future.netty.chat.common.bean.ChatUser;
import com.future.netty.chat.proto.ProtoMsg;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ClientSession {
    public static final String SESSION_KEY = "SESSION_KEY";

    private static ClientSession session = new ClientSession();

    private NettyClient nettyClient = new NettyClient(new SessionNetworkListener());
    private ChatUser user;

    /**
     * 保存登录后的服务端sessionid
     */
    private String sessionId;
    private boolean isLogin = false;
    private boolean isConnected = false;
    private AtomicLong sequenceId = new AtomicLong();
    /**
     * session中存储的session 变量属性值
     */
    private Map<String, Object> map = new HashMap<String, Object>();

    public static ClientSession getSession() {
        return session;
    }

    public static ChatUser getUser() {
        return session.user;
    }

    public static void put(String key, Object value) {
        session.map.put(key, value);
    }

    public void connectServer() {
        nettyClient.connectSync();
    }

    // 绑定通道
    public void connectSuccess() {
        // 连接成功，还没登录，暂时没有会话id
        this.sessionId = String.valueOf(0);
        this.isConnected = true;
        this.sequenceId.set(System.currentTimeMillis());
        nettyClient.addChannelAttr(SESSION_KEY, this);
    }

    // 登录成功之后,设置sessionId
    public void loginSuccess(ProtoMsg.Message pkg) {
        session.setSessionId(pkg.getSessionId());
        session.setLogin(true);
        log.info("登录成功");
    }

    public String getRemoteAddress() {
        return nettyClient.getRemoteAddress();
    }

    /**
     * 构建消息 基础部分
     */
    public ProtoMsg.Message.Builder buildCommon() {
        long newSequeneceId = sequenceId.incrementAndGet();
        ProtoMsg.Message.Builder mb = ProtoMsg.Message.newBuilder().setSessionId(sessionId).setSequence(newSequeneceId);
        return mb;
    }

    public void writeMessage(ProtoMsg.Message message) {
        nettyClient.writeAndFlush(message);
    }

    // 主动退出客户端
    public void close() {
        isConnected = false;
        nettyClient.close();
    }

    private class SessionNetworkListener implements NetworkListener {

        @Override
        public void onConnectSuccess() {
            connectSuccess();
        }

        @Override
        public void onConenctFailed() {
            System.out.println("连接失败");
        }
    }

}
