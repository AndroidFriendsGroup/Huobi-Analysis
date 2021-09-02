package com.razerdp.huobi.analysis.ui.activity;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.razerdp.huobi.analysis.base.baseactivity.BaseActivity;
import com.razerdp.huobi.analysis.base.baseadapter.BaseSimpleRecyclerViewHolder;
import com.razerdp.huobi.analysis.base.baseadapter.SimpleRecyclerViewAdapter;
import com.razerdp.huobi.analysis.base.manager.DataManager;
import com.razerdp.huobi.analysis.base.manager.LiveDataBus;
import com.razerdp.huobi.analysis.base.net.listener.OnResponseListener;
import com.razerdp.huobi.analysis.base.net.retry.RetryHandler;
import com.razerdp.huobi.analysis.entity.UserInfo;
import com.razerdp.huobi.analysis.net.api.account.AccountAssets;
import com.razerdp.huobi.analysis.net.api.market.Trade;
import com.razerdp.huobi.analysis.net.api.order.History;
import com.razerdp.huobi.analysis.net.response.account.BalanceResponse;
import com.razerdp.huobi.analysis.net.response.market.TradeResponse;
import com.razerdp.huobi.analysis.net.response.order.HistoryOrderResponse;
import com.razerdp.huobi.analysis.ui.widget.DPRecyclerView;
import com.razerdp.huobi.analysis.utils.ButterKnifeUtil;
import com.razerdp.huobi.analysis.utils.NumberUtils;
import com.razerdp.huobi.analysis.utils.SpanUtil;
import com.razerdp.huobi.analysis.utils.TimeUtil;
import com.razerdp.huobi.analysis.utils.ToolUtil;
import com.razerdp.huobi.analysis.utils.UIHelper;
import com.razerdp.huobi.analysis.utils.log.HLog;
import com.razerdp.huobi.analysis.utils.rx.RxHelper;
import com.razerdp.huobi_analysis.R;
import com.rxjava.rxlife.RxLife;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import io.reactivex.disposables.Disposable;
import rxhttp.RxHttp;

public class DetailActivity extends BaseActivity<DetailActivity.Data> {

    static final String INCOME_FORMAT = "%s (%.2f%%)";
    static final String INCOME_FORMAT_POSITIVE = "%s (+%.2f%%)";

    @BindView(R.id.tv_state)
    TextView mTvState;
    @BindView(R.id.rv_content)
    DPRecyclerView rvContent;
    @BindView(R.id.tv_all_cost)
    TextView mTvAllCost;
    @BindView(R.id.tv_all_profit)
    TextView mTvAllProfit;


    UserInfo userInfo;

    SimpleRecyclerViewAdapter<DetailInfo> mAdapter;
    Map<String, Disposable> disposableMap;

    @Override
    protected boolean onCheckIntentDataValidate(@Nullable Data activityData) {
        if (activityData == null || activityData.userInfo == null) {
            return false;
        }
        userInfo = activityData.userInfo;
        return true;
    }

    @Override
    public int contentViewLayoutId() {
        return R.layout.activity_detail;
    }

    @Override
    protected void onInitView(View decorView) {
        disposableMap = new HashMap<>();
        updateTitle(userInfo);
        mTvState.setOnClickListener(v -> requestBalance());
        requestBalance();
        LiveDataBus.INSTANCE.getMyAssetsLiveData().observe(this, this::updateTitle);
    }

    void refreshAllCostAndProfit() {
        double cost = 0;
        double profit = 0;
        for (DetailInfo data : mAdapter.getDatas()) {
            double dataCost = data.costMode == DetailInfo.MODE_IDLE ? data.getAveragePrice() * data.myAmount : 0;
            double dataIncome = data.incomeMode == DetailInfo.MODE_IDLE ? data.getIncome() : 0;
            if (Double.isNaN(dataCost) || Double.isNaN(dataIncome)) {
                continue;
            }
            cost += dataCost;
            profit += dataIncome;
        }
        String formattedCost = NumberUtils.getPrice(cost);
        String formattedProfit = NumberUtils.getPrice(profit);
        if (cost != 0) {
            formattedProfit = String.format(Locale.getDefault(),
                                            profit > 0 ? INCOME_FORMAT_POSITIVE : INCOME_FORMAT,
                                            formattedProfit,
                                            100 * profit / cost);
        }
        SpanUtil.create(String.format("预估总成本（USDT）：\n%s", formattedCost))
                .append(formattedCost)
                .setTextColor(UIHelper.getColor(R.color.text_black2))
                .setTextSize(12)
                .into(mTvAllCost);
        SpanUtil.create(String.format("预估总收益（USDT）：\n%s", formattedProfit))
                .append(formattedProfit)
                .setTextColor(profit > 0 ? UIHelper.getColor(R.color.common_red) : UIHelper
                        .getColor(R.color.common_green))
                .setTextSize(12)
                .into(mTvAllProfit);
    }

    void updateTitle(UserInfo userInfo) {
        String money = String.format("CNY:%s", userInfo.assets);
        setTitle(SpanUtil.create(String.format("%s\n%s", userInfo.name, money))
                         .append(money).setTextSize(12)
                         .getSpannableStringBuilder(Color.WHITE));
    }

    void initRvContent() {
        if (userInfo.balances == null) {
            return;
        }
        if (mAdapter != null) {
            mAdapter.getDatas().clear();
            mAdapter.getDatas().addAll(processData());
            mAdapter.notifyDataSetChanged();
        } else {
            mAdapter = new SimpleRecyclerViewAdapter<>(this, processData());
            mAdapter.setHolder(Holder.class);
            mAdapter.outher(this);
            rvContent.setLayoutManager(new LinearLayoutManager(this));
            rvContent.setItemAnimator(null);
            rvContent.setAdapter(mAdapter);
        }
        for (DetailInfo data : mAdapter.getDatas()) {
            requestCost(data);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        DataManager.INSTANCE.save(null);
    }

    List<DetailInfo> processData() {
        List<DetailInfo> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : userInfo.balances.entrySet()) {
            DetailInfo detailInfo = new DetailInfo();
            detailInfo.tradingPair = entry.getKey();
            detailInfo.myAmount = entry.getValue();
            result.add(detailInfo);
        }
        Collections.sort(result, mComparator);
        return result;
    }

    void requestBalance() {
        mTvState.setVisibility(View.VISIBLE);
        mTvState.setText("正在获取现货余额...");
        RxHttp.get(AccountAssets.balanceApi(userInfo.accountId), userInfo)
                .asResponse(BalanceResponse.class)
                .retryWhen(new RetryHandler(5, 800))
                .as(RxLife.asOnMain(self()))
                .subscribe(new OnResponseListener<BalanceResponse>() {
                    @Override
                    public void onSuccess(@NotNull BalanceResponse balanceResponse) {
                        mTvState.setVisibility(View.GONE);
                        balanceResponse.fillInUser(userInfo);
                        initRvContent();
                    }

                    @Override
                    public void onError(String errorCode, @NotNull Throwable e) {
                        super.onError(errorCode, e);
                        mTvState.setText("获取失败，点击重新获取");
                    }
                });
    }

    // 窗口只有48h。。。所以需要一直往前推
    void requestCost(DetailInfo detailInfo) {
        if (detailInfo == null) {
            return;
        }
        onRequest(detailInfo);
        if (!detailInfo.ignoreCache) {
            List<HistoryOrderResponse> cache = DataManager.INSTANCE.getCacheHistoryOrders(detailInfo.tradingPair, detailInfo.endTime);
            if (cache != null) {
                postOnSuccess(detailInfo, cache);
                return;
            }
        }
        RxHttp.get(History.historyOrders(), userInfo)
                .addQuery("symbol", detailInfo.getRequestTradePairs())
                .addQuery("types", "buy-limit,buy-market,buy-limit-maker")
                .addQuery("end-time", detailInfo.endTime)
                .asResponseList(HistoryOrderResponse.class)
                .retryWhen(new RetryHandler(5, 500))
                .as(RxLife.asOnMain(self()))
                .subscribe(new OnResponseListener<List<HistoryOrderResponse>>() {
                    @Override
                    public void onSuccess(@NotNull List<HistoryOrderResponse> historyOrderResponses) {
                        postOnSuccess(detailInfo, historyOrderResponses);
                    }

                    @Override
                    public void onError(String errorCode, @NotNull Throwable e) {
                        super.onError(errorCode, e);
                        detailInfo.costMode = DetailInfo.MODE_ERROR;
                        detailInfo.incomeMode = DetailInfo.MODE_ERROR;
                        mAdapter.notifyItemChanged(detailInfo);
                    }
                });
    }

    private void postOnSuccess(DetailInfo detailInfo, @NonNull List<HistoryOrderResponse> historyOrderResponses) {
        boolean continueRequest = true;
        if (!ToolUtil.isEmpty(historyOrderResponses)) {
            for (HistoryOrderResponse data : historyOrderResponses) {
                detailInfo.recordTrades(data);
                continueRequest = detailInfo.amount < detailInfo.myAmount;
                if (!continueRequest) {
                    break;
                }
            }
        }
        // 达到查询日期的极限
        if (detailInfo.endTime == detailInfo.overTime) {
            continueRequest = false;
        }
        DataManager.INSTANCE.cacheHistoryOrders(detailInfo.tradingPair, historyOrderResponses);
        if (continueRequest) {
            detailInfo.endTime = Math.max(detailInfo.endTime - TimeUtil.DAY * 2 * 1000, detailInfo.overTime);
            requestCost(detailInfo);
        } else {
            detailInfo.ignoreCache = false;
            DataManager.INSTANCE.save(detailInfo.tradingPair);
            detailInfo.getAveragePrice();
            detailInfo.costMode = DetailInfo.MODE_IDLE;
            mAdapter.notifyItemChanged(detailInfo);
            requestNewestPrice(detailInfo);
        }
    }

    void onRequest(DetailInfo detailInfo) {
        if (detailInfo.costMode != DetailInfo.MODE_REFRESHING) {
            detailInfo.costMode = DetailInfo.MODE_REFRESHING;
            mAdapter.notifyItemChanged(detailInfo);
        }
        if (detailInfo.incomeMode != DetailInfo.MODE_REFRESHING) {
            detailInfo.incomeMode = DetailInfo.MODE_REFRESHING;
            mAdapter.notifyItemChanged(detailInfo);
        }
        if (detailInfo.endTime == 0) {
            detailInfo.endTime = System.currentTimeMillis();
            detailInfo.overTime = detailInfo.endTime - TimeUtil.DAY * 119 * 1000;
        }
        if (disposableMap.containsKey(detailInfo.tradingPair)) {
            disposableMap.get(detailInfo.tradingPair).dispose();
            disposableMap.remove(detailInfo.tradingPair);
        }
    }

    // 获取最新价格
    void requestNewestPrice(DetailInfo detailInfo) {
        if (detailInfo == null) {
            return;
        }
        RxHttp.get(Trade.newestTrade(), userInfo)
                .addQuery("symbol", detailInfo.getRequestTradePairs())
                .asResponseList(TradeResponse.class)
                .retryWhen(new RetryHandler(5, 500))
                .as(RxLife.asOnMain(self()))
                .subscribe(new OnResponseListener<List<TradeResponse>>() {
                    @Override
                    public void onSuccess(@NotNull List<TradeResponse> tradeResponses) {
                        if (detailInfo.costMode == DetailInfo.MODE_REFRESHING) {
                            return;
                        }
                        if (ToolUtil.isEmpty(tradeResponses)) {
                            detailInfo.newestPrice = 0;
                            detailInfo.incomeMode = DetailInfo.MODE_ERROR;
                            mAdapter.notifyItemChanged(detailInfo);
                            return;
                        }
                        TradeResponse rep = tradeResponses.get(0);
                        if (ToolUtil.isEmpty(rep.data)) {
                            detailInfo.newestPrice = 0;
                            detailInfo.incomeMode = DetailInfo.MODE_ERROR;
                            mAdapter.notifyItemChanged(detailInfo);
                            return;
                        }
                        TradeResponse.TradeInfo data = rep.data.get(0);
                        detailInfo.newestPrice = data.price;
                        detailInfo.incomeMode = DetailInfo.MODE_IDLE;
                        mAdapter.notifyItemChanged(detailInfo);
                        refreshAllCostAndProfit();
                        if (disposableMap.get(detailInfo.tradingPair) == null) {
                            // 总价值大于2个usdt才自动更新
                            if (detailInfo.newestPrice * detailInfo.myAmount > 2) {
                                disposableMap.put(detailInfo.tradingPair,
                                                  RxHelper.loop(3000,
                                                                3000,
                                                                _void -> requestNewestPrice(
                                                                        detailInfo)));
                            }
                        }
                    }

                    @Override
                    public void onError(String errorCode, @NotNull Throwable e) {
                        super.onError(errorCode, e);
                        detailInfo.incomeMode = DetailInfo.MODE_ERROR;
                        mAdapter.notifyItemChanged(detailInfo);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        for (Disposable value : disposableMap.values()) {
            value.dispose();
        }
        super.onDestroy();
    }

    class Holder extends BaseSimpleRecyclerViewHolder<DetailInfo> {

        @BindView(R.id.tv_currency)
        TextView mTvCurrency;
        @BindView(R.id.tv_amount)
        TextView mTvAmount;
        @BindView(R.id.tv_cost)
        TextView mTvCost;
        @BindView(R.id.tv_income)
        TextView mTvIncome;
        @BindView(R.id.layout_cost)
        View layoutCost;
        @BindView(R.id.layout_income)
        View layoutIncome;
        @BindView(R.id.tv_total_hold)
        TextView mTvHolder;


        public Holder(@NonNull @NotNull View itemView) {
            super(itemView);
            ButterKnifeUtil.bind(this, itemView);
            layoutCost.setOnClickListener(v -> {
                if (getData().costMode == DetailInfo.MODE_REFRESHING) {
                    return;
                }
                getData().clearInnerData();
                getData().ignoreCache = true;
                requestCost(getData());
            });
            layoutIncome.setOnClickListener(v -> {
                if (getData().incomeMode == DetailInfo.MODE_REFRESHING) {
                    return;
                }
                getData().incomeMode = DetailInfo.MODE_REFRESHING;
                requestNewestPrice(getData());
            });
        }

        @Override
        public int inflateLayoutResourceId() {
            return R.layout.item_detail;
        }

        @Override
        public void onBindData(DetailInfo data, int position) {
            if (data.newestPrice != 0) {
                String formatted = String.format("%s USDT",
                                                 NumberUtils.formatDecimal(data.newestPrice, 8));
                SpanUtil.create(String.format("%s  %s", data.getCurrencyName(), formatted))
                        .append(formatted)
                        .setTextColor(UIHelper.getColor(R.color.text_black3))
                        .setTextSize(12)
                        .into(mTvCurrency);
            } else {
                mTvCurrency.setText(data.getCurrencyName());
            }
            mTvAmount.setText(NumberUtils.formatDecimal(data.myAmount, 4));
            double myHolder = data.myAmount * data.getAveragePrice();
            mTvHolder.setText(NumberUtils.formatDecimal(myHolder, 4));
            switch (data.costMode) {
                case DetailInfo.MODE_IDLE:
                    mTvCost.setText(NumberUtils.getPrice(data.getAveragePrice()));
                    break;
                case DetailInfo.MODE_REFRESHING:
                    mTvCost.setText("正在刷新");
                    break;
                case DetailInfo.MODE_ERROR:
                    mTvCost.setText("获取失败");
                    break;
            }
            switch (data.incomeMode) {
                case DetailInfo.MODE_IDLE:
                    double income = data.getIncome();
                    mTvIncome.setTextColor(income > 0 ? UIHelper.getColor(R.color.common_red) : UIHelper
                            .getColor(R.color.common_green));
                    mTvIncome.setText(String.format(income > 0 ? INCOME_FORMAT_POSITIVE : INCOME_FORMAT,
                                                    NumberUtils.getPrice(income),
                                                    100 * income / myHolder));
                    break;
                case DetailInfo.MODE_REFRESHING:
                    mTvIncome.setTextColor(UIHelper.getColor(R.color.text_black2));
                    mTvIncome.setText("正在刷新");
                    break;
                case DetailInfo.MODE_ERROR:
                    mTvIncome.setTextColor(UIHelper.getColor(R.color.text_black2));
                    mTvIncome.setText("获取失败");
                    break;
            }
        }

    }

    static class DetailInfo {
        public static final int MODE_IDLE = 0;
        public static final int MODE_REFRESHING = 1;
        public static final int MODE_ERROR = 2;
        String tradingPair;
        String currencyName;
        double myAmount;
        Map<String, Void> orderIDMaps;

        int costMode;
        int incomeMode;

        // inner
        List<Double> amounts;
        List<Double> prices;
        double amount;
        long endTime;
        long overTime;
        boolean isChange;
        double cacheAveragePrice;
        double newestPrice;
        boolean ignoreCache;

        public DetailInfo() {
            amounts = new ArrayList<>();
            prices = new ArrayList<>();
            orderIDMaps = new HashMap<>();
        }

        String getRequestTradePairs() {
            return tradingPair + "usdt";
        }

        String getCurrencyName() {
            if (currencyName != null) return currencyName;
            if (tradingPair.toLowerCase().endsWith("3l")) {
                currencyName = String.format("%s(*3)",
                                             tradingPair.substring(0, tradingPair.length() - 2));
            } else if (tradingPair.toLowerCase().endsWith("3s")) {
                currencyName = String.format("%s(*-3)",
                                             tradingPair.substring(0, tradingPair.length() - 2));
            } else {
                currencyName = tradingPair;
            }
            return currencyName;
        }

        public void recordTrades(HistoryOrderResponse response) {
            if (response == null) {
                return;
            }
            String orderId = String.valueOf(response.orderID);
            if (orderIDMaps.containsKey(orderId)) return;
            orderIDMaps.put(orderId, null);
            amounts.add(response.amount);
            this.amount += response.amount;
            prices.add(response.price);
            this.isChange = true;
        }

        void clearInnerData() {
            amount = 0;
            amounts.clear();
            prices.clear();
            orderIDMaps.clear();
            endTime = 0;
            isChange = true;
            cacheAveragePrice = 0;
            newestPrice = 0;
        }

        double getAveragePrice() {
            if (isChange || cacheAveragePrice == 0) {
                HLog.i("getAveragePrice", tradingPair, prices, amounts, myAmount);
                double all = 0;
                for (int i = 0; i < prices.size(); i++) {
                    all += prices.get(i) * amounts.get(i);
                }
                try {
                    cacheAveragePrice = all / amount;
                } catch (Exception e) {
                    // pass
                }
                isChange = false;
            }
            return cacheAveragePrice;
        }

        double getIncome() {
            return myAmount * (newestPrice - cacheAveragePrice);
        }

    }

    public static class Data extends BaseActivity.IntentData {

        UserInfo userInfo;

        public Data setUserInfo(UserInfo userInfo) {
            this.userInfo = userInfo;
            return this;
        }
    }

    private Comparator<DetailInfo> mComparator = (o1, o2) -> -Double.compare(o1.myAmount,
                                                                             o2.myAmount);

}