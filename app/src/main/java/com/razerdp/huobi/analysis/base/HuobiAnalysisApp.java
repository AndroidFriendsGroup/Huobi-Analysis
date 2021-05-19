package com.razerdp.huobi.analysis.base;

import android.app.Application;
import android.content.Context;

/**
 * Created by 大灯泡 on 2018/12/4.
 */
public class HuobiAnalysisApp extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        AppContext.init(this);
        super.attachBaseContext(base);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
