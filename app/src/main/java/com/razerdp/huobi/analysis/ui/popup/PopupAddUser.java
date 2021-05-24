package com.razerdp.huobi.analysis.ui.popup;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.razerdp.huobi.analysis.ui.ActivityLauncher;
import com.razerdp.huobi.analysis.ui.activity.GuideActivity;
import com.razerdp.huobi.analysis.ui.widget.DPTextView;
import com.razerdp.huobi.analysis.utils.ButterKnifeUtil;
import com.razerdp.huobi.analysis.utils.ToolUtil;
import com.razerdp.huobi.analysis.utils.UIHelper;
import com.razerdp.huobi_analysis.R;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import razerdp.basepopup.BasePopupWindow;

/**
 * Created by 大灯泡 on 2021/5/19
 * <p>
 * Description：
 */
public class PopupAddUser extends BasePopupWindow {

    @BindView(R.id.ed_name)
    EditText edName;
    @BindView(R.id.ed_acckey)
    EditText edAccetKey;
    @BindView(R.id.tv_acckey_paste)
    TextView tvAccetKeyPaste;
    @BindView(R.id.ed_secretkey)
    EditText edSecretKey;
    @BindView(R.id.tv_secret_paste)
    TextView tvSecretKeyPaste;
    @BindView(R.id.tv_how)
    TextView tvHow;
    @BindView(R.id.tv_ok)
    DPTextView tvOk;

    OnAddUserClickListener onAddUserClickListener;

    public PopupAddUser(Context context) {
        super(context);
        setContentView(R.layout.popup_add_user);
        tvOk.setOnClickListener(v -> {
            if (onAddUserClickListener == null) {
                return;
            }
            String accetkey = edAccetKey.getText().toString().trim();
            if (TextUtils.isEmpty(accetkey)) {
                UIHelper.toast("accetkey不能为空哦");
                return;
            }
            String secretKey = edSecretKey.getText().toString().trim();
            if (TextUtils.isEmpty(secretKey)) {
                UIHelper.toast("secretKey不能为空哦");
                return;
            }
            String nickName = edName.getText().toString().trim();
            if (TextUtils.isEmpty(nickName)) {
                int length = accetkey.length();
                nickName = "****" + accetkey.substring(length - length / 4, length);
            }
            onAddUserClickListener.onAddUserClick(nickName, accetkey, secretKey);
            dismiss();
            edName.setText("");
            edAccetKey.setText("");
        });
        tvAccetKeyPaste
                .setOnClickListener(v -> {
                    edAccetKey.setText(ToolUtil.getDataFromClipboard());
                    edAccetKey.setSelection(edAccetKey.length());

                });
        tvSecretKeyPaste.setOnClickListener(
                v -> {
                    edSecretKey.setText(ToolUtil.getDataFromClipboard());
                    edSecretKey.setSelection(edSecretKey.length());
                });
        tvHow.setOnClickListener(v -> {
            ActivityLauncher.start(getContext(), GuideActivity.class);
            dismiss();
        });
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View contentView) {
        super.onViewCreated(contentView);
        ButterKnifeUtil.bind(this, contentView);
    }

    public PopupAddUser setOnAddUserClickListener(OnAddUserClickListener onAddUserClickListener) {
        this.onAddUserClickListener = onAddUserClickListener;
        return this;
    }

    public interface OnAddUserClickListener {

        void onAddUserClick(String nickName, String accetKey, String secretKey);
    }
}
