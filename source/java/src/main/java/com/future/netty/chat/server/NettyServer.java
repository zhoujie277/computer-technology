package com.future.netty.chat.server;

import com.future.netty.chat.common.codec.CodecFrameDecoder;
import com.future.netty.chat.common.codec.MessageCodec;
import com.future.netty.chat.common.util.ChatConfiguration;
import com.future.netty.chat.server.handler.QuitHandler;
import com.future.netty.chat.server.handler.ServerActionDispatcher;
import com.future.netty.chat.server.handler.ServerExceptionHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务器 <br/>
 * 此处应要进行资源管理 <br/>
 * 客户端连接数，已登录连接（全连接），未登录连接（半连接） <br/>
 * 客户端读数据超时
 */
@Slf4j
public class NettyServer {

    private EventLoopGroup mBoss = new NioEventLoopGroup();
    private EventLoopGroup mWorker = new NioEventLoopGroup();

    public void run() {
        int maxFrameLength = ChatConfiguration.getIntProperty("maxFrameLength");
        int lengthFieldOffset = ChatConfiguration.getIntProperty("lengthFieldOffset");
        int lengthFieldLength = ChatConfiguration.getIntProperty("lengthFieldLength");
        int lengthAdjustment = ChatConfiguration.getIntProperty("lengthAdjustment");
        int initialBytesToStrip = ChatConfiguration.getIntProperty("initialBytesToStrip");

        final LoggingHandler loggingHandler = new LoggingHandler();
        MessageCodec mCodec = new MessageCodec();
        ServerActionDispatcher mDispatcher = new ServerActionDispatcher();
        ServerExceptionHandler mExceptionHandler = new ServerExceptionHandler();
        final QuitHandler quitHandler = new QuitHandler();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(mBoss, mWorker);
        bootstrap.localAddress(ChatConfiguration.getPort());
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.childHandler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(new IdleStateHandler(5, 0, 0));
                pipeline.addLast(new ReadTimeoutHandler());
                pipeline.addLast(new CodecFrameDecoder(maxFrameLength, lengthFieldOffset, lengthFieldLength,
                        lengthAdjustment, initialBytesToStrip));
                pipeline.addLast(mCodec);
                pipeline.addLast(loggingHandler);
                pipeline.addLast(mDispatcher);
                pipeline.addLast(quitHandler);
                pipeline.addLast(mExceptionHandler);
            }
        });

        try {
            Channel channel = bootstrap.bind().sync().channel();
            log.info("Server has started.");
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        } finally {
            mBoss.shutdownGracefully();
            mWorker.shutdownGracefully();
        }
    }

    private static class ReadTimeoutHandler extends ChannelDuplexHandler {

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                log.debug("已经5s没有读到数据了");
            }
        }
    }
}
