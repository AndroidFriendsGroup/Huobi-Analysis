package com.razerdp.huobi.analysis.entity;

import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by 大灯泡 on 2021/5/19
 * <p>
 * Description：
 */
public class UserInfo implements Serializable {
    public final String accetKey;
    public final String secretKey;
    public String name;
    public long accountId;
    public String assets;

    public transient boolean isRefreshing;

    public UserInfo(String accetKey, String secretKey) {
        this.accetKey = accetKey;
        this.secretKey = secretKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserInfo userInfo = (UserInfo) o;
        return accetKey.equals(userInfo.accetKey) &&
                secretKey.equals(userInfo.secretKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accetKey, secretKey);
    }

}
