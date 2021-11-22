package com.future.netty.chat.common.message;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = { "name" })
public class User implements Serializable {
    private static final long serialVersionUID = 1905122041750251207L;

    private String uid;
    private String name;
    private String token;
    private String deviceID;
    private int platform;
    private String appVersion;

    public enum PLATTYPE {
        WINDOWS, MAC, ANDROID, IOS, WEB, OTHER;
    }

    public PLATTYPE platform() {
        return PLATTYPE.values()[platform];
    }

    public void plaform(PLATTYPE type) {
        this.platform = type.ordinal();
    }
}
