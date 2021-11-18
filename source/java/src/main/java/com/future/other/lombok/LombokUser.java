package com.future.other.lombok;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class LombokUser {

    @Getter(AccessLevel.PROTECTED)
    @Setter
    private Integer id;
    private String userName;
    private String password;
    private String phone;
    @Getter(AccessLevel.NONE)
    private String email;

    @Getter
    @Setter
    private static String str = "";

    public static void main(String[] args) {
        LombokUser user = new LombokUser();
        user.setEmail("world");
        user.setPhone("186623232");
        user.setId(123);
        user.setUserName("userName");
        log.info(user.toString());
    }

}
