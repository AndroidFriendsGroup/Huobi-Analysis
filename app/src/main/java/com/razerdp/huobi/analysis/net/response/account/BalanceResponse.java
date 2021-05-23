package com.razerdp.huobi.analysis.net.response.account;

import android.text.TextUtils;

import com.razerdp.huobi.analysis.entity.UserInfo;
import com.razerdp.huobi.analysis.utils.log.HLog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 大灯泡 on 2021/5/20
 * <p>
 * Description：
 */
public class BalanceResponse  {

    public long id;
    public String type;
    public String state;
    public List<BalanceInfo> list;

    public static class BalanceInfo {

        public String currency;
        public String type;
        public double balance;
    }

    public void fillInUser(UserInfo userInfo) {
        if (userInfo == null) {
            return;
        }
        Map<String, Double> map = new HashMap<>();
        for (BalanceInfo balanceInfo : list) {
            if (TextUtils.equals(balanceInfo.type, "trade") || TextUtils
                    .equals(balanceInfo.type, "frozen")) {
                if (balanceInfo.balance > 0.001) {
                    double ret = 0;
                    if (map.containsKey(balanceInfo.currency)) {
                        ret = map.get(balanceInfo.currency).doubleValue();
                    }
                    map.put(balanceInfo.currency, ret + balanceInfo.balance);
                }
            }
        }
        if (userInfo.balances != null) {
            userInfo.balances.putAll(map);
        } else {
            userInfo.balances = map;
        }
        HLog.i("fillInUser", map);
    }
}
