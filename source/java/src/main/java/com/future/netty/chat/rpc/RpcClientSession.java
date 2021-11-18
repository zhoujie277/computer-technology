package com.future.netty.chat.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.future.io.nio.NIOConfig;
import com.future.netty.chat.message.RpcRequestMessage;
import com.future.netty.chat.protocol.MessageCodecSharable;
import com.future.netty.chat.protocol.ProtocolFrameDecoder;
import com.future.netty.chat.protocol.SequenceIdGenerator;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcClientSession {

    private static final Map<Integer, Promise<Object>> sInvokePromises = new ConcurrentHashMap<>();

    public static Promise<Object> getPromise(int sequenceId) {
        return sInvokePromises.get(sequenceId);
    }

    private Channel channel;

    public void run() {
        final NioEventLoopGroup worker = new NioEventLoopGroup();
        LoggingHandler loggingHandler = new LoggingHandler();
        MessageCodecSharable codecSharable = new MessageCodecSharable();
        RpcMessageResponseHandler handler = new RpcMessageResponseHandler();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(worker);
            bootstrap.channel(NioSocketChannel.class).handler(new ChannelInitializer<Channel>() {
                protected void initChannel(Channel ch) throws Exception {
                    ch.pipeline().addLast(new ProtocolFrameDecoder()).addLast(loggingHandler).addLast(codecSharable)
                            .addLast(handler);
                };
            });
            this.channel = bootstrap.connect(NIOConfig.getServerIP(), NIOConfig.getServerPort()).sync().channel();

            this.channel.closeFuture().addListener(new GenericFutureListener<Future<? super Void>>() {

                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    worker.shutdownGracefully();
                }
            });

        } catch (Exception exception) {
            log.error("client error", exception);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> clazz) {
        final int nextId = SequenceIdGenerator.nextId();
        Class<?>[] interfaces = { clazz };
        ClassLoader loader = getClass().getClassLoader();
        Object proxy = Proxy.newProxyInstance(loader, interfaces, new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                RpcRequestMessage message = new RpcRequestMessage(nextId, clazz.getName(), method.getName(),
                        method.getReturnType(), method.getParameterTypes(), args);
                ChannelFuture writeFuture = channel.writeAndFlush(message);
                writeFuture.addListener(new GenericFutureListener<Future<? super Object>>() {

                    @Override
                    public void operationComplete(Future<? super Object> future) throws Exception {
                        log.debug("future.isSuccess() {}", future.isSuccess());
                        if (!future.isSuccess()) {
                            log.error("error", future.cause());
                        }
                    }
                });
                Promise<Object> promise = new DefaultPromise<>(channel.eventLoop());
                sInvokePromises.put(nextId, promise);
                return promise.get();
            }
        });
        return (T) proxy;
    }

}
