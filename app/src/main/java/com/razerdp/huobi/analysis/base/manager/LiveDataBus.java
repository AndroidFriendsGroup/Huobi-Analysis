package com.razerdp.huobi.analysis.base.manager;

import com.razerdp.huobi.analysis.base.livedata.HLiveData;
import com.razerdp.huobi.analysis.entity.UserInfo;

/**
 * Created by 大灯泡 on 2021/5/26
 * <p>
 * Description：
 */
public enum LiveDataBus {
    INSTANCE;

    HLiveData<UserInfo> myAssets;

    public HLiveData<UserInfo> getMyAssetsLiveData() {
        if (myAssets == null) {
            myAssets = new HLiveData<>();
        }
        return myAssets;
    }


}
