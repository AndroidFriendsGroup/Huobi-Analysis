package com.razerdp.huobi.analysis.base.net.interceptor;

import com.razerdp.huobi.analysis.base.net.NetManager;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Created by 大灯泡 on 2021/5/24
 * <p>
 * Description：
 */
public class SignInterceptor implements Interceptor {
    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        return chain.proceed(NetManager.Sign.sign(chain.request()));
    }
}
