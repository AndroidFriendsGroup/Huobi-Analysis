package com.razerdp.huobi.analysis.net.response.base;

import com.google.gson.annotations.SerializedName;

import android.text.TextUtils;

/**
 * Created by 大灯泡 on 2021/5/20
 * <p>
 * Description：v1接口返回格式：最上层有四个字段：status, ch, ts 和 data。前三个字段表示请求状态和属性，实际的业务数据在data字段里。
 * https://huobiapi.github.io/docs/spot/v1/cn/#dac673286f
 */
public class BaseResponse<T> {
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
    public T data;

    public boolean isOK(){
        return TextUtils.equals(status,"ok");
    }

    public String getErrorMsg(){
        return errorMsg;
    }


}
