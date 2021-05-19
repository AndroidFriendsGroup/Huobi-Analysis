package com.razerdp.huobi.analysis.base.net;

import android.annotation.SuppressLint;

import com.razerdp.huobi.analysis.utils.StringUtil;
import com.razerdp.huobi.analysis.utils.TimeUtil;
import com.razerdp.huobi.analysis.utils.log.HLog;
import com.razerdp.huobi.analysis.utils.rx.RxHelper;
import com.razerdp.huobi.analysis.utils.rx.RxTaskCall;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.RequestBody;
import rxhttp.wrapper.annotation.DefaultDomain;
import rxhttp.wrapper.annotation.Param;
import rxhttp.wrapper.entity.KeyValuePair;
import rxhttp.wrapper.param.FormParam;
import rxhttp.wrapper.param.Method;

/**
 * Created by 大灯泡 on 2021/5/19
 * <p>
 * Description：
 */
public enum NetManager {
    INSTANCE;
    String[] apis = {"api.huobi.pro", "api-aws.huobi.pro", "api.huobi.de.com"};


    static class Url {
        @DefaultDomain
        public static String baseUrl = INSTANCE.api();
    }

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
                Url.baseUrl = curApi;
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


    // https://github.com/liujingxing/rxhttp/wiki/%E9%AB%98%E7%BA%A7%E5%8A%9F%E8%83%BD#%E8%87%AA%E5%AE%9A%E4%B9%89Param
    static abstract class BaseFormParam extends FormParam {
        String apiKey;

        public BaseFormParam(String url, Method method, String apiKey) {
            super(url, method);
            this.apiKey = apiKey;
        }

        public String getApiKey() {
            return apiKey;
        }
    }

    @Param(methodName = "post")
    static class PostSignParam extends BaseFormParam {

        public PostSignParam(String url, String apiKey) {
            super(url, Method.POST, apiKey);
        }
    }

    @Param(methodName = "get")
    static class GetSignParam extends BaseFormParam {

        public GetSignParam(String url, String apiKey) {
            super(url, Method.GET, apiKey);
        }

        @Override
        public RequestBody getRequestBody() {
            return super.getRequestBody();
        }
    }

    // https://huobiapi.github.io/docs/spot/v1/cn/#c64cd15fdc
    static class Sign {
        public static final String HUOBI_KEY_TIME = "Timestamp";
        public static final String HUOBI_KEY_ACCESSKEY = "AccessKeyId";

        public static void sign(BaseFormParam param) {
            StringBuilder builder = new StringBuilder();
            builder.append(param.getMethod().toString()).append('\n')
                    .append(INSTANCE.api()).append('\n')
                    .append(param.getSimpleUrl()).append('\n');
            List<KeyValuePair> queryParams = param.getQueryParam();
            Map<String, Object> paramsMap = new HashMap<>();
            for (KeyValuePair kv : queryParams) {
                paramsMap.put(kv.getKey(), String.valueOf(kv.getValue()));
            }
            fillParams(paramsMap, param);
            builder.append(sortAndGetString(paramsMap));

        }

        public static void fillParams(Map<String, Object> paramsMap, BaseFormParam param) {
            if (!paramsMap.containsKey(HUOBI_KEY_ACCESSKEY)) {
                paramsMap.put(HUOBI_KEY_ACCESSKEY, String.valueOf(param.getApiKey()));
            }
            if (!paramsMap.containsKey(HUOBI_KEY_TIME)) {
                paramsMap.put(HUOBI_KEY_TIME, TimeUtil.longToTimeStr(System.currentTimeMillis(), TimeUtil.YYYYMMDDTHHMMSS));
            }
        }

        public static String sortAndGetString(Map<String, Object> paramsMap) {
            StringBuilder builder = new StringBuilder();
            List<String> keys = new ArrayList<>(paramsMap.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                builder.append(key).append("=").append(encode(paramsMap.get(key))).append("&");
            }
            builder.delete(builder.length() - 1, builder.length());
            return builder.toString();
        }

        public static String encode(Object what) {
            String encodeTarget = String.valueOf(what);
            try {
                return URLEncoder.encode(encodeTarget, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return encodeTarget;
            }
        }
    }
}
