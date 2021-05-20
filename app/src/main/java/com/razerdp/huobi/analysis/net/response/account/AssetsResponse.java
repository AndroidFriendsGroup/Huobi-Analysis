package com.razerdp.huobi.analysis.net.response.account;

import com.razerdp.huobi.analysis.net.response.base.BaseResponse;

import java.util.List;

/**
 * Created by 大灯泡 on 2021/5/20
 * <p>
 * Description：
 */
public class AssetsResponse extends BaseResponse<AssetsResponse> {
    //账户 ID
    public long id;
    //working：正常 lock：账户被锁定
    public String state;
    //spot：现货账户, margin：逐仓杠杆账户, otc：OTC 账户, point：点卡账户, super-margin：全仓杠杆账户, investment: C2C杠杆借出账户, borrow: C2C杠杆借入账户，矿池账户: minepool, ETF账户: etf, 抵押借贷账户: crypto-loans
    public String type;
    public List<AssetInfo> list;

    public static class AssetInfo {
        //余额
        public String balance;
        //	币种
        public String currency;
        //trade: 交易余额，frozen: 冻结余额, loan: 待还借贷本金, interest: 待还借贷利息, lock: 锁仓, bank: 储蓄
        public String type;
    }
}
