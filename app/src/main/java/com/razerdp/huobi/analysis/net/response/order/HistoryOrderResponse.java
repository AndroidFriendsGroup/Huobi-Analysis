package com.razerdp.huobi.analysis.net.response.order;

import com.google.gson.annotations.SerializedName;

import com.razerdp.huobi.analysis.net.response.base.BaseResponse;

import java.util.List;

public class HistoryOrderResponse extends BaseResponse<List<HistoryOrderResponse>> {

    // 订单时间
    @SerializedName("created-at")
    public long createTime;
    // 成交数量
    @SerializedName("filled-amount")
    public double amount;
    // 手续费
    @SerializedName("filled-fees")
    public double fees;
    // 订单成交记录 ID，无大小顺序，可作为下一次翻页查询请求的from字段
    @SerializedName("id")
    public long requestID;
    // 订单id
    @SerializedName("order-id")
    public long orderID;
    // 成交价格
    public double price;
    // 成交类型 buy: 买 sell: 卖
    public String type;
}
