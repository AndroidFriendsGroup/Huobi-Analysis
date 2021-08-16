package com.razerdp.huobi.analysis.ui.popup;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.razerdp.huobi.analysis.utils.ButterKnifeUtil;
import com.razerdp.huobi_analysis.R;

import butterknife.BindView;
import razerdp.basepopup.BasePopupWindow;
import razerdp.util.animation.AnimationHelper;
import razerdp.util.animation.TranslationConfig;

/**
 * Created by 大灯泡 on 2021/8/16
 * <p>
 * Description：
 */
public class PopupSetting extends BasePopupWindow {
    @BindView(R.id.tv_update)
    TextView tvUpdate;
    @BindView(R.id.tv_clear_cache)
    TextView tvClearCache;

    OnMenuselectListener menuselectListener;


    public PopupSetting(Context context) {
        super(context);
        setContentView(R.layout.popup_setting);
        tvUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (menuselectListener != null) {
                    menuselectListener.onUpdateClicked();
                }
                dismiss();
            }
        });
        tvClearCache.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (menuselectListener != null) {
                    menuselectListener.onClearCacheClicked();
                }
                dismiss();
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View contentView) {
        ButterKnifeUtil.bind(this, contentView);
    }

    @Override
    protected Animation onCreateShowAnimation() {
        return AnimationHelper.asAnimation().withTranslation(TranslationConfig.FROM_TOP)
                .toShow();
    }

    @Override
    protected Animation onCreateDismissAnimation() {
        return AnimationHelper.asAnimation().withTranslation(TranslationConfig.TO_TOP)
                .toDismiss();
    }

    public PopupSetting setMenuselectListener(OnMenuselectListener menuselectListener) {
        this.menuselectListener = menuselectListener;
        return this;
    }

    public interface OnMenuselectListener {
        void onUpdateClicked();

        void onClearCacheClicked();
    }
}
