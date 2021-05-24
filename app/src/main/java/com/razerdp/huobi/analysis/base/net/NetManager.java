package com.razerdp.huobi.analysis.base.net;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Base64;

import com.razerdp.huobi.analysis.base.net.exception.NetException;
import com.razerdp.huobi.analysis.base.net.interceptor.HttpLoggingInterceptor;
import com.razerdp.huobi.analysis.base.net.interceptor.SignInterceptor;
import com.razerdp.huobi.analysis.entity.UserInfo;
import com.razerdp.huobi.analysis.net.response.base.BaseResponse;
import com.razerdp.huobi.analysis.utils.StringUtil;
import com.razerdp.huobi.analysis.utils.log.HLog;
import com.razerdp.huobi.analysis.utils.rx.RxHelper;
import com.razerdp.huobi.analysis.utils.rx.RxTaskCall;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import rxhttp.RxHttpPlugins;
import rxhttp.wrapper.annotation.DefaultDomain;
import rxhttp.wrapper.annotation.Param;
import rxhttp.wrapper.annotation.Parser;
import rxhttp.wrapper.annotations.Nullable;
import rxhttp.wrapper.entity.KeyValuePair;
import rxhttp.wrapper.param.AbstractParam;
import rxhttp.wrapper.param.Method;
import rxhttp.wrapper.parse.AbstractParser;
import rxhttp.wrapper.ssl.HttpsUtils;
import rxhttp.wrapper.utils.Converter;

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
        RxHttpPlugins.init(getDefaultOkHttpClient());
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
                .addInterceptor(new SignInterceptor())
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

    @Parser(name = "Response")
    public static class ResponseParser<T> extends AbstractParser<T> {

        protected ResponseParser() {
            super();
        }

        public ResponseParser(Type type) {
            super(type);
        }

        @Override
        public T onParse(@NotNull Response response) throws IOException {
            BaseResponse<T> data = Converter.convertTo(response, BaseResponse.class, mType);
            if (data == null) {
                throw new NetException("0", "no data");
            }
            if (!data.isOK()) {
                throw new NetException(data.getErrorCode(), data.getErrorMsg());
            }
            return data.data;
        }
    }

    // https://github.com/liujingxing/rxhttp/wiki/%E9%AB%98%E7%BA%A7%E5%8A%9F%E8%83%BD#%E8%87%AA%E5%AE%9A%E4%B9%89Param
    interface BaseParam {

        UserInfo getUserInfo();

    }

    @Param(methodName = "get")
    public static class GetSignParam extends AbstractParam<GetSignParam> implements BaseParam {

        UserInfo userInfo;

        public GetSignParam(String url, UserInfo userInfo) {
            super(url, Method.GET);
            this.userInfo = userInfo;
        }

        @Override
        public UserInfo getUserInfo() {
            return userInfo;
        }


        @Override
        public GetSignParam add(String key, @Nullable Object value) {
            return addQuery(key, value);
        }

        @Override
        public GetSignParam removeAllQuery(String key) {
            final List<KeyValuePair> pairs = getQueryParam();
            if (pairs != null) {
                Iterator<KeyValuePair> iterator = pairs.iterator();
                while (iterator.hasNext()) {
                    KeyValuePair next = iterator.next();
                    if (TextUtils.equals(next.getKey(), key)) {
                        iterator.remove();
                    }

                }
            }
            return this;
        }

        @Override
        public final RequestBody getRequestBody() {
            return null;
        }

        @Override
        public <T> GetSignParam tag(Class<? super T> type, T tag) {
            Sign.appendParams(this);
            return super.tag(type, tag);
        }
    }

    // https://huobiapi.github.io/docs/spot/v1/cn/#c64cd15fdc
    public static class Sign {

        static class V1 {

            public static final String TIME = "Timestamp";
            public static final String ACC_ID = "AccessKeyId";
            public static final String SIGN_VER = "SignatureVersion";
            public static final String SIGN_VER_VALUE = "2";
            public static final String SIGN_METHOD = "SignatureMethod";
            public static final String SIGN_METHOD_VALUE = "HmacSHA256";
            public static final String SIGN = "Signature";
            public static final String INTERNAL_SECRET_KEY = "SecretKey";
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

        private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter
                .ofPattern("uuuu-MM-dd'T'HH:mm:ss");
        private static final ZoneId ZONE_GMT = ZoneId.of("Z");

        public static Request sign(Request request) {
            HttpUrl url = request.url();
            HttpUrl.Builder urlBuilder = url.newBuilder();
            StringBuilder builder = new StringBuilder();
            builder.append(request.method().toUpperCase()).append('\n')
                    .append(url.host()).append('\n')
                    .append(url.encodedPath()).append('\n');
            List<KeyValuePair> params = new ArrayList<>();
            String secretKey = url.queryParameter(V1.INTERNAL_SECRET_KEY);
            if (TextUtils.isEmpty(secretKey)) {
                return request;
            }
            params.add(new KeyValuePair(V1.TIME, timeStamp()));
            for (String key : url.queryParameterNames()) {
                if (!TextUtils.equals(key, V1.INTERNAL_SECRET_KEY)) {
                    params.add(new KeyValuePair(key, url.queryParameter(key)));
                }
            }
            Collections.sort(params, COMPARATOR);
            for (KeyValuePair param : params) {
                urlBuilder.setEncodedQueryParameter(param.getKey(), encode(param.getValue()));
            }
            urlBuilder.removeAllEncodedQueryParameters(V1.INTERNAL_SECRET_KEY);
            builder.append(getSignString(params));
            HLog.i("sign", builder);
            urlBuilder.setEncodedQueryParameter(V1.SIGN, signInternal(builder.toString(),
                    V1.SIGN_METHOD_VALUE,
                    secretKey));
            return request.newBuilder().url(urlBuilder.build()).build();
        }

        static void appendParams(BaseParam param) {
            AbstractParam requestParam = (AbstractParam) param;
            requestParam.setQuery(V1.ACC_ID, param.getUserInfo().accetKey);
            requestParam.setQuery(V1.SIGN_METHOD, V1.SIGN_METHOD_VALUE);
            requestParam.setQuery(V1.SIGN_VER, V1.SIGN_VER_VALUE);
            requestParam.setQuery(V1.INTERNAL_SECRET_KEY, param.getUserInfo().secretKey);
        }


        static String getSignString(List<KeyValuePair> keyValues) {
            StringBuilder builder = new StringBuilder();
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

        static String timeStamp() {
            return Instant.ofEpochSecond(Instant.now().getEpochSecond())
                    .atZone(ZONE_GMT)
                    .format(DT_FORMAT);
        }
    }
}
