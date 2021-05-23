package com.razerdp.huobi.analysis.net.response.listener;

import com.razerdp.huobi.analysis.base.net.exception.NetExcepction;
import com.razerdp.huobi.analysis.net.response.base.BaseResponse;
import com.razerdp.huobi.analysis.net.response.base.BaseResponse2;

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
        if (t instanceof BaseResponse) {
            if (!((BaseResponse<?>) t).isOK()) {
                onError(((BaseResponse<?>) t).getErrorCode(),
                        new NetExcepction(((BaseResponse<?>) t).getErrorMsg()));
                onComplete();
                return;
            }
        }
        if (t instanceof BaseResponse2) {
            if (!((BaseResponse2<?>) t).isOK()) {
                onError(String.valueOf(((BaseResponse2<?>) t).getCode()),
                        new NetExcepction(((BaseResponse2<?>) t).getMessage()));
                onComplete();
                return;
            }
        }
        onSuccess(t);
        onComplete();
    }

    public abstract void onSuccess(@NotNull T t);

    @Override
    public void onError(@NotNull Throwable e) {
        onError(null, e);
        onComplete();
    }

    public void onError(String errorCode, @NotNull Throwable e) {
    }

    @Override
    public void onComplete() {
    }
}
