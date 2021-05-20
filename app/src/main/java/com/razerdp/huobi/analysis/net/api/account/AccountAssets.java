package com.razerdp.huobi.analysis.net.api.account;

/**
 * Created by 大灯泡 on 2021/5/20
 * <p>
 * Description：
 */
public class AccountAssets {

    // 用户余额
    public static String assetsApi(String apiToken) {
        return String.format("/v1/account/accounts/%s/balance", apiToken);
    }

}
