package com.razerdp.huobi.analysis.ui.activity;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.razerdp.huobi.analysis.base.baseactivity.BaseActivity;
import com.razerdp.huobi.analysis.base.baseadapter.BaseSimpleRecyclerViewHolder;
import com.razerdp.huobi.analysis.base.baseadapter.SimpleRecyclerViewAdapter;
import com.razerdp.huobi.analysis.base.interfaces.SimpleCallback;
import com.razerdp.huobi.analysis.base.manager.UserManager;
import com.razerdp.huobi.analysis.entity.UserInfo;
import com.razerdp.huobi.analysis.ui.ActivityLauncher;
import com.razerdp.huobi.analysis.ui.popup.PopupAddUser;
import com.razerdp.huobi.analysis.ui.widget.DPRecyclerView;
import com.razerdp.huobi.analysis.ui.widget.DPTextView;
import com.razerdp.huobi.analysis.utils.ButterKnifeUtil;
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
    public int contentViewLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onInitView(View decorView) {
        mAdapter = new SimpleRecyclerViewAdapter<>(this, UserManager.INSTANCE.getUsers());
        mAdapter.setHolder(Holder.class);
        mAdapter.outher(this);
        rvContent.setLayoutManager(new LinearLayoutManager(this));
        rvContent.addFooterView(inflateFooterView());
        mAdapter.setOnItemClickListener((v, position, data) -> ActivityLauncher.toDetail(self(), data));
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
                updateUserinfo(userInfo);
                mAdapter.updateData(UserManager.INSTANCE.getUsers());
            });
        }
        popupAddUser.showPopupWindow();
    }


    void updateUserinfo(UserInfo userInfo) {
        if (userInfo == null) return;
        userInfo.isRefreshing = true;
        mAdapter.notifyDataSetChanged();
        UserManager.INSTANCE.updateUser(userInfo, new SimpleCallback<UserInfo>() {
            @Override
            public void onCall(UserInfo data) {
                userInfo.isRefreshing = false;
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    class Holder extends BaseSimpleRecyclerViewHolder<UserInfo> {

        @BindView(R.id.tv_name)
        TextView tvName;
        @BindView(R.id.tv_token)
        TextView tvToken;
        @BindView(R.id.tv_assets)
        TextView tvAssets;
        @BindView(R.id.tv_no_accountid)
        DPTextView tvNoAccountId;
        @BindView(R.id.tv_refreshing)
        TextView tvRefreshing;

        public Holder(@NonNull @NotNull View itemView) {
            super(itemView);
            ButterKnifeUtil.bind(this, itemView);
            tvNoAccountId.setOnClickListener(v -> updateUserinfo(getData()));
        }

        @Override
        public int inflateLayoutResourceId() {
            return R.layout.item_user;
        }

        @Override
        public void onBindData(UserInfo data, int position) {
            tvName.setText(data.name);
            tvToken.setText(StringUtil.subApiToken(data.apiToken));
            if (data.accountId == 0) {
                tvAssets.setVisibility(View.GONE);
                if (data.isRefreshing) {
                    tvNoAccountId.setVisibility(View.GONE);
                    tvRefreshing.setVisibility(View.VISIBLE);
                } else {
                    tvNoAccountId.setVisibility(View.VISIBLE);
                    tvRefreshing.setVisibility(View.GONE);
                }
            } else {
                tvNoAccountId.setVisibility(View.GONE);
                tvRefreshing.setVisibility(View.GONE);
                tvAssets.setVisibility(View.VISIBLE);
                tvAssets.setText(NumberUtils.formatDecimal(data.assets, 4));
            }
        }
    }
}