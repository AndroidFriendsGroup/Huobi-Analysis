package com.razerdp.huobi.analysis.net.response.base;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.razerdp.huobi.analysis.utils.StringUtil;

/**
 * Created by 大灯泡 on 2021/5/23.
 */
public class BaseResponse<T> {

    // V1
    //API接口返回状态
    protected String status;
    //接口数据对应的数据流。部分接口没有对应数据流因此不返回此字段
    protected String ch;
    //接口返回的UTC时间的时间戳，单位毫秒
    protected long ts;
    @SerializedName("err-code")
    protected String errorCode;
    @SerializedName("err-msg")
    protected String errorMsg;


    // V2
    //API接口返回码
    protected int code;
    //错误消息（如果有）
    protected String message;

    public T data;


    public boolean isOK() {
        if (StringUtil.noEmpty(status)) {
            return TextUtils.equals(status, "ok");
        } else {
            return code == 200;
        }
    }

    public String getErrorMsg() {
        if (StringUtil.noEmpty(errorMsg)) {
            return errorMsg;
        } else {
            return message;
        }
    }

    public String getErrorCode() {
        if (StringUtil.noEmpty(errorCode)) {
            return errorCode;
        } else {
            return String.valueOf(code);
        }
    }


}
