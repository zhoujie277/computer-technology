package com.future.netty.chat.common.bean;

import com.future.netty.chat.proto.ProtoMsg;

import lombok.Data;

@Data
public class ChatMsg {
    // 消息类型 1：纯文本 2：音频 3：视频 4：地理位置 5：其他
    public enum MSGTYPE {
        TEXT, AUDIO, VIDEO, POS, OTHER;
    }

    public ChatMsg(ChatUser user) {
        this.user = user;
        this.setTime(System.currentTimeMillis());
        this.setFrom(user.getUid());
        this.setFromNick(user.getNickName());
    }

    private ChatUser user;
    private long msgId;
    private String from;
    private String to;
    private long time;
    private MSGTYPE msgType;
    private String content;
    private String url; // 多媒体地址
    private String property; // 附加属性
    private String fromNick; // 发送者昵称
    private String json; // 附加的json串

    public void fillMsg(ProtoMsg.MessageRequest.Builder cb) {
        if (msgId > 0) {
            cb.setMsgId(msgId);
        }
        cb.setFrom(from);
        cb.setTo(to);
        cb.setTime(time);
        cb.setMsgType(msgType.ordinal());
        cb.setContent(content);
        cb.setUrl(url);
        cb.setProperty(property);
        cb.setFromNick(fromNick);
        cb.setJson(json);
    }
}
