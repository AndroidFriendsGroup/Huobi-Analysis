package com.razerdp.huobi.analysis.base.livedata;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;


import com.razerdp.huobi.analysis.utils.ToolUtil;
import com.razerdp.huobi.analysis.utils.rx.RxCall;
import com.razerdp.huobi.analysis.utils.rx.RxHelper;

import static androidx.lifecycle.Lifecycle.State.DESTROYED;

/**
 * Created by 大灯泡 on 2019/4/9.
 */
@SuppressWarnings("ALL")
public class HLiveData<T> extends MutableLiveData<T> {
    private static final String TAG = "NELiveData";
    private int mVersion = -1;

    public void send(T value) {
        if (ToolUtil.isMainThread()) {
            setValue(value);
        } else {
            postValue(value);
        }
    }

    public void unPendingSend(T value) {
        if (ToolUtil.isMainThread()) {
            setValue(value);
        } else {
            ArchTaskExecutor.getInstance().postToMainThread(new Runnable() {
                @Override
                public void run() {
                    setValue(value);
                }
            });
        }
    }

    @Override
    public void setValue(T value) {
        mVersion++;
        super.setValue(value);
    }

    int getVersion() {
        return mVersion;
    }

    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        if (owner.getLifecycle().getCurrentState() == DESTROYED) {
            // ignore
            return;
        }
        try {
            if (ToolUtil.isMainThread()) {
                super.observe(owner, new LiveDataObserverWrapper(observer, this));
            } else {
                RxHelper.runOnUiThread(new RxCall<Void>() {
                    @Override
                    public void onCall(Void data) {
                        observe(owner, new LiveDataObserverWrapper(observer, HLiveData.this));
                    }
                });
            }
        } catch (Exception e) {

        }
    }

    public void observeSticky(@NonNull LifecycleOwner owner, @NonNull Observer<T> observer) {
        if (owner.getLifecycle().getCurrentState() == DESTROYED) {
            // ignore
            return;
        }
        try {
            if (ToolUtil.isMainThread()) {
                super.observe(owner, observer);
            } else {
                RxHelper.runOnUiThread(new RxCall<Void>() {
                    @Override
                    public void onCall(Void data) {
                        observeSticky(owner, observer);
                    }
                });
            }
        } catch (Exception e) {

        }

    }

    @Override
    public void observeForever(@NonNull Observer<? super T> observer) {
        try {
            if (ToolUtil.isMainThread()) {
                super.observeForever(new LiveDataObserverWrapper(observer, this));
            } else {
                RxHelper.runOnUiThread(new RxCall<Void>() {
                    @Override
                    public void onCall(Void data) {
                        observeForever(new LiveDataObserverWrapper(observer, HLiveData.this));
                    }
                });
            }
        } catch (Exception e) {

        }
    }


    public void observeForeverSticky(@NonNull Observer<T> observer) {
        try {

            if (ToolUtil.isMainThread()) {
                super.observeForever(observer);
            } else {
                RxHelper.runOnUiThread(new RxCall<Void>() {
                    @Override
                    public void onCall(Void data) {
                        observeForever(observer);
                    }
                });
            }
        } catch (Exception e) {
        }
    }
}
