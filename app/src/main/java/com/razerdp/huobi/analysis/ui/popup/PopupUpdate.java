package com.razerdp.huobi.analysis.ui.popup;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.pgyersdk.update.PgyUpdateManager;
import com.pgyersdk.update.javabean.AppBean;
import com.razerdp.huobi.analysis.ui.widget.DPTextView;
import com.razerdp.huobi_analysis.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import razerdp.basepopup.BasePopupWindow;
import razerdp.util.animation.AlphaConfig;
import razerdp.util.animation.AnimationHelper;

/**
 * Created by 大灯泡 on 2020/3/4.
 */
public class PopupUpdate extends BasePopupWindow {
    @BindView(R.id.tv_title)
    TextView mTvTitle;
    @BindView(R.id.tv_content)
    TextView mTvContent;
    @BindView(R.id.tv_ignore)
    DPTextView mTvIgnore;
    @BindView(R.id.tv_update)
    DPTextView mTvUpdate;
    @BindView(R.id.layout_controller)
    View controller;
    @BindView(R.id.progress)
    ProgressBar mProgressBar;

    AppBean mAppBean;

    public PopupUpdate(Context context) {
        super(context);
        setContentView(R.layout.popup_update);
        setOutSideDismiss(false);
        setBackPressEnable(false);
        setBlurBackgroundEnable(true);
    }

    @Override
    public void onViewCreated(View contentView) {
        super.onViewCreated(contentView);
        ButterKnife.bind(this, contentView);
    }


    @Override
    protected Animation onCreateShowAnimation() {
       return AnimationHelper.asAnimation()
               .withAlpha(AlphaConfig.IN)
               .toShow();
    }

    @Override
    protected Animation onCreateDismissAnimation() {
        return AnimationHelper.asAnimation()
                .withAlpha(AlphaConfig.OUT)
                .toDismiss();
    }

    public void showPopupWindow(AppBean appBean) {
        if (appBean == null) return;
        this.mAppBean = appBean;
        mTvTitle.setText(String.format("发现新版本：【%s】", appBean.getVersionName()));
        mTvContent.setText(appBean.getReleaseNote());
        showPopupWindow();
    }

    @OnClick(R.id.tv_ignore)
    void ignore() {
        dismiss();
    }

    @OnClick(R.id.tv_update)
    void download() {
        if (mAppBean == null) return;
        PgyUpdateManager.downLoadApk(mAppBean.getDownloadURL());
    }

    public void reset(){
        if (controller.getVisibility() != View.VISIBLE) {
            controller.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(0);
            mProgressBar.setVisibility(View.GONE);
        }
        mTvUpdate.setText("立即升级");
    }

    public void onProgress(int progress) {
        if (controller.getVisibility() == View.VISIBLE) {
            controller.setVisibility(View.GONE);
            mProgressBar.setProgress(0);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        mProgressBar.setProgress(progress);
    }

    public void onError() {
        if (controller.getVisibility() != View.VISIBLE) {
            controller.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(0);
            mProgressBar.setVisibility(View.GONE);
        }
        mTvUpdate.setText("重新下载");
    }

}
