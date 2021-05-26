package com.razerdp.huobi.analysis.base.net.retry;


import android.text.TextUtils;

import com.razerdp.huobi.analysis.base.net.exception.NetException;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

public class RetryHandler implements Function<Observable<? extends Throwable>, Observable<?>> {
    private static final String TAG = "RetryHandler";
    private volatile int maxRetryCount;
    private int retryInterval;
    private boolean hasAddSignRetryCount = false;

    public RetryHandler() {
        this(5, 1000);
    }

    public RetryHandler(int maxRetryCount) {
        this(maxRetryCount, 1000);
    }

    public RetryHandler(int maxRetryCount, int retryInterval) {
        this.maxRetryCount = maxRetryCount;
        this.retryInterval = retryInterval;
    }

    @Override
    public Observable<?> apply(@NotNull Observable<? extends Throwable> observable) throws Exception {
        return observable.flatMap((Function<Throwable, ObservableSource<?>>) throwable -> {
            if (maxRetryCount > 0) {
                maxRetryCount--;
                if (throwable instanceof NetException) {
                    if (!hasAddSignRetryCount &&
                            (TextUtils.equals(((NetException) throwable).getErrorCode(), "api-signature-not-valid")
                                    || TextUtils.equals(((NetException) throwable).getErrorCode(), "1003"))) {
                        // 验签失败不算重试次数
                        maxRetryCount += 5;
                        hasAddSignRetryCount = true;
                    }
                }
                return Observable.timer(retryInterval, TimeUnit.MILLISECONDS);
            }
            return Observable.error(throwable);
        });
    }
}
