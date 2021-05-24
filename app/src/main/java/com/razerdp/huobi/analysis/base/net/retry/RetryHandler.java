package com.razerdp.huobi.analysis.base.net.retry;


import com.razerdp.huobi.analysis.utils.log.HLog;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

public class RetryHandler implements Function<Observable<? extends Throwable>, Observable<?>> {
    private static final String TAG = "RetryHandler";
    private int maxRetryCount;
    private int retryInterval;

    public RetryHandler() {
        this(5, 500);
    }

    public RetryHandler(int maxRetryCount) {
        this(maxRetryCount, 500);
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
                return Observable.timer(retryInterval, TimeUnit.MILLISECONDS);
            }
            return Observable.error(throwable);
        });
    }
}
