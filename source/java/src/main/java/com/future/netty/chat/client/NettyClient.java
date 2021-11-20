package com.future.netty.chat.client;

import java.util.concurrent.TimeUnit;

import com.future.io.nio.NIOConfig;
import com.future.netty.chat.client.handler.DispatchReponseHandler;
import com.future.netty.chat.client.handler.ExceptionHandler;
import com.future.netty.chat.client.handler.HeartBeatHandler;
import com.future.netty.chat.common.codec.SimpleProtobufDecoder;
import com.future.netty.chat.common.codec.SimpleProtobufEncoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
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
            e.printStackTrace();
        }
    }

    private ChannelFuture doConnect() {
        Bootstrap b = new Bootstrap();
        b.group(mWorkder);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        b.remoteAddress(NIOConfig.getServerIP(), NIOConfig.getServerPort());
        b.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast("decoder", new SimpleProtobufDecoder())
                        .addLast("encoder", new SimpleProtobufEncoder()).addLast(new DispatchReponseHandler())
                        .addLast(new ExceptionHandler());
            }
        });
        ChannelFuture future = b.connect();
        return future;
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
                mWorkder.schedule(() -> doConnect(), 10, TimeUnit.SECONDS);
            else
                faildConnect();
        } else {
            log.info("To Connected Server is Successful!");
            Channel channel = future.channel();
            mChannel = channel;
            if (mNetworkListener != null) {
                mNetworkListener.onConnectSuccess();
            }
            ChannelPipeline p = channel.pipeline();
            // 在编码器后面，动态插入心跳处理器
            p.addLast(new IdleStateHandler(0, 3, 0));
            p.addLast(new HeartBeatHandler());
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
            if (future.isSuccess()) {
                log.debug("write success");
            } else {
                // log or resend strategy
                log.error("failed", future.cause());
            }
        }
    }
}
