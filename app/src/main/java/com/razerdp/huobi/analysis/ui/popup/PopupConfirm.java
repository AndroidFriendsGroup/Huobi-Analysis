package com.razerdp.huobi.analysis.ui.popup;

import com.razerdp.huobi.analysis.ui.widget.DPTextView;
import com.razerdp.huobi.analysis.utils.ButterKnifeUtil;
import com.razerdp.huobi_analysis.R;

import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.widget.TextView;

import androidx.annotation.NonNull;
import butterknife.BindView;
import razerdp.basepopup.BasePopupWindow;
import razerdp.util.animation.AnimationHelper;
import razerdp.util.animation.ScaleConfig;

public class PopupConfirm extends BasePopupWindow {

    @BindView(R.id.tv_title)
    TextView mTvTitle;
    @BindView(R.id.tv_tips)
    TextView mTvTips;
    @BindView(R.id.tv_cancel)
    DPTextView mTvCancel;
    @BindView(R.id.tv_ok)
    DPTextView mTvOk;

    View.OnClickListener mCancelListener;
    View.OnClickListener mOkClickListener;

    public PopupConfirm(Context context) {
        super(context);
        setContentView(R.layout.popup_confirm_box);
        mTvOk.setOnClickListener(v -> {
            dismiss();
            if (mOkClickListener != null) {
                mOkClickListener.onClick(v);
            }
        });
        mTvCancel.setOnClickListener(v -> {
            dismiss();
            if (mCancelListener != null) {
                mCancelListener.onClick(v);
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View contentView) {
        super.onViewCreated(contentView);
        ButterKnifeUtil.bind(this, contentView);
    }

    @Override
    protected Animation onCreateShowAnimation() {
        return AnimationHelper.asAnimation()
                              .withScale(ScaleConfig.CENTER)
                              .toShow();
    }

    @Override
    protected Animation onCreateDismissAnimation() {
        return AnimationHelper.asAnimation()
                              .withScale(ScaleConfig.CENTER)
                              .toDismiss();
    }

    public PopupConfirm setTitle(CharSequence title) {
        mTvTitle.setText(title);
        return this;
    }

    public PopupConfirm setTips(CharSequence tips) {
        mTvTips.setText(tips);
        return this;
    }

    public PopupConfirm setOKText(CharSequence okText) {
        mTvOk.setText(okText);
        return this;
    }

    public PopupConfirm setCancelText(CharSequence cancelText) {
        mTvCancel.setText(cancelText);
        return this;
    }

    public PopupConfirm setOKClickListener(View.OnClickListener l) {
        this.mOkClickListener = l;
        return this;
    }

    public PopupConfirm setCancelClickListener(View.OnClickListener l) {
        this.mCancelListener = l;
        return this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mOkClickListener = null;
        this.mCancelListener = null;
    }
}
