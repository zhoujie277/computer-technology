package com.future.netty.chat.common.message;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
public abstract class Response extends Message {
    /**
     * 返回码
     */
    public enum ResultCode {

        SUCCESS(0, "Success"), AUTH_FAILED(1, "登录失败，用户名或密码错误"), NO_TOKEN(2, "没有授权码"), UNKNOW_ERROR(3, "未知错误"),
        USER_OUTLINE(5, "用户不在线"), NOT_EXISTS_GROUP(6, "不存在群");

        private Integer code;
        private String desc;

        ResultCode(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public Integer getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }

    @Getter
    @Setter
    private int code;

    @Getter
    @Setter
    private String msg;

    protected Response() {
    }

    protected Response(ResultCode result) {
        this.code = result.code;
        this.msg = result.desc;
    }

    protected Response(int code, String desc) {
        this.code = code;
        this.msg = desc;
    }

    public void setResultCode(ResultCode result) {
        this.code = result.code;
        this.msg = result.desc;
    }

    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.code;
    }

}
