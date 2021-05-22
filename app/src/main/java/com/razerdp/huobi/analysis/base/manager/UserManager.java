package com.razerdp.huobi.analysis.base.manager;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.razerdp.huobi.analysis.base.Constants;
import com.razerdp.huobi.analysis.base.interfaces.ExtSimpleCallback;
import com.razerdp.huobi.analysis.base.interfaces.SimpleCallback;
import com.razerdp.huobi.analysis.base.net.NetManager;
import com.razerdp.huobi.analysis.entity.UserInfo;
import com.razerdp.huobi.analysis.net.api.account.AccountAssets;
import com.razerdp.huobi.analysis.net.api.account.AccountInfo;
import com.razerdp.huobi.analysis.net.response.account.AccountResponse;
import com.razerdp.huobi.analysis.net.response.account.AssetsResponse;
import com.razerdp.huobi.analysis.net.response.listener.OnResponseListener;
import com.razerdp.huobi.analysis.utils.SharedPreferencesUtils;
import com.razerdp.huobi.analysis.utils.ToolUtil;
import com.razerdp.huobi.analysis.utils.gson.GsonUtil;
import com.razerdp.huobi.analysis.utils.rx.RxCall;
import com.razerdp.huobi.analysis.utils.rx.RxHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
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
        List<UserInfo> locals = GsonUtil.INSTANCE
                .toArrayList(SharedPreferencesUtils.getString("users", ""), UserInfo.class);
        if (!ToolUtil.isEmpty(locals)) {
            userInfos.addAll(locals);
        }
        return userInfos;
    }

    public void addUser(UserInfo info) {
        if (info != null) {
            if (userInfos.contains(info)) {
                return;
            }
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

    public void removeUser(UserInfo userInfo) {
        if (userInfo == null) {
            return;
        }
        userInfos.remove(userInfo);
        saveAsync();
    }

    public void requestUserAccount(UserInfo userInfo, @Nullable SimpleCallback<UserInfo> cb) {
        if (userInfo == null) {
            callError(cb, 0, "no userinfo");
            return;
        }
        RxHttp.get(AccountInfo.accountInfoApi(), userInfo)
              .sign()
              .asClass(AccountResponse.class)
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(new OnResponseListener<AccountResponse>() {
                  @Override
                  public void onSuccess(@NotNull AccountResponse accountResponse) {
                      List<AccountResponse> responses = accountResponse.data;
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
                  public void onFailed(@NotNull Throwable e) {
                      super.onFailed(e);
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
              .sign()
              .asClass(AssetsResponse.class)
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(new OnResponseListener<AssetsResponse>() {
                  @Override
                  public void onSuccess(@NotNull AssetsResponse accountAssets) {
                      userInfo.assets = accountAssets.data.balance;
                      saveAsync();
                      callSuccess(cb, userInfo);
                  }

                  @Override
                  public void onFailed(@NotNull Throwable e) {
                      super.onFailed(e);
                      callError(cb, e.hashCode(), e.getMessage());
                      RxHelper.delay(5, TimeUnit.SECONDS, data -> requestUserAssets(userInfo, cb));
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
