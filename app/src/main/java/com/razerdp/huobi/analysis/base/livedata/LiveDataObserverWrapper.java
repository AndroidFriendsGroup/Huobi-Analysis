package com.razerdp.huobi.analysis.base.livedata;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

/**
 * Created by 大灯泡 on 2019/4/9.
 */
class LiveDataObserverWrapper<T> implements Observer<T> {
    private int lastVersion;
    private Observer<T> mTarget;
    private HLiveData<T> mLiveData;

    LiveDataObserverWrapper(Observer<T> target, HLiveData<T> liveData) {
        mTarget = target;
        mLiveData = liveData;
        lastVersion = mLiveData.getVersion();
    }

    @Override
    public void onChanged(@Nullable T t) {
        if (lastVersion >= mLiveData.getVersion()) {
            return;
        }
        lastVersion = mLiveData.getVersion();
        if (mTarget != null) {
            try {
                mTarget.onChanged(t);
            } catch (Exception e) {

            }
        }
    }
}
