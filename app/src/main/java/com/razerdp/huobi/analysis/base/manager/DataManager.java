package com.razerdp.huobi.analysis.base.manager;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.ArrayMap;

import androidx.annotation.Nullable;

import com.razerdp.huobi.analysis.base.file.AppFileHelper;
import com.razerdp.huobi.analysis.base.interfaces.SimpleCallback;
import com.razerdp.huobi.analysis.base.net.listener.OnResponseListener;
import com.razerdp.huobi.analysis.base.net.retry.RetryHandler;
import com.razerdp.huobi.analysis.entity.internal.SupportedTradeInfo;
import com.razerdp.huobi.analysis.net.api.market.SupportedTrade;
import com.razerdp.huobi.analysis.net.response.order.HistoryOrderResponse;
import com.razerdp.huobi.analysis.utils.FileUtil;
import com.razerdp.huobi.analysis.utils.SharedPreferencesUtils;
import com.razerdp.huobi.analysis.utils.ToolUtil;
import com.razerdp.huobi.analysis.utils.VersionUtil;
import com.razerdp.huobi.analysis.utils.gson.GsonUtil;
import com.razerdp.huobi.analysis.utils.log.HLog;
import com.razerdp.huobi.analysis.utils.rx.RxCall;
import com.razerdp.huobi.analysis.utils.rx.RxHelper;
import com.razerdp.huobi.analysis.utils.rx.RxTaskCall;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    Map<String, HistoryQueryInfo> historyCache = new HashMap<>();
    final String filePath = AppFileHelper.getFilePath() + "cache";
    Handler mHandler = new android.os.Handler(Looper.getMainLooper());
    Runnable mRunnable = this::saveInternal;

    public void clearCache() {
        HLog.i("clearCache");
        FileUtil.deleteFile(filePath);
        historyCache.clear();
    }

    public static class HistoryQueryInfo {
        public long queryTime;
        public List<HistoryOrderResponse> cacheList;
        public Map<String, Void> orderIDMap;

        public HistoryQueryInfo() {
            cacheList = new ArrayList<>();
            orderIDMap = new ArrayMap<>();
        }

        public void add(List<HistoryOrderResponse> responses) {
            if (ToolUtil.isEmpty(responses)) return;
            if (orderIDMap == null) {
                orderIDMap = new HashMap<>();
            }
            if (cacheList == null) {
                cacheList = new ArrayList<>();
            }
            for (HistoryOrderResponse response : responses) {
                String orderId = String.valueOf(response.orderID);
                if (orderIDMap.containsKey(orderId)) {
                    continue;
                }
                cacheList.add(response);
                orderIDMap.put(orderId, null);
            }
        }

        public void onSave() {
            if (cacheList != null) {
                Collections.sort(cacheList, sortCmp);
                queryTime = cacheList.get(0).createTime;
            }
        }


        private static Comparator<HistoryOrderResponse> sortCmp = (o1, o2) -> -Long.compare(o1.createTime, o2.createTime);
    }

    public void init(SimpleCallback<Boolean> cb) {
        if (hasInit()) {
            if (cb != null) {
                cb.onCall(true);
            }
            updateDataInternal(null);
            return;
        }
        int verCode = SharedPreferencesUtils.getInt("clear_cache_ver", 0);
        if (verCode < 88) {
            clearCache();
            SharedPreferencesUtils.saveInt("clear_cache_ver", VersionUtil.getAppVersionCode());
        }
        RxHelper.runOnBackground(data -> initData());
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
                            map.put(supportedTradeInfo.basecurrency + supportedTradeInfo.quotecurrency,
                                    supportedTradeInfo);
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

    private void initData() {
        Map<String, HistoryQueryInfo> result =
                GsonUtil.INSTANCE.toHashMap(FileUtil.readFile(filePath),
                                            String.class,
                                            HistoryQueryInfo.class);
        if (result != null && !result.isEmpty()) {
            historyCache.putAll(result);
        }
    }

    @Nullable
    public List<HistoryOrderResponse> getCacheHistoryOrders(String tradePairs, long queryTime) {
        if (TextUtils.isEmpty(tradePairs)) {
            return null;
        }
        if (!historyCache.containsKey(tradePairs)) {
            return null;
        }
        HistoryQueryInfo cache = historyCache.get(tradePairs);
        if (cache != null && queryTime <= cache.queryTime) {
            return cache.cacheList;
        }
        return null;
    }

    public void cacheHistoryOrders(String tradePairs, List<HistoryOrderResponse> response) {
        if (TextUtils.isEmpty(tradePairs)) {
            return;
        }
        HistoryQueryInfo cache = null;
        if (historyCache.containsKey(tradePairs)) {
            cache = historyCache.get(tradePairs);
        }
        if (cache == null) {
            cache = new HistoryQueryInfo();
            historyCache.put(tradePairs, cache);
        }
        cache.add(response);
        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, 3000);
    }

    public void save(String tradePairs) {
        if (TextUtils.isEmpty(tradePairs)) {
            mHandler.removeCallbacks(mRunnable);
            mHandler.postDelayed(mRunnable, 1000);
            return;
        }
        HistoryQueryInfo info = historyCache.get(tradePairs);
        if (info == null) return;
        info.onSave();
        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, 3000);
    }

    void saveInternal() {
        RxHelper.runOnBackground(data -> FileUtil.writeToFile(filePath, GsonUtil.INSTANCE.toString(historyCache)));
    }
}
