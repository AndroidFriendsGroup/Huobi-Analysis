package com.razerdp.huobi.analysis.ui.activity;

import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pgyersdk.update.DownloadFileListener;
import com.pgyersdk.update.PgyUpdateManager;
import com.pgyersdk.update.UpdateManagerListener;
import com.pgyersdk.update.javabean.AppBean;
import com.razerdp.huobi.analysis.base.baseactivity.BaseActivity;
import com.razerdp.huobi.analysis.base.baseadapter.BaseSimpleRecyclerViewHolder;
import com.razerdp.huobi.analysis.base.baseadapter.SimpleRecyclerViewAdapter;
import com.razerdp.huobi.analysis.base.interfaces.ExtSimpleCallback;
import com.razerdp.huobi.analysis.base.manager.UserManager;
import com.razerdp.huobi.analysis.entity.UserInfo;
import com.razerdp.huobi.analysis.ui.ActivityLauncher;
import com.razerdp.huobi.analysis.ui.popup.PopupAddUser;
import com.razerdp.huobi.analysis.ui.popup.PopupConfirm;
import com.razerdp.huobi.analysis.ui.popup.PopupUpdate;
import com.razerdp.huobi.analysis.ui.widget.DPRecyclerView;
import com.razerdp.huobi.analysis.ui.widget.DPTextView;
import com.razerdp.huobi.analysis.utils.ButterKnifeUtil;
import com.razerdp.huobi.analysis.utils.SpanUtil;
import com.razerdp.huobi.analysis.utils.StringUtil;
import com.razerdp.huobi.analysis.utils.UIHelper;
import com.razerdp.huobi.analysis.utils.ViewUtil;
import com.razerdp.huobi.analysis.utils.rx.RxHelper;
import com.razerdp.huobi_analysis.R;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import io.reactivex.disposables.Disposable;

public class MainActivity extends BaseActivity {

    @BindView(R.id.rv_content)
    DPRecyclerView rvContent;
    SimpleRecyclerViewAdapter<UserInfo> mAdapter;

    PopupAddUser popupAddUser;
    PopupConfirm mPopupConfirm;
    PopupUpdate mPopupUpdate;

    Map<String, Disposable> disposableMap;

    @Override
    public int contentViewLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void onInitView(View decorView) {
        disposableMap = new HashMap<>();
        mAdapter = new SimpleRecyclerViewAdapter<>(this, UserManager.INSTANCE.getUsers());
        mAdapter.setHolder(Holder.class);
        mAdapter.outher(this);
        rvContent.setLayoutManager(new LinearLayoutManager(this));
        rvContent.addFooterView(inflateFooterView());
        rvContent.setItemAnimator(null);
        mAdapter.setOnItemClickListener(
                (v, position, data) -> ActivityLauncher.toDetail(self(), data));
        mAdapter.setOnItemLongClickListener((v, position, data) -> {
            showDelUserPopup(data);
            return true;
        });
        rvContent.setAdapter(mAdapter);
        refreshAccountAssets();
        checkForUpdate();
    }

    @Override
    public void onTitleRightClick(View view) {
        checkForUpdate();
    }

    void refreshAccountAssets() {
        if (mAdapter == null) {
            return;
        }
        for (UserInfo data : mAdapter.getDatas()) {
            requestAccountAssets(data);
        }
    }

    View inflateFooterView() {
        View footer = ViewUtil.inflate(this, R.layout.item_footer_add, rvContent, false);
        footer.setOnClickListener(v -> showAddUserPopup());
        return footer;
    }

    void showAddUserPopup() {
        if (popupAddUser == null) {
            popupAddUser = new PopupAddUser(this);
            popupAddUser.setOnAddUserClickListener((nickName, accetKey, secretKey) -> {
                UserInfo userInfo = new UserInfo(accetKey, secretKey);
                userInfo.name = nickName;
                userInfo.assets = "0";
                UserManager.INSTANCE.addUser(userInfo);
                mAdapter.addData(userInfo);
                requestUserAccount(userInfo);
            });
        }
        popupAddUser.showPopupWindow();
    }

    void showDelUserPopup(UserInfo userInfo) {
        if (mPopupConfirm == null) {
            mPopupConfirm = new PopupConfirm(this);
            mPopupConfirm.setTitle("删除用户");
            mPopupConfirm.setOKClickListener(v -> {
                UIHelper.toast("删除成功");
                UserManager.INSTANCE.removeUser(userInfo);
                mAdapter.remove(userInfo);
            });
        }
        mPopupConfirm.setTips(SpanUtil.create("确定删除用户：" + userInfo.name + "吗？")
                .append(userInfo.name)
                .setTextColor(UIHelper.getColor(R.color.common_red_light))
                .setTextStyle(Typeface.DEFAULT_BOLD)
                .getSpannableStringBuilder());
        mPopupConfirm.showPopupWindow();
    }

    void requestUserAccount(UserInfo userInfo) {
        if (userInfo == null) {
            return;
        }
        userInfo.isRefreshing = true;
        mAdapter.notifyDataSetChanged();
        UserManager.INSTANCE.requestUserAccount(userInfo, new ExtSimpleCallback<UserInfo>() {
            @Override
            public void onCall(UserInfo data) {
                userInfo.isRefreshing = false;
                mAdapter.notifyDataSetChanged();
                requestAccountAssets(userInfo);
            }

            @Override
            public void onError(int code, String errorMessage) {
                super.onError(code, errorMessage);
                userInfo.isRefreshing = false;
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    void requestAccountAssets(UserInfo userInfo) {
        if (userInfo == null) {
            return;
        }
        UserManager.INSTANCE.requestUserAssets(userInfo, new ExtSimpleCallback<UserInfo>() {
            @Override
            public void onCall(UserInfo data) {
                mAdapter.notifyItemChanged(data);
                if (disposableMap.get(userInfo.accetKey) == null) {
                    disposableMap.put(userInfo.accetKey, RxHelper.loop(5000, 5000, data1 -> requestAccountAssets(userInfo)));
                }
            }
        });

    }


    private void checkForUpdate() {
        new PgyUpdateManager.Builder()
                .setUpdateManagerListener(new UpdateManagerListener() {
                    @Override
                    public void onNoUpdateAvailable() {
                        UIHelper.toast("已经是最新版");
                    }

                    @Override
                    public void onUpdateAvailable(AppBean appBean) {
                        if (appBean == null) {
                            UIHelper.toast("已经是最新版");
                            return;
                        }
                        if (mPopupUpdate == null) {
                            mPopupUpdate = new PopupUpdate(self());
                        }
                        mPopupUpdate.reset();
                        mPopupUpdate.showPopupWindow(appBean);
                    }

                    @Override
                    public void checkUpdateFailed(Exception e) {
                        UIHelper.toast("已经是最新版");
                    }
                })
                .setDownloadFileListener(new DownloadFileListener() {
                    @Override
                    public void downloadFailed() {
                        //下载失败
                        if (mPopupUpdate != null) {
                            mPopupUpdate.onError();
                        }
                    }

                    @Override
                    public void downloadSuccessful(File file) {
                        if (mPopupUpdate != null) {
                            mPopupUpdate.dismiss(false);
                        }
                        PgyUpdateManager.installApk(file);
                    }

                    @Override
                    public void onProgressUpdate(Integer... integers) {
                        if (mPopupUpdate != null) {
                            mPopupUpdate.onProgress(integers[0]);
                        }
                    }
                })
                .register();
    }

    @Override
    protected void onDestroy() {
        for (Disposable value : disposableMap.values()) {
            value.dispose();
        }
        super.onDestroy();
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
        @BindView(R.id.layout_name)
        View layoutName;

        public Holder(@NonNull @NotNull View itemView) {
            super(itemView);
            ButterKnifeUtil.bind(this, itemView);
            tvNoAccountId.setOnClickListener(v -> requestUserAccount(getData()));
            layoutName.setOnClickListener(v -> {
                if (getData().accountId != 0) {
                    requestAccountAssets(getData());
                } else {
                    requestUserAccount(getData());
                }
            });
        }

        @Override
        public int inflateLayoutResourceId() {
            return R.layout.item_user;
        }

        @Override
        public void onBindData(UserInfo data, int position) {
            tvName.setText(data.name);
            tvToken.setText(StringUtil.subApiToken(data.accetKey));
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
                tvAssets.setText(String.format("CNY: %S", data.assets));
            }
        }
    }
}