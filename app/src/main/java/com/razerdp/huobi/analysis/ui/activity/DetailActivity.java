package com.razerdp.huobi.analysis.ui.activity;

import com.razerdp.huobi.analysis.base.baseactivity.BaseActivity;
import com.razerdp.huobi.analysis.base.baseadapter.BaseSimpleRecyclerViewHolder;
import com.razerdp.huobi.analysis.base.baseadapter.SimpleRecyclerViewAdapter;
import com.razerdp.huobi.analysis.entity.UserInfo;
import com.razerdp.huobi.analysis.net.api.account.AccountAssets;
import com.razerdp.huobi.analysis.net.api.order.History;
import com.razerdp.huobi.analysis.net.response.account.BalanceResponse;
import com.razerdp.huobi.analysis.net.response.listener.OnResponseListener;
import com.razerdp.huobi.analysis.net.response.order.HistoryOrderResponse;
import com.razerdp.huobi.analysis.ui.widget.DPRecyclerView;
import com.razerdp.huobi.analysis.utils.ButterKnifeUtil;
import com.razerdp.huobi.analysis.utils.NumberUtils;
import com.razerdp.huobi.analysis.utils.TimeUtil;
import com.razerdp.huobi.analysis.utils.ToolUtil;
import com.razerdp.huobi.analysis.utils.log.HLog;
import com.razerdp.huobi.analysis.utils.rx.RxHelper;
import com.razerdp.huobi_analysis.R;

import org.jetbrains.annotations.NotNull;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import butterknife.BindView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import rxhttp.RxHttp;

public class DetailActivity extends BaseActivity<DetailActivity.Data> {

    public static final int MODE_BALANCE = 1;

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
        mTvState.setOnClickListener(v -> {
            switch (mode) {
                case MODE_BALANCE:
                    requestBalance();
                    break;
            }
        });
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
        mTvState.setText("正在获取现货余额...");
        RxHttp.get(AccountAssets.balanceApi(userInfo.accountId), userInfo)
              .sign()
              .asClass(BalanceResponse.class)
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(new OnResponseListener<BalanceResponse>() {
                  @Override
                  public void onSuccess(@NotNull BalanceResponse balanceResponse) {
                      balanceResponse.data.fillInUser(userInfo);
                      initRvContent();
                  }

                  @Override
                  public void onError(String errorCode, @NotNull Throwable e) {
                      super.onError(errorCode, e);
                      setMode(MODE_BALANCE);
                      mTvState.setText("获取失败，点击重新获取");
                  }
              });
    }

    // 窗口只有48h。。。所以需要一直往前推
    void requestCost(DetailInfo detailInfo) {
        if (detailInfo == null) {
            return;
        }
        if (!detailInfo.isRefreshingCost) {
            detailInfo.isRefreshingCost = true;
            if (detailInfo.index == -1) {
                detailInfo.index = mAdapter.getDatas().indexOf(detailInfo);
            }
            mAdapter.notifyItemChanged(detailInfo.index);
        }
        if (detailInfo.endTime == 0) {
            detailInfo.endTime = System.currentTimeMillis();
        }
        HLog.i("matchresults", TimeUtil.longToTimeStr(detailInfo.endTime, TimeUtil.YYYYMMDDHHMMSS));
        RxHttp.get(History.historyOrders(), userInfo)
              .addQuery("symbol", detailInfo.getRequestTradePairs())
              .addQuery("end-time", detailInfo.endTime)
              .sign()
              .asClass(HistoryOrderResponse.class)
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(new OnResponseListener<HistoryOrderResponse>() {
                  @Override
                  public void onSuccess(@NotNull HistoryOrderResponse historyOrderResponse) {
                      List<HistoryOrderResponse> datas = historyOrderResponse.data;
                      boolean continueRequest;
                      if (ToolUtil.isEmpty(datas)) {
                          continueRequest = true;
                      } else {
                          for (HistoryOrderResponse data : datas) {
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
                          detailInfo.isRefreshingCost = false;
                          mAdapter.notifyItemChanged(detailInfo.index);
                      }
                  }

                  @Override
                  public void onError(String errorCode, @NotNull Throwable e) {
                      super.onError(errorCode, e);
                      if (TextUtils.equals(errorCode, "api-signature-not-valid")) {
                          RxHelper.delay(500, data -> requestCost(detailInfo));
                      }
                  }
              });

    }

    void setMode(int mode) {
        this.mode = mode;
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
            data.index = position;
            mTvCurrency.setText(data.tradingPair);
            mTvAmount.setText(NumberUtils.formatDecimal(data.myAmount, 4));
            if (data.isRefreshingCost) {
                mTvCost.setText("正在刷新");
            } else {
                mTvCost.setText(NumberUtils.formatDecimal(data.getAveragePrice(),8));
            }
            if (data.isRefreshingIncome) {
                mTvIncome.setText("正在刷新");
            }
        }

    }

    static class DetailInfo {

        String tradingPair;
        double myAmount;

        boolean isRefreshingCost;
        boolean isRefreshingIncome;

        // inner
        List<Double> amounts;
        List<Double> prices;
        double amount;
        long endTime;
        int index = -1;
        boolean isChange;
        double cacheAveragePrice;

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
    }

    public static class Data extends BaseActivity.IntentData {

        UserInfo userInfo;

        public Data setUserInfo(UserInfo userInfo) {
            this.userInfo = userInfo;
            return this;
        }
    }

    private Comparator<DetailInfo> mComparator = (o1, o2) -> -Double.compare(o1.myAmount, o2.myAmount);

}