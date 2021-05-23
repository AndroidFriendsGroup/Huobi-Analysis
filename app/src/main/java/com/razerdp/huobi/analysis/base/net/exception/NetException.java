package com.razerdp.huobi.analysis.base.net.exception;

import java.io.IOException;

public class NetException extends IOException {
    String errorCode;
    public NetException(String code,String message) {
        super(message);
        this.errorCode = code;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
