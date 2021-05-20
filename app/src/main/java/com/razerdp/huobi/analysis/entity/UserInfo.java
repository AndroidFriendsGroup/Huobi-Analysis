package com.razerdp.huobi.analysis.entity;

import java.io.Serializable;

/**
 * Created by 大灯泡 on 2021/5/19
 * <p>
 * Description：
 */
public class UserInfo implements Serializable {
    public String apiToken;
    public String name;
    public long accountId;
    public double assets;

    public boolean isRefreshing;
}
