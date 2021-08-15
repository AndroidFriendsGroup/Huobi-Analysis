package com.razerdp.huobi.analysis.ui.activity;

import android.graphics.Color;
import android.view.View;
import android.widget.TextView;

import com.razerdp.huobi.analysis.base.baseactivity.BaseActivity;
import com.razerdp.huobi.analysis.base.baseadapter.BaseSimpleRecyclerViewHolder;
import com.razerdp.huobi.analysis.base.baseadapter.SimpleRecyclerViewAdapter;
import com.razerdp.huobi.analysis.base.interfaces.SimpleCallback;
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
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import butterknife.BindView;
import io.reactivex.disposables.Disposable;
import rxhttp.RxHttp;
import rxhttp.RxHttpGetSignParam;

public class DetailActivity extends BaseActivity<DetailActivity.Data> {


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
            cost += data.getAveragePrice() * data.myAmount;
            profit += data.getIncome();
        }
        String formattedCost = NumberUtils.getPrice(cost);
        String formattedProfit = NumberUtils.getPrice(profit);
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

    @Override
    protected void onStop() {
        super.onStop();
        DataManager.INSTANCE.save();
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
            refreshQueryId(data, new SimpleCallback<Void>() {
                @Override
                public void onCall(Void v) {
                    requestCost(data);
                }
            });
        }

    }

    List<DetailInfo> processData() {
        List<DetailInfo> result = new ArrayList<>();
        for (Map.Entry<String, Double> entry : userInfo.balances.entrySet()) {
            DetailInfo detailInfo = new DetailInfo();
            detailInfo.tradingPair = entry.getKey();
            detailInfo.myAmount = entry.getValue();
            DataManager.NewestQueryInfo newestQueryInfo = DataManager.INSTANCE.getNewestQueryInfo(
                    detailInfo.tradingPair);
            if (newestQueryInfo != null) {
                detailInfo.queryID = newestQueryInfo.id;
                detailInfo.endTime = newestQueryInfo.createTime;
            }
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

    void refreshQueryId(DetailInfo detailInfo, SimpleCallback<Void> cb) {
        if (detailInfo.queryID == 0) {
            cb.onCall(null);
        } else {
            onRequest(detailInfo);
            RxHttp.get(History.historyOrders(), userInfo)
                    .addQuery("symbol", detailInfo.getRequestTradePairs())
                    .addQuery("types", "buy-limit,buy-market,buy-limit-maker")
                    .addQuery("from", detailInfo.queryID)
                    .addQuery("end-time", detailInfo.endTime)
                    .addQuery("direct", "prev")
                    .asResponseList(HistoryOrderResponse.class)
                    .retryWhen(new RetryHandler(5, 500))
                    .as(RxLife.asOnMain(self()))
                    .subscribe(new OnResponseListener<List<HistoryOrderResponse>>() {
                        @Override
                        public void onSuccess(@NonNull List<HistoryOrderResponse> historyOrderResponses) {
                            if (!ToolUtil.isEmpty(historyOrderResponses)) {
                                HistoryOrderResponse first = historyOrderResponses.get(0);
                                detailInfo.queryID = first.requestID;
                                long nextEndTime = detailInfo.endTime + TimeUtil.DAY * 2 * 1000;
                                detailInfo.endTime = Math.min(System.currentTimeMillis(),
                                                              nextEndTime);
                                DataManager.INSTANCE.saveLastQueryId(detailInfo.tradingPair,
                                                                     first.createTime,
                                                                     first.requestID);
                                if (nextEndTime < System.currentTimeMillis()) {
                                    refreshQueryId(detailInfo, cb);
                                } else {
                                    cb.onCall(null);
                                }
                            } else {
                                cb.onCall(null);
                            }
                        }

                        @Override
                        public void onError(String errorCode, @NotNull Throwable e) {
                            cb.onCall(null);
                        }
                    });


        }
    }

    // 窗口只有48h。。。所以需要一直往前推
    void requestCost(DetailInfo detailInfo) {
        if (detailInfo == null) {
            return;
        }
        onRequest(detailInfo);
        RxHttpGetSignParam param = RxHttp.get(History.historyOrders(), userInfo)
                .addQuery("symbol", detailInfo.getRequestTradePairs())
                .addQuery("types", "buy-limit,buy-market,buy-limit-maker");
        if (detailInfo.queryID > 0) {
            param.addQuery("from", detailInfo.queryID);
        }
        param.addQuery("end-time", detailInfo.endTime);
        param.asResponseList(HistoryOrderResponse.class)
                .retryWhen(new RetryHandler(5, 500))
                .as(RxLife.asOnMain(self()))
                .subscribe(new OnResponseListener<List<HistoryOrderResponse>>() {
                    @Override
                    public void onSuccess(@NotNull List<HistoryOrderResponse> historyOrderResponses) {
                        boolean continueRequest = true;
                        if (!ToolUtil.isEmpty(historyOrderResponses)) {
                            for (HistoryOrderResponse data : historyOrderResponses) {
                                detailInfo.recordTrades(data.amount, data.price);
                                continueRequest = detailInfo.amount < detailInfo.myAmount;
                                detailInfo.queryID = data.requestID;
                                DataManager.INSTANCE.saveLastQueryId(detailInfo.tradingPair,
                                                                     data.createTime,
                                                                     data.requestID);
                                if (!continueRequest) {
                                    break;
                                }
                            }
                        } else {
                            if (detailInfo.queryID != 0) {
                                continueRequest = false;
                            }
                        }
                        if (continueRequest) {
                            detailInfo.endTime -= TimeUtil.DAY * 2 * 1000;
                            requestCost(detailInfo);
                        } else {
                            detailInfo.getAveragePrice();
                            detailInfo.costMode = DetailInfo.MODE_IDLE;
                            mAdapter.notifyItemChanged(detailInfo);
                            requestNewestPrice(detailInfo);
                        }
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


        public Holder(@NonNull @NotNull View itemView) {
            super(itemView);
            ButterKnifeUtil.bind(this, itemView);
            layoutCost.setOnClickListener(v -> {
                if (getData().costMode == DetailInfo.MODE_REFRESHING) {
                    return;
                }
                getData().clearInnerData();
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
                    mTvIncome.setText(NumberUtils.getPrice(income));
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

        int costMode;
        int incomeMode;

        // inner
        List<Double> amounts;
        List<Double> prices;
        double amount;
        long endTime;
        boolean isChange;
        double cacheAveragePrice;
        double newestPrice;
        long queryID = 0;

        public DetailInfo() {
            amounts = new ArrayList<>();
            prices = new ArrayList<>();
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

        public void recordTrades(double amount, double price) {
            amounts.add(amount);
            this.amount += amount;
            prices.add(price);
            this.isChange = true;
        }

        void clearInnerData() {
            amount = 0;
            amounts.clear();
            prices.clear();
            endTime = 0;
            isChange = true;
            cacheAveragePrice = 0;
            newestPrice = 0;
            queryID = 0;
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