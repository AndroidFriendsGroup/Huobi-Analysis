package com.razerdp.huobi.analysis.net.api.account;

/**
 * Created by 大灯泡 on 2021/5/20
 * <p>
 * Description：
 */
public class AccountAssets {

    // 用户资产
    public static String assetsApi() {
        return "/v2/account/asset-valuation";
    }

    // 用户月
    public static String balanceApi(long accountID) {
        return String.format("/v1/account/accounts/%s/balance", accountID);
    }
}
