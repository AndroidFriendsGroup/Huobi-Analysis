package com.razerdp.huobi.analysis.base.net.exception;

import java.io.IOException;

public class NetException extends IOException {
    String errorCode;
    String type;
    public NetException(String code,String message) {
        super(message);
        this.errorCode = code;
    }
    public NetException(String code,String message,String type) {
        super(message);
        this.errorCode = code;
        this.type=type;
    }
    public String getErrorCode() {
        return errorCode;
    }

    public String getType() {
        return type;
    }
}
