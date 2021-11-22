package com.future.netty.chat.client;

import java.util.concurrent.TimeUnit;

import com.future.netty.chat.client.handler.ClientActionDispatcher;
import com.future.netty.chat.client.handler.ExceptionHandler;
import com.future.netty.chat.client.handler.HeartBeatHandler;
import com.future.netty.chat.common.codec.CodecFrameDecoder;
import com.future.netty.chat.common.codec.MessageCodec;
import com.future.netty.chat.common.util.ChatConfiguration;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

/**
 * 管理网络连接和关闭。及相关网络 I/O 事件的处理
 * 
 * @author future
 */
@Slf4j
public class NettyClient {

    private EventLoopGroup mWorkder;
    private Channel mChannel;
    private NetworkListener mNetworkListener;
    private int retryCount = 3;
    private boolean isConnected = false;

    public NettyClient(NetworkListener listener) {
        this.mWorkder = new NioEventLoopGroup();
        this.mNetworkListener = listener;
    }

    public String getRemoteAddress() {
        if (mChannel == null)
            return "你是否忘记了连接服务器";
        return mChannel.remoteAddress().toString();
    }

    public void connectSync() {
        try {
            ChannelFuture future = doConnect().sync();
            onConnectComplete(future);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    private ChannelFuture doConnect() {
        isConnected = false;
        int maxFrameLength = ChatConfiguration.getIntProperty("maxFrameLength");
        int lengthFieldOffset = ChatConfiguration.getIntProperty("lengthFieldOffset");
        int lengthFieldLength = ChatConfiguration.getIntProperty("lengthFieldLength");
        int lengthAdjustment = ChatConfiguration.getIntProperty("lengthAdjustment");
        int initialBytesToStrip = ChatConfiguration.getIntProperty("initialBytesToStrip");
        int pingInterval = ChatConfiguration.getIntProperty("ping.interval");

        final LoggingHandler loggingHandler = new LoggingHandler();
        CodecFrameDecoder mFrameDecoder = new CodecFrameDecoder(maxFrameLength, lengthFieldOffset, lengthFieldLength,
                lengthAdjustment, initialBytesToStrip);
        MessageCodec mCodec = new MessageCodec();
        ClientActionDispatcher mDispatcher = new ClientActionDispatcher();
        ExceptionHandler mExceptionHandler = new ExceptionHandler();
        Bootstrap b = new Bootstrap();
        b.group(mWorkder);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        b.remoteAddress(ChatConfiguration.getServerHost(), ChatConfiguration.getPort());
        b.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new IdleStateHandler(0, pingInterval, 0)).addLast(new HeartBeatHandler());
                ch.pipeline()
                        .addLast(new CodecFrameDecoder(maxFrameLength, lengthFieldOffset, lengthFieldLength,
                                lengthAdjustment, initialBytesToStrip))
                        .addLast(mFrameDecoder).addLast(mCodec).addLast(loggingHandler).addLast(mDispatcher)
                        .addLast(mExceptionHandler);
            }
        });
        return b.connect();
    }

    public <T> void addChannelAttr(String key, T value) {
        AttributeKey<T> attrKey = AttributeKey.valueOf(key);
        mChannel.attr(attrKey).set(value);
    }

    public <T> T getChannelAttr(String key) {
        AttributeKey<T> attrKey = AttributeKey.valueOf(key);
        return mChannel.attr(attrKey).get();
    }

    public ChannelFuture writeAndFlush(Object pkg) {
        ChannelFuture future = mChannel.writeAndFlush(pkg);
        future.addListener(new WrittenListener());
        return future;
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public void close() {
        mChannel.closeFuture().addListener(new ClosedListener());
        mChannel.close();
    }

    private void faildConnect() {
        if (mNetworkListener != null) {
            mNetworkListener.onConenctFailed();
        }
        close();
    }

    private void onConnectComplete(ChannelFuture future) {
        if (!future.isSuccess()) {
            log.info("连接失败!在10s之后准备尝试重连!");
            if (retryCount > 0)
                mWorkder.schedule(this::doConnect, 10, TimeUnit.SECONDS);
            else
                faildConnect();
        } else {
            log.info("To Connected Server is Successful!");
            isConnected = true;
            Channel channel = future.channel();
            mChannel = channel;
            if (mNetworkListener != null) {
                mNetworkListener.onConnectSuccess();
            }
        }
    }

    private class ClosedListener implements GenericFutureListener<ChannelFuture> {

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            log.info("a connected was broken..");
            mWorkder.shutdownGracefully();
        }
    }

    private class WrittenListener implements GenericFutureListener<ChannelFuture> {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (!future.isSuccess()) {
                // log or resend strategy
                log.error("failed", future.cause());
            }
        }
    }
}
