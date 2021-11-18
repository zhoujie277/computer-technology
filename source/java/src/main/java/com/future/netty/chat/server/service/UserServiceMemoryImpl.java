package com.future.netty.chat.server.service;

import java.util.HashMap;
import java.util.Map;

public class UserServiceMemoryImpl implements UserService {
    private Map<String, String> allUsers = new HashMap<>();
    {
        allUsers.put("zhangsan", "zhangsan");
        allUsers.put("lisi", "lisi");
        allUsers.put("wangwu", "wangwu");
        allUsers.put("zhaoliu", "zhaoliu");
        allUsers.put("qianqi", "qianqi");
    }

    @Override
    public boolean login(String username, String password) {
        String pwd = allUsers.get(username);
        if (pwd == null)
            return false;
        return pwd.equals(password);
    }
}
