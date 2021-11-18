package com.future.netty.chat.client;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import com.future.io.nio.NIOConfig;
import com.future.netty.chat.message.ChatRequestMessage;
import com.future.netty.chat.message.GroupChatRequestMessage;
import com.future.netty.chat.message.GroupCreateRequestMessage;
import com.future.netty.chat.message.GroupJoinRequestMessage;
import com.future.netty.chat.message.GroupMembersRequestMessage;
import com.future.netty.chat.message.GroupQuitRequestMessage;
import com.future.netty.chat.message.LoginRequestMessage;
import com.future.netty.chat.message.LoginResponseMessage;
import com.future.netty.chat.message.PingMessage;
import com.future.netty.chat.protocol.MessageCodecSharable;
import com.future.netty.chat.protocol.ProtocolFrameDecoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatClient {
    // 倒计时锁，【主次线程之间 通信】
    CountDownLatch WAIT_FOR_LOGIN = new CountDownLatch(1); // 初始基数1，减为零才继续往下运行，否则等待
    // 登录状态 初始值 false 【主次线程之间 共享变量】
    AtomicBoolean LOGIN = new AtomicBoolean(false);
    AtomicBoolean EXIT = new AtomicBoolean(false);

    private class LoginHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // 1. 处理登录 [登录成功 登录状态=true]
            if ((msg instanceof LoginResponseMessage)) {
                LoginResponseMessage responseMessage = (LoginResponseMessage) msg;
                if (responseMessage.isSuccess())
                    LOGIN.set(true);

                WAIT_FOR_LOGIN.countDown(); // 减一 唤醒 线程：system in
            }
            super.channelRead(ctx, msg);
        }

        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception {
            // 连接建立时回调
            // TODO 此处新开线程真的好嘛？
            new Thread(new Runnable() {

                @Override
                public void run() {
                    final Scanner scanner = new Scanner(System.in);
                    System.out.println("请输入用户名");
                    final String username = scanner.nextLine();
                    System.out.println("请输入密码");
                    final String password = scanner.nextLine();
                    // 构造消息对象
                    final LoginRequestMessage message = new LoginRequestMessage(username, password);
                    // 发送消息
                    ctx.writeAndFlush(message);
                    // ###################### [ 2 ] ######################
                    System.out.println("等待后续操作......");
                    try {
                        WAIT_FOR_LOGIN.await(); // 【 阻塞住，等 channelRead响应回来时 继续运行 】
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // ###################### [ 4 ] ######################
                    // 登录失败 停止运行
                    if (!LOGIN.get()) {
                        ctx.channel().close(); // 触发 【channel.closeFuture().sync(); 向下运行】
                        scanner.close();
                        return;
                    }

                    while (true) {
                        System.out.println("============ 功能菜单 ============");
                        System.out.println("send [username] [content]");
                        System.out.println("gsend [group name] [content]");
                        System.out.println("gcreate [group name] [m1,m2,m3...]");
                        System.out.println("gmembers [group name]");
                        System.out.println("gjoin [group name]");
                        System.out.println("gquit [group name]");
                        System.out.println("quit");
                        System.out.println("==================================");

                        String command = scanner.nextLine();
                        final String[] s = command.split(" ");
                        switch (s[0]) {
                        case "send": // 发送消息
                            ctx.writeAndFlush(new ChatRequestMessage(username, s[1], s[2]));
                            break;
                        case "gsend": // 群里 发送消息
                            ctx.writeAndFlush(new GroupChatRequestMessage(username, s[1], s[2]));
                            break;
                        case "gcreate": // 创建群
                            final Set<String> set = new HashSet<>(Arrays.asList(s[2].split(",")));
                            set.add(username);
                            ctx.writeAndFlush(new GroupCreateRequestMessage(s[1], set));
                            break;
                        case "gmembers": // 查看群列表
                            ctx.writeAndFlush(new GroupMembersRequestMessage(s[1]));
                            break;
                        case "gjoin":
                            ctx.writeAndFlush(new GroupJoinRequestMessage(username, s[1]));
                            break;
                        case "gquit":
                            ctx.writeAndFlush(new GroupQuitRequestMessage(username, s[1]));
                            break;
                        case "quit":
                            scanner.close();
                            ctx.channel().close(); // 触发 【channel.closeFuture().sync(); 向下运行】
                            return;
                        }
                    }
                }
            }, "SystemIn").start();
        }

        // 在连接断开时触发
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("Client...主动-连接已经断开，按任意键退出..");
            EXIT.set(true);
        }

        // 在出现异常时触发
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.out.println("Client...异常-连接已经断开，按任意键退出.." + cause.getMessage());
            EXIT.set(true);
        }
    }

    public void run() {
        NioEventLoopGroup group = new NioEventLoopGroup();
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
        final LoggingHandler nettyLog = new LoggingHandler();
        final MessageCodecSharable messageCodec = new MessageCodecSharable();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.group(group);
            bootstrap.handler(new ChannelInitializer<SocketChannel>() {

                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new ProtocolFrameDecoder()).addLast("logging", nettyLog)
                            .addLast(messageCodec);
                    ch.pipeline().addLast(new IdleStateHandler(0, 3, 0));
                    ch.pipeline().addLast(new ChannelDuplexHandler() {
                        @Override
                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                            IdleStateEvent event = (IdleStateEvent) evt;
                            if (event.state() == IdleState.WRITER_IDLE) {
                                log.debug("3s arrived, send ping msg");
                                ctx.writeAndFlush(new PingMessage());
                            }
                        }
                    });
                    ch.pipeline().addLast("loginHandler", new LoginHandler());
                }
            });
            ChannelFuture channel = bootstrap
                    .connect(new InetSocketAddress(NIOConfig.getServerIP(), NIOConfig.getServerPort())).sync();
            channel.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new ChatClient().run();
    }
}
