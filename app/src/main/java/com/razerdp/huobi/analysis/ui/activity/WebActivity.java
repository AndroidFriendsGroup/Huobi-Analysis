package com.razerdp.huobi.analysis.ui.activity;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.just.agentweb.AgentWeb;
import com.just.agentweb.WebChromeClient;
import com.razerdp.huobi.analysis.base.baseactivity.BaseActivity;
import com.razerdp.huobi.analysis.utils.UIHelper;
import com.razerdp.huobi_analysis.R;

import butterknife.BindView;

public class WebActivity extends BaseActivity<WebActivity.Data> {

    @BindView(R.id.web_view_container)
    FrameLayout mWebViewContainer;
    AgentWeb mAgentWeb;

    String url;

    @Override
    public int contentViewLayoutId() {
        return R.layout.activity_web;
    }

    @Override
    protected boolean onCheckIntentDataValidate(@Nullable WebActivity.Data activityData) {
        if (activityData == null || TextUtils.isEmpty(activityData.url)) {
            return false;
        }
        url = activityData.url;
        return true;
    }

    @Override
    protected void onInitView(View decorView) {
        mAgentWeb = AgentWeb.with(this)
                .setAgentWebParent(mWebViewContainer, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                .useDefaultIndicator(UIHelper.getColor(R.color.common_blue))
                .setWebChromeClient(mWebChromeClient)
                .createAgentWeb()
                .ready()
                .go(url);
        mAgentWeb.getAgentWebSettings().getWebSettings().setUseWideViewPort(true);
        mAgentWeb.getAgentWebSettings().getWebSettings().setLoadWithOverviewMode(true);
    }


    @Override
    public void onTitleLeftClick(View view) {
        if (!mAgentWeb.back()) {
            super.onTitleLeftClick(view);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAgentWeb.getWebLifeCycle().onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mAgentWeb.getWebLifeCycle().onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAgentWeb.getWebLifeCycle().onDestroy();
    }

    private WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            setTitle(title);
        }
    };

    public static class Data extends BaseActivity.IntentData {

        String url;

        public Data setUrl(String url) {
            this.url = url;
            return this;
        }
    }
}