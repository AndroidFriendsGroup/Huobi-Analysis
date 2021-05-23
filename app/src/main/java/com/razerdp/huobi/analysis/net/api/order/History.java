package com.razerdp.huobi.analysis.net.api.order;

import java.lang.annotation.Retention;

public class History {

    public static String historyOrders() {
        return "/v1/order/matchresults";
    }
}
