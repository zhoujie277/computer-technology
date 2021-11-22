package com.future.netty.chat.common.codec;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * 解决粘包半包问题
 * 
 * @author future
 */
public class CodecFrameDecoder extends LengthFieldBasedFrameDecoder {

    public CodecFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment,
            int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    public CodecFrameDecoder() {
        this(1024, 6, 4, 0, 0);
    }

}
