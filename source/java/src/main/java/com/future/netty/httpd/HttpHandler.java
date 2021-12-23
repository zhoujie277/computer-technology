package com.future.netty.httpd;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AsciiString;
import lombok.extern.slf4j.Slf4j;

// Handler需要声明泛型为<FullHttpRequest>，声明之后，只有msg为FullHttpRequest的消息才能进来。
// 如果没有aggregator，那么一个http请求就会通过多个Channel被处理，这对我们的业务开发是不方便的，而aggregator的作用就在于此。
@Slf4j
class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private AsciiString contentType = HttpHeaderValues.TEXT_PLAIN;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        log.debug("class:" + msg.getClass().getName());
        log.debug("FullHttpRequest {}", msg);
        DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.wrappedBuffer("hello world".getBytes()));
        HttpHeaders headers = resp.headers();
        headers.add(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
        // 添加header描述length。这一步是很重要的一步，如果没有这一步，你会发现用postman发出请求之后就一直在刷新，因为http请求方不知道返回的数据到底有多长。
        // channel读取完成之后需要输出缓冲流。如果没有这一步，你会发现postman同样会一直在刷新。
        headers.add(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());
        if (msg.headers().get(HttpHeaderNames.AUTHORIZATION) == null) {
            resp.setStatus(HttpResponseStatus.UNAUTHORIZED);
            headers.add(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=\"Username\"");
        } else {
            headers.add(HttpHeaderNames.SET_COOKIE, "id=1234");
        }
        ctx.writeAndFlush(resp);
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