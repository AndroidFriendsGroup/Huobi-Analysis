package com.razerdp.huobi.analysis.net.response.account;

import com.google.gson.annotations.SerializedName;

/**
 * Created by 大灯泡 on 2021/5/20
 * <p>
 * Description：
 */
public class AccountResponse {

    @SerializedName("id")
    public long accountid;
    //spot：现货账户, margin：逐仓杠杆账户, otc：OTC 账户, point：点卡账户, super-margin：全仓杠杆账户, investment: C2C杠杆借出账户, borrow: C2C杠杆借入账户，矿池账户: minepool, ETF账户: etf, 抵押借贷账户: crypto-loans
    public String type;
    //子账户
    public String subtype;
    //working：正常, lock：账户被锁定
    public String state;
}
