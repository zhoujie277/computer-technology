package com.future.netty.chat.server;

import com.future.io.nio.NIOConfig;
import com.future.netty.chat.protocol.MessageCodecSharable;
import com.future.netty.chat.protocol.ProtocolFrameDecoder;
import com.future.netty.chat.server.handler.ChatMessageHandler;
import com.future.netty.chat.server.handler.GroupChatMsgHandler;
import com.future.netty.chat.server.handler.GroupCreateMsgHandler;
import com.future.netty.chat.server.handler.LoginMessageHandler;
import com.future.netty.chat.server.handler.QuitHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatServer {

    public void run() {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();
        // InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
        final LoggingHandler loggingHandler = new LoggingHandler();

        final MessageCodecSharable msgCodec = new MessageCodecSharable();

        final LoginMessageHandler loginHandler = new LoginMessageHandler();
        final ChatMessageHandler chatHandler = new ChatMessageHandler();
        final GroupCreateMsgHandler groupCreateHandler = new GroupCreateMsgHandler();
        final GroupChatMsgHandler groupChatHandler = new GroupChatMsgHandler();
        final QuitHandler quitHandler = new QuitHandler();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.group(boss, worker);
            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new IdleStateHandler(5, 0, 0)).addLast(new ChannelDuplexHandler() {
                        @Override
                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                            IdleStateEvent event = (IdleStateEvent) evt;
                            if (event.state() == IdleState.READER_IDLE) {
                                log.debug("已经5s没有读到数据了");
                            }
                        }
                    }).addLast(new ProtocolFrameDecoder()).addLast(loggingHandler).addLast(msgCodec)
                            .addLast(loginHandler).addLast(chatHandler).addLast(groupCreateHandler)
                            .addLast(groupChatHandler).addLast(quitHandler);
                }
            });
            Channel channel = bootstrap.bind(NIOConfig.getServerPort()).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new ChatServer().run();
    }
}
