package com.future.netty.chat.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.future.netty.chat.common.message.Message;
import com.future.netty.chat.common.message.User;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Cookie {
    public static final String SESSION_KEY = "SESSION_KEY";

    private static Cookie cookie = new Cookie();

    public static Cookie getCookie() {
        return cookie;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class InnerSession {
        protected long sequence;
        private String sessionId;
        private User user;
        List<User> onlineList;
    }

    private InnerSession mInnerSession;
    private NettyClient nettyClient = new NettyClient(new SessionNetworkListener());

    // 客户端序列号
    private AtomicLong clientSequence = new AtomicLong(1);

    /**
     * cookie中存储的 变量属性值
     */
    private Map<String, Object> map = new HashMap<>();

    public User getUser() {
        if (mInnerSession == null)
            return null;
        return mInnerSession.getUser();
    }

    public void put(String key, Object value) {
        map.put(key, value);
    }

    public void connectServer() {
        nettyClient.connectSync();
    }

    // 绑定通道
    public void connectSuccess() {
        this.clientSequence.set(System.currentTimeMillis());
        nettyClient.addChannelAttr(SESSION_KEY, this);
    }

    // 登录成功之后,设置sessionId
    public void createSession(long sequence, String sessionId, User user, List<User> onlineList) {
        mInnerSession = new InnerSession(sequence, sessionId, user, onlineList);
    }

    public String getRemoteAddress() {
        return nettyClient.getRemoteAddress();
    }

    public void writeMessage(Message message) {
        message.setSequence(clientSequence.incrementAndGet());
        message.setSessionId(mInnerSession == null ? "-1" : mInnerSession.sessionId);
        nettyClient.writeAndFlush(message);
    }

    // 主动退出客户端
    public void close() {
        nettyClient.close();
    }

    public boolean isConnected() {
        return nettyClient.isConnected();
    }

    private class SessionNetworkListener implements NetworkListener {

        @Override
        public void onConnectSuccess() {
            connectSuccess();
        }

        @Override
        public void onConenctFailed() {
            log.info("连接失败");
        }
    }

}
