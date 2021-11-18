package com.future.netty.chat.protocol;

/**
 * 自定义协议要素
 * 1. 魔数。用来第一时间判定是否是无效数据包
 * 2. 版本号。可以支持协议的升级
 * 3. 序列化算法。消息正文到底采用哪种序列化反序列化方式。例如：json、protobuf、hessian
 * 4. 指令类型。是登录、注册、单聊、群聊... 跟业务相关
 * 5. 请求序号。为了双工通信，提供异步能力。
 * 6. 正文长度。
 * 7. 消息正文。
 * 
 */
public interface readme {
    
}
