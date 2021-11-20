package com.future.netty.chat.common.bean;

import lombok.Data;

@Data
public class ChatUser {
    PLATTYPE platform = PLATTYPE.WINDOWS;
    String uid;
    String devId;
    String token;
    String nickName = "nickName";
    String sessionId;

    // windows,mac,android, ios, web , other
    public enum PLATTYPE {
        WINDOWS, MAC, ANDROID, IOS, WEB, OTHER;
    }

    public void setPlatform(int platform) {
        PLATTYPE[] values = PLATTYPE.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i].ordinal() == platform) {
                this.platform = values[i];
            }
        }
    }
}
