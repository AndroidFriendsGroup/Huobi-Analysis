package com.razerdp.huobi.analysis.net.response.listener;

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

    @Override
    public void onNext(@NotNull T t) {
        onSuccess(t);
        onComplete();
    }

    public abstract void onSuccess(@NotNull T t);

    @Override
    public void onError(@NotNull Throwable e) {
        onFailed(e);
        onComplete();
    }

    public void onFailed(@NotNull Throwable e) {

    }

    @Override
    public void onComplete() {

    }
}
