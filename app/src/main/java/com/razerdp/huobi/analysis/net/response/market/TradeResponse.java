package com.razerdp.huobi.analysis.net.response.market;

import java.util.Comparator;
import java.util.List;

/**
 * Created by 大灯泡 on 2021/5/24
 * <p>
 * Description：
 */
public class TradeResponse {

    public long ts;
    public List<TradeInfo> data;

    public static class TradeInfo {
        public long ts;
        public double price;
        public double amount;
        public String direction;
    }

}
