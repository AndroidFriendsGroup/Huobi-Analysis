package com.razerdp.huobi.analysis.base.manager;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.razerdp.huobi.analysis.base.Constants;
import com.razerdp.huobi.analysis.base.interfaces.ExtSimpleCallback;
import com.razerdp.huobi.analysis.base.interfaces.SimpleCallback;
import com.razerdp.huobi.analysis.base.net.listener.OnResponseListener;
import com.razerdp.huobi.analysis.base.net.retry.RetryHandler;
import com.razerdp.huobi.analysis.entity.UserInfo;
import com.razerdp.huobi.analysis.net.api.account.AccountAssets;
import com.razerdp.huobi.analysis.net.api.account.AccountInfo;
import com.razerdp.huobi.analysis.net.response.account.AccountResponse;
import com.razerdp.huobi.analysis.net.response.account.AssetsResponse;
import com.razerdp.huobi.analysis.utils.SharedPreferencesUtils;
import com.razerdp.huobi.analysis.utils.StringUtil;
import com.razerdp.huobi.analysis.utils.ToolUtil;
import com.razerdp.huobi.analysis.utils.gson.GsonUtil;
import com.razerdp.huobi.analysis.utils.rx.RxHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import rxhttp.RxHttp;

/**
 * Created by 大灯泡 on 2021/5/19
 * <p>
 * Description：
 */
public enum UserManager {
    INSTANCE;

    Map<String, UserInfo> usersMap = new HashMap<>();

    public List<UserInfo> getUsers() {
        String usersOld = SharedPreferencesUtils.getString("users", "");
        String usersNew = SharedPreferencesUtils.getString("users_new", "");
        if (StringUtil.noEmpty(usersOld)) {
            List<UserInfo> userList = GsonUtil.INSTANCE.toArrayList(usersOld, UserInfo.class);
            if (!ToolUtil.isEmpty(userList)) {
                for (UserInfo userInfo : userList) {
                    usersMap.put(userInfo.accetKey, userInfo);
                }
            }
            SharedPreferencesUtils.remove("users");
        } else if (StringUtil.noEmpty(usersNew)) {
            Map<String, UserInfo> userMap = GsonUtil.INSTANCE.toHashMap(usersNew, String.class, UserInfo.class);
            if (userMap != null) {
                usersMap.putAll(userMap);
            }
        }
        return new ArrayList<>(usersMap.values());
    }

    public void addUser(UserInfo info) {
        if (info != null) {
            usersMap.put(info.accetKey, info);
            saveAsync();
        }
    }

    public void save() {
        SharedPreferencesUtils.saveString("users_new", GsonUtil.INSTANCE.toString(usersMap));
    }

    public void saveAsync() {
        RxHelper.runOnBackground(data -> save());
    }

    public void removeUser(UserInfo userInfo) {
        if (userInfo == null) {
            return;
        }
        usersMap.remove(userInfo.accetKey);
        saveAsync();
    }

    public void requestUserAccount(UserInfo userInfo, @Nullable SimpleCallback<UserInfo> cb) {
        if (userInfo == null) {
            callError(cb, 0, "no userinfo");
            return;
        }
        RxHttp.get(AccountInfo.accountInfoApi(), userInfo)
                .asResponseList(AccountResponse.class)
                .retryWhen(new RetryHandler())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new OnResponseListener<List<AccountResponse>>() {
                    @Override
                    public void onSuccess(@NotNull List<AccountResponse> responses) {
                        for (AccountResponse data : responses) {
                            if (TextUtils.equals(data.type, Constants.AccountType.SPOT)) {
                                userInfo.accountId = data.accountid;
                                save();
                                callSuccess(cb, userInfo);
                                break;
                            }
                        }
                    }

                    @Override
                    public void onError(String errorCode, @NotNull Throwable e) {
                        super.onError(errorCode, e);
                        callError(cb, e.hashCode(), e.getMessage());
                    }
                });
    }

    public void requestUserAssets(UserInfo userInfo, @Nullable SimpleCallback<UserInfo> cb) {
        if (userInfo == null || userInfo.accountId == 0) {
            callError(cb, 0, "no user or no accountid");
            return;
        }
        RxHttp.get(AccountAssets.assetsApi(), userInfo)
                .addQuery("accountType", Constants.AccountType.SPOT)
                .addQuery("valuationCurrency", "CNY")
                .asResponse(AssetsResponse.class)
                .retryWhen(new RetryHandler())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new OnResponseListener<AssetsResponse>() {
                    @Override
                    public void onSuccess(@NotNull AssetsResponse accountAssets) {
                        userInfo.assets = accountAssets.balance;
                        saveAsync();
                        callSuccess(cb, userInfo);
                    }

                    @Override
                    public void onError(String errorCode, @NotNull Throwable e) {
                        super.onError(errorCode, e);
                        callError(cb, e.hashCode(), e.getMessage());
                    }
                });
    }

    <T> void callSuccess(SimpleCallback<T> cb, T t) {
        if (cb != null) {
            cb.onCall(t);
        }
    }

    void callError(SimpleCallback cb, int code, String errorMessage) {
        if (cb instanceof ExtSimpleCallback) {
            ((ExtSimpleCallback<?>) cb).onError(code, errorMessage);
        }
    }

}
