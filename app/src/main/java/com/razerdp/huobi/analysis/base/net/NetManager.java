package com.razerdp.huobi.analysis.base.net;

import android.annotation.SuppressLint;

import com.razerdp.huobi.analysis.utils.StringUtil;
import com.razerdp.huobi.analysis.utils.log.HLog;
import com.razerdp.huobi.analysis.utils.rx.RxHelper;
import com.razerdp.huobi.analysis.utils.rx.RxTaskCall;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * Created by 大灯泡 on 2021/5/19
 * <p>
 * Description：
 */
public enum NetManager {
    INSTANCE;
    String[] apis = {"api.huobi.pro", "api-aws.huobi.pro", "api.huobi.de.com"};

    String curApi = apis[2];

    @SuppressLint("CheckResult")
    public void init() {
        ping();
    }

    public String api() {
        return curApi;
    }

    void ping() {
        RxHelper.runOnBackground(new RxTaskCall<String>() {
            @Override
            public String doInBackground() {
                int delay = Integer.MAX_VALUE;
                String result = apis[2];
                for (String api : apis) {
                    int curDelay = pingInternal(api);
                    HLog.i("net_ping", "delay for " + api + " is " + curDelay);
                    if (curDelay > 0 && curDelay <= delay) {
                        delay = curDelay;
                        result = api;
                    }
                }
                return result;
            }

            @Override
            public void onResult(String result) {
                HLog.i("net_ping", "set curApi = " + result);
                curApi = result;
                RxHelper.delay(1, TimeUnit.MINUTES, data -> ping());
            }
        });
    }

    int pingInternal(String url) {
        Process p;
        int delay = -1;
        try {
            InetAddress inetAddress = InetAddress.getByName(url);
            String ip = inetAddress.getHostAddress();
            p = Runtime.getRuntime().exec("/system/bin/ping -c 3 " + ip);
            if (p == null) {
                HLog.e("net_ping", "error for ping: " + url);
                return -1;
            }
            InputStream input = p.getInputStream();
            if (input == null) {
                HLog.e("net_ping", "process InputStream is null: " + url);
                return -1;
            }
            BufferedReader buf = new BufferedReader(new InputStreamReader(input));
            String content;
            while ((content = buf.readLine()) != null) {
                if (content.contains("avg")) {
                    int i = content.indexOf("/", 20);
                    int j = content.indexOf(".", i);
                    delay = StringUtil.toInt(content.substring(i + 1, j));
                }
            }
        } catch (Exception e) {
            HLog.e(e);
            e.printStackTrace();
        }
        return delay;
    }

}
