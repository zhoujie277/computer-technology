package com.future.netty.chat.message;

public abstract class AbstractResponseMessage extends Message {
    private boolean success;
    private String reason;

    public AbstractResponseMessage() {
    }

    public AbstractResponseMessage(boolean success, String reason) {
        this.success = success;
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "AbstractResponseMessage [reason=" + reason + ", success=" + success + "]";
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
