package com.razerdp.huobi.analysis.base.manager;

import androidx.annotation.Nullable;

import com.razerdp.huobi.analysis.base.interfaces.SimpleCallback;
import com.razerdp.huobi.analysis.entity.UserInfo;
import com.razerdp.huobi.analysis.net.api.account.AccountInfo;
import com.razerdp.huobi.analysis.net.response.account.AccountResponse;
import com.razerdp.huobi.analysis.net.response.listener.OnResponseListener;
import com.razerdp.huobi.analysis.utils.SharedPreferencesUtils;
import com.razerdp.huobi.analysis.utils.ToolUtil;
import com.razerdp.huobi.analysis.utils.gson.GsonUtil;
import com.razerdp.huobi.analysis.utils.rx.RxHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import rxhttp.RxHttp;

/**
 * Created by 大灯泡 on 2021/5/19
 * <p>
 * Description：
 */
public enum UserManager {
    INSTANCE;

    List<UserInfo> userInfos = new ArrayList<>();

    public List<UserInfo> getUsers() {
        List<UserInfo> locals = GsonUtil.INSTANCE.toArrayList(SharedPreferencesUtils.getString("users", ""), UserInfo.class);
        if (!ToolUtil.isEmpty(locals)) {
            userInfos.addAll(locals);
        }
        return userInfos;
    }

    public void addUser(UserInfo info) {
        if (info != null) {
            userInfos.add(info);
            saveAsync();
        }
    }

    public void save() {
        SharedPreferencesUtils.saveString("users", GsonUtil.INSTANCE.toString(userInfos));
    }

    public void saveAsync() {
        RxHelper.runOnBackground(data -> save());
    }


    public void updateUser(UserInfo userInfo, @Nullable SimpleCallback<UserInfo> cb) {
        if (userInfo == null) return;
        RxHttp.get(AccountInfo.accountInfoApi(), userInfo.apiToken)
                .asClass(AccountResponse.class)
                .subscribe(new OnResponseListener<AccountResponse>() {
                    @Override
                    public void onSuccess(@NotNull AccountResponse accountResponse) {

                    }
                });

    }
}
