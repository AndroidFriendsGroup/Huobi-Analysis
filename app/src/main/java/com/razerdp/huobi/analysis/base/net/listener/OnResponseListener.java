package com.razerdp.huobi.analysis.base.net.listener;

import android.annotation.SuppressLint;

import com.razerdp.huobi.analysis.base.net.exception.NetException;
import com.razerdp.huobi.analysis.utils.log.HLog;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Created by 大灯泡 on 2021/5/20
 * <p>
 * Description：
 */
public abstract class OnResponseListener<T> implements Observer<T> {

    @Override
    public void onSubscribe(@NotNull Disposable d) {
    }

    @SuppressLint("CheckResult")
    @Override
    public void onNext(@NotNull T t) {
        onSuccess(t);
        onComplete();
    }

    public abstract void onSuccess(@NotNull T t);

    @Override
    public void onError(@NotNull Throwable e) {
        if (e instanceof NetException) {
            onError(((NetException) e).getErrorCode(), e);
        }
        onComplete();
    }

    public void onError(String errorCode, @NotNull Throwable e) {
        HLog.e("onError", errorCode, e.getMessage());
    }

    @Override
    public void onComplete() {
    }
}
