package com.razerdp.huobi.analysis.ui.activity;

import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.razerdp.huobi.analysis.base.baseactivity.BaseActivity;
import com.razerdp.huobi.analysis.base.baseadapter.BaseSimpleRecyclerViewHolder;
import com.razerdp.huobi.analysis.base.baseadapter.SimpleRecyclerViewAdapter;
import com.razerdp.huobi.analysis.base.manager.UserManager;
import com.razerdp.huobi.analysis.entity.UserInfo;
import com.razerdp.huobi.analysis.ui.popup.PopupAddUser;
import com.razerdp.huobi.analysis.ui.widget.DPRecyclerView;
import com.razerdp.huobi.analysis.utils.NumberUtils;
import com.razerdp.huobi.analysis.utils.StringUtil;
import com.razerdp.huobi.analysis.utils.ViewUtil;
import com.razerdp.huobi_analysis.R;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;

public class MainActivity extends BaseActivity {

    @BindView(R.id.rv_content)
    DPRecyclerView rvContent;
    SimpleRecyclerViewAdapter<UserInfo> mAdapter;

    PopupAddUser popupAddUser;

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    @Override
    public int contentViewLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onInitView(View decorView) {
        mAdapter = new SimpleRecyclerViewAdapter<>(this, UserManager.INSTANCE.getUsers());
        mAdapter.setHolder(Holder.class);
        rvContent.setLayoutManager(new LinearLayoutManager(this));
        rvContent.addFooterView(inflateFooterView());
        rvContent.setAdapter(mAdapter);
    }


    View inflateFooterView() {
        View footer = ViewUtil.inflate(this, R.layout.item_footer_add, rvContent, false);
        footer.setOnClickListener(v -> showAddUserPopup());
        return footer;
    }

    void showAddUserPopup() {
        if (popupAddUser == null) {
            popupAddUser = new PopupAddUser(this);
            popupAddUser.setOnAddUserClickListener((nickName, apiToken) -> {
                UserInfo userInfo = new UserInfo();
                userInfo.name = nickName;
                userInfo.apiToken = apiToken;
                userInfo.assets = 0;
                UserManager.INSTANCE.addUser(userInfo);
                mAdapter.updateData(UserManager.INSTANCE.getUsers());
            });
        }
        popupAddUser.showPopupWindow();
    }


    static class Holder extends BaseSimpleRecyclerViewHolder<UserInfo> {

        @BindView(R.id.tv_name)
        TextView tvName;
        @BindView(R.id.tv_token)
        TextView tvToken;
        @BindView(R.id.tv_assets)
        TextView tvAssets;
        @BindView(R.id.iv_arrow)
        ImageView ivArrow;

        public Holder(@NonNull @NotNull View itemView) {
            super(itemView);
        }

        @Override
        public int inflateLayoutResourceId() {
            return R.layout.item_user;
        }

        @Override
        public void onBindData(UserInfo data, int position) {
            tvName.setText(data.name);
            tvToken.setText(StringUtil.subApiToken(data.apiToken));
            tvAssets.setText(NumberUtils.formatDecimal(data.assets, 4));
        }
    }
}