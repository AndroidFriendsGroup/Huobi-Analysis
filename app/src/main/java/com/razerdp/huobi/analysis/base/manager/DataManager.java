package com.razerdp.huobi.analysis.base.manager;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.razerdp.huobi.analysis.base.file.AppFileHelper;
import com.razerdp.huobi.analysis.base.interfaces.SimpleCallback;
import com.razerdp.huobi.analysis.base.net.listener.OnResponseListener;
import com.razerdp.huobi.analysis.base.net.retry.RetryHandler;
import com.razerdp.huobi.analysis.entity.internal.SupportedTradeInfo;
import com.razerdp.huobi.analysis.net.api.market.SupportedTrade;
import com.razerdp.huobi.analysis.utils.FileUtil;
import com.razerdp.huobi.analysis.utils.ToolUtil;
import com.razerdp.huobi.analysis.utils.gson.GsonUtil;
import com.razerdp.huobi.analysis.utils.log.HLog;
import com.razerdp.huobi.analysis.utils.rx.RxCall;
import com.razerdp.huobi.analysis.utils.rx.RxHelper;
import com.razerdp.huobi.analysis.utils.rx.RxTaskCall;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.schedulers.Schedulers;
import rxhttp.RxHttp;

/**
 * Created by 大灯泡 on 2021/5/25
 * <p>
 * Description：
 */
public enum DataManager {
    INSTANCE;


    Map<String, SupportedTradeInfo> supportedTradeInfoMap = new HashMap<>();
    String supportedTradeFileName = "supportedTrade";

    public void init(SimpleCallback<Boolean> cb) {
        if (hasInit()) {
            if (cb != null) {
                cb.onCall(true);
            }
            updateDataInternal(null);
            return;
        }
        if (FileUtil.exists(new File(AppFileHelper.getFilePath() + supportedTradeFileName))) {
            RxHelper.runOnBackground(new RxTaskCall<Map<String, SupportedTradeInfo>>() {
                @Override
                public Map<String, SupportedTradeInfo> doInBackground() {
                    return GsonUtil.INSTANCE.toHashMap(
                            FileUtil.readFile(AppFileHelper.getFilePath() + supportedTradeFileName),
                            String.class,
                            SupportedTradeInfo.class
                    );
                }

                @Override
                public void onResult(Map<String, SupportedTradeInfo> result) {
                    if (!ToolUtil.isEmpty(result)) {
                        supportedTradeInfoMap.putAll(result);
                        if (cb != null) {
                            cb.onCall(true);
                        }
                        updateDataInternal(null);
                    } else {
                        updateDataInternal(cb);
                    }
                }

                @Override
                public void onError(Throwable e) {
                    HLog.e(e);
                    updateDataInternal(cb);
                }
            });
        } else {
            updateDataInternal(cb);
        }
    }

    void updateDataInternal(@Nullable SimpleCallback<Boolean> cb) {
        RxHttp.get(SupportedTrade.getSupportedTrades())
                .asResponseList(SupportedTradeInfo.class)
                .retryWhen(new RetryHandler())
                .observeOn(Schedulers.io())
                .subscribe(new OnResponseListener<List<SupportedTradeInfo>>() {
                    @Override
                    public void onSuccess(@NotNull List<SupportedTradeInfo> supportedTradeInfos) {
                        Map<String, SupportedTradeInfo> map = new HashMap<>();
                        for (SupportedTradeInfo supportedTradeInfo : supportedTradeInfos) {
                            map.put(supportedTradeInfo.basecurrency + supportedTradeInfo.quotecurrency, supportedTradeInfo);
                        }
                        if (!ToolUtil.isEmpty(map)) {
                            FileUtil.writeToFile(AppFileHelper.getFilePath() + supportedTradeFileName,
                                    GsonUtil.INSTANCE.toString(map));
                            supportedTradeInfoMap.putAll(map);
                            if (cb != null) {
                                RxHelper.runOnUiThread((RxCall<Void>) data -> cb.onCall(true));
                            }
                        } else {
                            if (cb != null) {
                                RxHelper.runOnUiThread((RxCall<Void>) data -> cb.onCall(false));
                            }
                        }
                    }

                    @Override
                    public void onError(String errorCode, @NotNull Throwable e) {
                        super.onError(errorCode, e);
                        if (cb != null) {
                            RxHelper.runOnUiThread((RxCall<Void>) data -> cb.onCall(false));
                        }
                    }
                });
    }

    public boolean hasInit() {
        return !ToolUtil.isEmpty(supportedTradeInfoMap);
    }

    public boolean isTradePairExists(String tradePairs) {
        if (!supportedTradeInfoMap.containsKey(tradePairs)) {
            return false;
        }
        SupportedTradeInfo info = supportedTradeInfoMap.get(tradePairs);
        return TextUtils.equals(info.apitrading, "enabled");
    }
}
