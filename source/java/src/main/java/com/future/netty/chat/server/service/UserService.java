package com.future.netty.chat.server.service;

import com.future.netty.chat.common.message.User;

public interface UserService {

    User login(String username, String password);
}
