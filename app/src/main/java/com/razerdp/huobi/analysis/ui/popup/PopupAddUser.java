package com.razerdp.huobi.analysis.ui.popup;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

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
    @BindView(R.id.ed_token)
    EditText edToken;
    @BindView(R.id.tv_paste)
    TextView tvPaste;
    @BindView(R.id.tv_how)
    TextView tvHow;
    @BindView(R.id.tv_ok)
    DPTextView tvOk;


    OnAddUserClickListener onAddUserClickListener;

    public PopupAddUser(Context context) {
        super(context);
        setContentView(R.layout.popup_add_user);
        tvOk.setOnClickListener(v -> {
            if (onAddUserClickListener == null) return;
            String apiToken = edToken.getText().toString().trim();
            if (TextUtils.isEmpty(apiToken)) {
                UIHelper.toast("api token不能为空哦");
                return;
            }
            String nickName = edName.getText().toString().trim();
            if (TextUtils.isEmpty(nickName)) {
                int length = apiToken.length();
                nickName = "****" + apiToken.substring(length - length / 4, length);
            }
            onAddUserClickListener.onAddUserClick(nickName, apiToken);
            edName.setText("");
            edToken.setText("");
        });

        tvPaste.setOnClickListener(v -> edToken.setText(ToolUtil.getDataFromClipboard()));
        tvHow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
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
        void onAddUserClick(String nickName, String apiToken);
    }
}
