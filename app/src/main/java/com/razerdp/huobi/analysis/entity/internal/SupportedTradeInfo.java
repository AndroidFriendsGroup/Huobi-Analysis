package com.razerdp.huobi.analysis.entity.internal;

import com.google.gson.annotations.SerializedName;

/**
 * Created by 大灯泡 on 2021/5/25
 * <p>
 * Description：
 */
public class SupportedTradeInfo {

    @SerializedName("base-currency")
    public String basecurrency;
    @SerializedName("quote-currency")
    public String quotecurrency;
    @SerializedName("price-precision")
    public Integer priceprecision;
    @SerializedName("amount-precision")
    public Integer amountprecision;
    @SerializedName("symbol-partition")
    public String symbolpartition;
    @SerializedName("symbol")
    public String symbol;
    @SerializedName("state")
    public String state;
    @SerializedName("value-precision")
    public Integer valueprecision;
    @SerializedName("min-order-amt")
    public Integer minorderamt;
    @SerializedName("max-order-amt")
    public Integer maxorderamt;
    @SerializedName("min-order-value")
    public Integer minordervalue;
    @SerializedName("limit-order-min-order-amt")
    public Integer limitorderminorderamt;
    @SerializedName("limit-order-max-order-amt")
    public Integer limitordermaxorderamt;
    @SerializedName("limit-order-max-buy-amt")
    public Integer limitordermaxbuyamt;
    @SerializedName("limit-order-max-sell-amt")
    public Integer limitordermaxsellamt;
    @SerializedName("sell-market-min-order-amt")
    public Integer sellmarketminorderamt;
    @SerializedName("sell-market-max-order-amt")
    public Integer sellmarketmaxorderamt;
    @SerializedName("buy-market-max-order-value")
    public Integer buymarketmaxordervalue;
    @SerializedName("api-trading")
    public String apitrading;
    @SerializedName("tags")
    public String tags;
}
