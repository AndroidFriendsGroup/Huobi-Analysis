package com.razerdp.huobi.analysis.net.response.base;

/**
 * Created by 大灯泡 on 2021/5/20
 * <p>
 * Description：v2接口返回格式：最上层有三个字段：code, message 和 data。前两个字段表示返回码和错误消息，实际的业务数据在data字段里。
 * https://huobiapi.github.io/docs/spot/v1/cn/#dac673286f
 */
public class BaseResponse2<T> {
    //API接口返回码
    protected int code;
    //错误消息（如果有）
    protected String message;
    protected long ts;
    public T data;

    public boolean isOK() {
        return code == 200;
    }

}
