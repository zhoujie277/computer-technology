package com.future.netty.practice;

import java.io.FileInputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AsciiString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpServer {

    // Handler需要声明泛型为<FullHttpRequest>，声明之后，只有msg为FullHttpRequest的消息才能进来。
    // 如果没有aggregator，那么一个http请求就会通过多个Channel被处理，这对我们的业务开发是不方便的，而aggregator的作用就在于此。
    private static class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private AsciiString contentType = HttpHeaderValues.TEXT_PLAIN;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
            log.debug("class:" + msg.getClass().getName() + ", currentThread=" + Thread.currentThread());
            DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                    Unpooled.wrappedBuffer("test".getBytes()));
            HttpHeaders headers = resp.headers();
            headers.add(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
            // 添加header描述length。这一步是很重要的一步，如果没有这一步，你会发现用postman发出请求之后就一直在刷新，因为http请求方不知道返回的数据到底有多长。
            // channel读取完成之后需要输出缓冲流。如果没有这一步，你会发现postman同样会一直在刷新。
            headers.add(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());
            headers.add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.write(resp);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            log.debug("channelReadComplete");
            super.channelReadComplete(ctx);
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.debug("exceptionCaught");
            if (null != cause)
                cause.printStackTrace();
            if (null != ctx)
                ctx.close();
        }

    }

    public static class SSLChannelInitializer extends ChannelInitializer<Channel> {
        private final SslContext sslContext;

        public SSLChannelInitializer() {
            String keyStoreFilePath = "/xxx/path";
            String keyStorePassword = "Password";
            try {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(new FileInputStream(keyStoreFilePath), keyStorePassword.toCharArray());
                KeyManagerFactory keyManagerFactory = KeyManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
                sslContext = SslContextBuilder.forServer(keyManagerFactory).build();
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            SSLEngine sslEngine = sslContext.newEngine(ch.alloc());
            pipeline.addLast(new SslHandler(sslEngine)).addLast("decoder", new HttpRequestDecoder())
                    .addLast("encoder", new HttpResponseEncoder())
                    .addLast("aggregator", new HttpObjectAggregator(512 * 1024)).addLast("handler", new HttpHandler());

        }

    }

    private final int port;

    public HttpServer(int port) {
        this.port = port;
    }

    public void run() {
        ServerBootstrap bootstrap = new ServerBootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup();
        bootstrap.group(group).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<Channel>() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
                log.debug("initChannel ch:" + ch);
                // HttpRequestDecoder，用于解码request
                // HttpResponseEncoder，用于编码response
                // aggregator，消息聚合器。为什么能有FullHttpRequest这个东西，就是因为有他，
                // HttpObjectAggregator，如果没有他，就不会有那个消息是FullHttpRequest的那段Channel，同样也不会有FullHttpResponse。
                // 512 * 1024的参数含义是消息合并的数据大小。代表聚合的消息内容长度不超过512kb
                // 最后添加我们自己的处理接口
                ch.pipeline().addLast("decoder", new HttpRequestDecoder()).addLast("encoder", new HttpResponseEncoder())
                        .addLast("aggregator", new HttpObjectAggregator(512 * 1024))
                        .addLast("handler", new HttpHandler());
            }

        }).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);
        try {
            bootstrap.bind(port).sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new HttpServer(8080).run();
    }
}