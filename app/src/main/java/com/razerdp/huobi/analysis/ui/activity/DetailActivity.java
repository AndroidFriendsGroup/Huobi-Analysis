package com.razerdp.huobi.analysis.ui.activity;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.razerdp.huobi.analysis.base.baseactivity.BaseActivity;
import com.razerdp.huobi.analysis.base.baseadapter.BaseSimpleRecyclerViewHolder;
import com.razerdp.huobi.analysis.base.baseadapter.SimpleRecyclerViewAdapter;
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
import com.razerdp.huobi.analysis.utils.TimeUtil;
import com.razerdp.huobi.analysis.utils.ToolUtil;
import com.razerdp.huobi.analysis.utils.UIHelper;
import com.razerdp.huobi_analysis.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import rxhttp.RxHttp;

public class DetailActivity extends BaseActivity<DetailActivity.Data> {


    @BindView(R.id.tv_state)
    TextView mTvState;
    @BindView(R.id.rv_content)
    DPRecyclerView rvContent;

    UserInfo userInfo;
    int mode;

    SimpleRecyclerViewAdapter<DetailInfo> mAdapter;

    @Override
    protected boolean onCheckIntentDataValidate(
            @Nullable @org.jetbrains.annotations.Nullable Data activityData) {
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
        setTitle("用户：" + userInfo.name);
        mTvState.setOnClickListener(v -> requestBalance());
        requestBalance();
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
            rvContent.setAdapter(mAdapter);
        }
        for (DetailInfo data : mAdapter.getDatas()) {
            requestCost(data);
        }
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
                .retryWhen(new RetryHandler(10, 800))
                .observeOn(AndroidSchedulers.mainThread())
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
        RxHttp.get(History.historyOrders(), userInfo)
                .addQuery("symbol", detailInfo.getRequestTradePairs())
                .addQuery("end-time", detailInfo.endTime)
                .asResponseList(HistoryOrderResponse.class)
                .retryWhen(new RetryHandler(10, 500))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new OnResponseListener<List<HistoryOrderResponse>>() {
                    @Override
                    public void onSuccess(@NotNull List<HistoryOrderResponse> historyOrderResponses) {
                        boolean continueRequest;
                        if (ToolUtil.isEmpty(historyOrderResponses)) {
                            continueRequest = true;
                        } else {
                            for (HistoryOrderResponse data : historyOrderResponses) {
                                if (data.type.contains("buy")) {
                                    detailInfo.recordTrades(data.amount, data.price);
                                }
                            }
                            continueRequest = detailInfo.amount < detailInfo.myAmount;
                        }
                        if (continueRequest) {
                            detailInfo.endTime -= TimeUtil.DAY * 2000;
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

    // 获取最新价格
    void requestNewestPrice(DetailInfo detailInfo) {
        if (detailInfo == null) {
            return;
        }
        RxHttp.get(Trade.newestTrade(), userInfo)
                .addQuery("symbol", detailInfo.getRequestTradePairs())
                .asResponseList(TradeResponse.class)
                .retryWhen(new RetryHandler(10, 500))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new OnResponseListener<List<TradeResponse>>() {
                    @Override
                    public void onSuccess(@NotNull List<TradeResponse> tradeResponses) {
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

                    }

                    @Override
                    public void onError(String errorCode, @NotNull Throwable e) {
                        super.onError(errorCode, e);
                        detailInfo.incomeMode = DetailInfo.MODE_ERROR;
                        mAdapter.notifyItemChanged(detailInfo);
                    }
                });
    }

    class Holder extends BaseSimpleRecyclerViewHolder<DetailInfo> {

        @BindView(R.id.tv_currency)
        TextView mTvCurrency;
        @BindView(R.id.tv_refresh)
        TextView mTvRefresh;
        @BindView(R.id.tv_amount)
        TextView mTvAmount;
        @BindView(R.id.tv_cost)
        TextView mTvCost;
        @BindView(R.id.tv_income)
        TextView mTvIncome;

        public Holder(@NonNull @NotNull View itemView) {
            super(itemView);
            ButterKnifeUtil.bind(this, itemView);
            mTvRefresh.setOnClickListener(v -> requestCost(getData()));
        }

        @Override
        public int inflateLayoutResourceId() {
            return R.layout.item_detail;
        }

        @Override
        public void onBindData(DetailInfo data, int position) {
            mTvCurrency.setText(data.tradingPair);
            mTvAmount.setText(NumberUtils.formatDecimal(data.myAmount, 4));
            switch (data.costMode) {
                case DetailInfo.MODE_IDLE:
                    mTvCost.setText(NumberUtils.formatDecimal(data.getAveragePrice(), 8));
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
                    mTvIncome.setText(NumberUtils.formatDecimal(income, 8));
                    break;
                case DetailInfo.MODE_REFRESHING:
                    mTvIncome.setText("正在刷新");
                    break;
                case DetailInfo.MODE_ERROR:
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

        public DetailInfo() {
            amounts = new ArrayList<>();
            prices = new ArrayList<>();
        }

        String getRequestTradePairs() {
            return tradingPair + "usdt";
        }

        public void recordTrades(double amount, double price) {
            amounts.add(amount);
            this.amount += amount;
            prices.add(price);
            this.isChange = true;
        }

        double getAveragePrice() {
            if (isChange || cacheAveragePrice == 0) {
                double all = 0;
                for (int i = 0; i < prices.size(); i++) {
                    all += prices.get(i) * amounts.get(i);
                }
                try {
                    cacheAveragePrice = all / amount;
                } catch (Exception e) {
                    // pass
                }
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