package com.razerdp.huobi.analysis.ui.activity;

import android.view.View;

import androidx.annotation.Nullable;

import com.razerdp.huobi.analysis.base.baseactivity.BaseActivity;
import com.razerdp.huobi.analysis.entity.UserInfo;
import com.razerdp.huobi.analysis.net.api.account.AccountAssets;
import com.razerdp.huobi.analysis.net.response.account.AssetsResponse;
import com.razerdp.huobi.analysis.net.response.listener.OnResponseListener;
import com.razerdp.huobi_analysis.R;

import org.jetbrains.annotations.NotNull;

import rxhttp.RxHttp;

public class DetailActivity extends BaseActivity<DetailActivity.Data> {

    UserInfo userInfo;

    @Override
    protected boolean onCheckIntentDataValidate(@Nullable @org.jetbrains.annotations.Nullable Data activityData) {
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
        RxHttp.get(AccountAssets.assetsApi(userInfo.apiToken))
                .asClass(AssetsResponse.class)
                .subscribe(new OnResponseListener<AssetsResponse>() {
                    @Override
                    public void onSuccess(@NotNull AssetsResponse assetsResponse) {

                    }
                });

    }

    public static class Data extends BaseActivity.IntentData {
        UserInfo userInfo;

        public Data setUserInfo(UserInfo userInfo) {
            this.userInfo = userInfo;
            return this;
        }
    }

}