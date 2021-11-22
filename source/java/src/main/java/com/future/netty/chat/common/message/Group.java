package com.future.netty.chat.common.message;

import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Group implements Serializable {
    private static final long serialVersionUID = 1905122091750251207L;

    private String groupID;
    private String sessionID;
    private String name;
    private User leader;
    private List<User> members;

}
