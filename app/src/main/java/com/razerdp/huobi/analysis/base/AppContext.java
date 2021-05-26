package com.razerdp.huobi.analysis.base;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.razerdp.huobi.analysis.base.livedata.HLiveData;

import java.lang.ref.WeakReference;

/**
 * Created by 大灯泡 on 2017/3/22.
 * <p>
 * baselib appcontext类
 * <p>
 */

public class AppContext {
    private static final String TAG = "AppContext";
    public static Application sApplication;
    private static InnerLifecycleHandler INNER_LIFECYCLE_HANDLER;
    private static HLiveData<Boolean> foreGroundAndBackgroundChangeLiveData;

    public static void init(Application app) {
        if (sApplication != null) return;
        sApplication = app;
        INNER_LIFECYCLE_HANDLER = new InnerLifecycleHandler();
        if (sApplication != null) {
            sApplication.registerActivityLifecycleCallbacks(INNER_LIFECYCLE_HANDLER);
        }
    }

    public static boolean isAppForeGround() {
        return INNER_LIFECYCLE_HANDLER != null && INNER_LIFECYCLE_HANDLER.hasVisibleActivities;
    }

    public static boolean isAppBackground() {
        return INNER_LIFECYCLE_HANDLER != null && !INNER_LIFECYCLE_HANDLER.hasVisibleActivities;
    }

    private static void checkAppContext() {
        if (sApplication == null) {
            reflectAppContext();
        }
        if (sApplication == null) {
            throw new IllegalStateException("app reference is null");
        }
    }

    public static void registerForeGroundChange(LifecycleOwner owner, Observer<Boolean> ob) {
        getForeGroundAndBackgroundChangeLiveData().observe(owner, ob);
    }

    public static void registerForeGroundChangeForever(Observer<Boolean> ob) {
        getForeGroundAndBackgroundChangeLiveData().observeForever(ob);
    }

    public static void unregisterForeGroundChange(LifecycleOwner owner) {
        getForeGroundAndBackgroundChangeLiveData().removeObservers(owner);
    }

    private static HLiveData<Boolean> getForeGroundAndBackgroundChangeLiveData() {
        if (foreGroundAndBackgroundChangeLiveData == null) {
            foreGroundAndBackgroundChangeLiveData = new HLiveData<>();
        }
        return foreGroundAndBackgroundChangeLiveData;
    }

    private static void reflectAppContext() {
        Application app = null;
        try {
            app = (Application) Class.forName("android.app.AppGlobals")
                    .getMethod("getInitialApplication")
                    .invoke(null);
            if (app == null)
                throw new IllegalStateException("Static initialization of Applications must be on main thread.");
        } catch (final Exception e) {
            Log.e(TAG, "Failed to get current application from AppGlobals." + e.getMessage());
            try {
                app = (Application) Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication")
                        .invoke(null);
            } catch (final Exception ex) {
                Log.e(TAG, "Failed to get current application from ActivityThread." + e.getMessage());
            }
        } finally {
            sApplication = app;
        }
        if (sApplication != null && INNER_LIFECYCLE_HANDLER != null) {
            sApplication.registerActivityLifecycleCallbacks(INNER_LIFECYCLE_HANDLER);
        }
    }

    public static Application getAppInstance() {
        checkAppContext();
        return sApplication;
    }

    public static Context getAppContext() {
//        checkAppContext();
        return sApplication.getApplicationContext();
    }

    public static Resources getResources() {
        checkAppContext();
        return sApplication.getResources();
    }

    public static Activity getTopActivity() {
        return INNER_LIFECYCLE_HANDLER.mTopActivity == null ? null : INNER_LIFECYCLE_HANDLER.mTopActivity
                .get();
    }

    private static class InnerLifecycleHandler implements Application.ActivityLifecycleCallbacks {
        private int started;
        private boolean hasVisibleActivities = false;
        private WeakReference<Activity> mTopActivity;


        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
            started++;
            if (mTopActivity != null) {
                mTopActivity.clear();
            }
            mTopActivity = new WeakReference<>(activity);
            if (!hasVisibleActivities && started == 1) {
                hasVisibleActivities = true;
                getForeGroundAndBackgroundChangeLiveData().send(true);
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
            if (started > 0) {
                started--;
            }
            if (hasVisibleActivities && started == 0 && !activity.isChangingConfigurations()) {
                hasVisibleActivities = false;
                getForeGroundAndBackgroundChangeLiveData().send(false);
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

        }

        @Override
        public void onActivityDestroyed(Activity activity) {

        }
    }

}
