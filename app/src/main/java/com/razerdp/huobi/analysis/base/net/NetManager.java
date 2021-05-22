package com.razerdp.huobi.analysis.base.net;

import com.razerdp.huobi.analysis.base.net.interceptor.HttpLoggingInterceptor;
import com.razerdp.huobi.analysis.entity.UserInfo;
import com.razerdp.huobi.analysis.utils.StringUtil;
import com.razerdp.huobi.analysis.utils.TimeUtil;
import com.razerdp.huobi.analysis.utils.log.HLog;
import com.razerdp.huobi.analysis.utils.rx.RxHelper;
import com.razerdp.huobi.analysis.utils.rx.RxTaskCall;

import android.annotation.SuppressLint;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import rxhttp.RxHttp;
import rxhttp.wrapper.annotation.DefaultDomain;
import rxhttp.wrapper.annotation.Param;
import rxhttp.wrapper.annotations.Nullable;
import rxhttp.wrapper.entity.KeyValuePair;
import rxhttp.wrapper.param.AbstractParam;
import rxhttp.wrapper.param.FormParam;
import rxhttp.wrapper.param.Method;
import rxhttp.wrapper.ssl.HttpsUtils;

/**
 * Created by 大灯泡 on 2021/5/19
 * <p>
 * Description：
 */
public enum NetManager {
    INSTANCE;
    String[] apis = {"api.huobi.pro", "api-aws.huobi.pro", "api.huobi.de.com"};

    public static class Url {

        @DefaultDomain
        public static String baseUrl = INSTANCE.api();
    }

    String curApi = apis[2];

    @SuppressLint("CheckResult")
    public void init() {
        RxHttp.init(getDefaultOkHttpClient());
        ping();
    }

    private static OkHttpClient getDefaultOkHttpClient() {
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory();
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager)
                .hostnameVerifier((hostname, session) -> true)
                .addInterceptor(new HttpLoggingInterceptor())
                .build();
    }

    public String simpleApi() {
        return curApi;
    }

    public String api() {
        return String.format("https://%s", simpleApi());
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
                Url.baseUrl = api();
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
            e.printStackTrace();
        }
        return delay;
    }

    // https://github.com/liujingxing/rxhttp/wiki/%E9%AB%98%E7%BA%A7%E5%8A%9F%E8%83%BD#%E8%87%AA%E5%AE%9A%E4%B9%89Param
    interface BaseParam {

        UserInfo getUserInfo();

        String getCurApi();
    }

    @Param(methodName = "post")
    public static class PostSignParam extends FormParam implements BaseParam {

        UserInfo userInfo;
        String curApi;

        public PostSignParam(String url, UserInfo userInfo) {
            super(url, rxhttp.wrapper.param.Method.POST);
            this.userInfo = userInfo;
            this.curApi = INSTANCE.simpleApi();
        }

        @Override
        public UserInfo getUserInfo() {
            return userInfo;
        }

        @Override
        public String getCurApi() {
            return curApi;
        }
    }

    @Param(methodName = "get")
    public static class GetSignParam extends AbstractParam<GetSignParam> implements BaseParam {

        UserInfo userInfo;
        String curApi;

        public GetSignParam(String url, UserInfo userInfo) {
            super(url, Method.GET);
            this.userInfo = userInfo;
            this.curApi = INSTANCE.simpleApi();
        }

        @Override
        public UserInfo getUserInfo() {
            return userInfo;
        }

        @Override
        public String getCurApi() {
            return curApi;
        }

        @Override
        public GetSignParam add(String key, @Nullable Object value) {
            return addQuery(key, value);
        }

        @Override
        public final RequestBody getRequestBody() {
            return null;
        }

        public GetSignParam sign() {
            Sign.sign(this);
            return this;
        }

    }

    public void test() {
        UserInfo userInfo = new UserInfo("b921733d-a2794daf-cb171f7c-bg2hyw2dfg",
                "e0e7ae45-78660353-60678e79-345bf");
        userInfo.accountId = 16936884;
    }

    public void testSign() {
        StringBuilder builder = new StringBuilder();
        builder.append("GET").append('\n')
               .append("api.huobi.pro").append('\n')
               .append("/v2/account/asset-valuation").append('\n')
               .append("AccessKeyId=b921733d-a2794daf-cb171f7c-bg2hyw2dfg&SignatureMethod=HmacSHA256&SignatureVersion=2&Timestamp=2021-05-22T18%3A20%3A12&accountType=spot&valuationCurrency=CNY");
        HLog.i("testSign", Sign.signInternal(builder.toString(), Sign.V1.SIGN_METHOD_VALUE,
                "e0e7ae45-78660353-60678e79-345bf"));
    }

    // https://huobiapi.github.io/docs/spot/v1/cn/#c64cd15fdc
    static class Sign {

        static class V1 {

            public static final String TIME = "Timestamp";
            public static final String ACC_ID = "AccessKeyId";
            public static final String SIGN_VER = "SignatureVersion";
            public static final String SIGN_VER_VALUE = "2";
            public static final String SIGN_METHOD = "SignatureMethod";
            public static final String SIGN_METHOD_VALUE = "HmacSHA256";
            public static final String SIGN = "Signature";
        }

        static class V2 {

            public static final String TIME = "timestamp";
            public static final String ACC_ID = "accessKey";
            public static final String SIGN_VER = "signatureVersion";
            public static final String SIGN_VER_VALUE = "2.1";
            public static final String SIGN_METHOD = "signatureMethod";
            public static final String SIGN_METHOD_VALUE = "HmacSHA256";
            public static final String SIGN = "signature";
        }

        public static void sign(BaseParam param) {
            AbstractParam requestParam = (AbstractParam) param;
            StringBuilder builder = new StringBuilder();
            builder.append(requestParam.getMethod().toString()).append('\n')
                   .append(param.getCurApi()).append('\n')
                   .append(requestParam.getSimpleUrl()).append('\n');
//            boolean isV2 = requestParam.getSimpleUrl().toLowerCase().contains("v2");
            signV1(builder, param);
        }

        @SuppressWarnings("ConstantConditions")
        static void signV1(StringBuilder builder, BaseParam param) {
            AbstractParam requestParam = (AbstractParam) param;
            requestParam.add(V1.ACC_ID, param.getUserInfo().accetKey);
            requestParam.add(V1.SIGN_METHOD, V1.SIGN_METHOD_VALUE);
            requestParam.add(V1.SIGN_VER, V1.SIGN_VER_VALUE);
            requestParam.add(V1.TIME,
                    TimeUtil.longToTimeStr(System.currentTimeMillis(), TimeUtil.YYYYMMDDTHHMMSS));
            builder.append(sortAndEncode(requestParam.getQueryParam()));
            HLog.i("signV1", builder);
            requestParam
                    .addEncodedQuery(V1.SIGN, signInternal(builder.toString(), V1.SIGN_METHOD_VALUE,
                            param.getUserInfo().secretKey));
        }

        @SuppressWarnings("ConstantConditions")
        static void signV2(StringBuilder builder, BaseParam param) {
            AbstractParam requestParam = (AbstractParam) param;
            requestParam.add(V2.ACC_ID, param.getUserInfo().accetKey);
            requestParam.add(V2.SIGN_METHOD, V2.SIGN_METHOD_VALUE);
            requestParam.add(V2.SIGN_VER, V2.SIGN_VER_VALUE);
            requestParam.add(V2.TIME,
                    TimeUtil.longToTimeStr(System.currentTimeMillis(), TimeUtil.YYYYMMDDTHHMMSS));
            builder.append(sortAndEncode(requestParam.getQueryParam()));
            HLog.i("signV2", builder);
            requestParam
                    .addEncodedQuery(V2.SIGN, signInternal(builder.toString(), V2.SIGN_METHOD_VALUE,
                            param.getUserInfo().secretKey));
        }

        static String sortAndEncode(List<KeyValuePair> keyValues) {
            StringBuilder builder = new StringBuilder();
            Collections.sort(keyValues, COMPARATOR);
            for (KeyValuePair keyValue : keyValues) {
                builder.append("&")
                       .append(keyValue.getKey())
                       .append("=")
                       .append(encode(keyValue.getValue()));
            }
            builder.delete(0, 1);
            return builder.toString();
        }

        static String encode(Object what) {
            String encodeTarget = String.valueOf(what);
            try {
                return URLEncoder.encode(encodeTarget, "UTF-8").replaceAll("\\+", "%20");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return encodeTarget;
            }
        }

        static String signInternal(String from, String signType, String secretKey) {
            Mac hmacSha256;
            try {
                hmacSha256 = Mac.getInstance(signType);
                SecretKeySpec secKey = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8),
                        signType);
                hmacSha256.init(secKey);
                byte[] hash = hmacSha256.doFinal(from.getBytes(StandardCharsets.UTF_8));
                return Base64.encodeToString(hash, Base64.DEFAULT);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                HLog.e("signInternal", e);
            }
            return "null";
        }

        static Comparator<KeyValuePair> COMPARATOR = (o1, o2) -> o1.getKey().compareTo(
                o2.getKey());


    }
}
