package com.future.netty.chat.server.service;

import java.util.HashMap;
import java.util.Map;

import com.future.netty.chat.common.message.User;
import com.future.util.Utility;

public class UserServiceMemoryImpl implements UserService {
    private static Map<String, String> allUsers = new HashMap<>();
    static {
        allUsers.put("zhangsan", "zhangsan");
        allUsers.put("lisi", "lisi");
        allUsers.put("wangwu", "wangwu");
        allUsers.put("zhaoliu", "zhaoliu");
        allUsers.put("qianqi", "qianqi");
    }

    @Override
    public User login(String username, String password) {
        String pwd = allUsers.get(username);
        if (pwd == null || !pwd.equals(password))
            return null;
        User user = new User();
        user.setUid(Utility.UUID());
        user.setName(username);
        user.setToken(password);
        return user;
    }
}
