package com.razerdp.huobi.analysis.base.net.interceptor;

import android.text.TextUtils;

import com.razerdp.huobi.analysis.net.api.market.SupportedTrade;
import com.razerdp.huobi.analysis.utils.gson.GsonUtil;
import com.razerdp.huobi.analysis.utils.log.HLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;

/**
 * Created by 大灯泡 on 2019/5/5
 * <p>
 * Description：
 */
public final class HttpLoggingInterceptor implements Interceptor {
    private static final String TAG = "Net";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (!HLog.isOpenLog()) {
            return chain.proceed(chain.request());
        }

        Request request = chain.request();

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;
        if (String.valueOf(request.url()).contains(SupportedTrade.getSupportedTrades())) {
            return chain.proceed(request);
        }

        StringBuilder logBuilder = new StringBuilder('\n');
        logBuilder.append("【Send Request】-->")
                .append(request.method())
                .append("  ")
                .append(request.url())
                .append('\n');
        if (hasRequestBody) {
            logBuilder.append("【Content-Type】-->")
                    .append(requestBody.contentType())
                    .append('\n');
        }
        logBuilder.append("【Headers】-->\n");

        Headers headers = request.headers();
        if (headers.size() > 0) {
            for (int i = 0, count = headers.size(); i < count; i++) {
                String name = headers.name(i);
                // Skip headers from the request body as they are explicitly logged above.
                if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                    logBuilder.append(name).append(" : ").append(headers.value(i)).append('\n');
                }
            }
            logBuilder.append("}\n");
        }

        logBuilder.append("【Body】-->\n");
        if (hasRequestBody) {
            Buffer buffer = new Buffer();
            requestBody.writeTo(buffer);

            Charset charset = UTF8;
            MediaType contentType = requestBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(UTF8);
            }
            logBuilder.append(GsonUtil.INSTANCE.toString(urlToHashMap(buffer.readString(charset))));
        }

        HLog.i(TAG, logBuilder.toString());

        logBuilder = new StringBuilder();
        //response
        long startMs = System.currentTimeMillis();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            HLog.e(TAG, request.url(), e.getMessage());
            throw e;
        }
        long tookMs = System.currentTimeMillis() - startMs;

        ResponseBody responseBody = response.body();
        long contentLength = responseBody.contentLength();
        logBuilder.append("【Receive Response】-->  ").append(response.request().url()).append('\n')
                .append("code：").append(response.code()).append('\n')
                .append("message：").append(response.message()).append('\n')
                .append("time：").append(tookMs).append("ms\n");
        BufferedSource source = responseBody.source();
        source.request(Long.MAX_VALUE); // Buffer the entire body.
        Buffer buffer = source.buffer();

        if ("gzip".equalsIgnoreCase(headers.get("Content-Encoding"))) {
            GzipSource gzippedResponseBody = null;
            try {
                gzippedResponseBody = new GzipSource(buffer.clone());
                buffer = new Buffer();
                buffer.writeAll(gzippedResponseBody);
            } finally {
                if (gzippedResponseBody != null) {
                    gzippedResponseBody.close();
                }
            }
        }

        Charset charset = UTF8;
        MediaType contentType = responseBody.contentType();
        if (contentType != null) {
            charset = contentType.charset(UTF8);
        }

        if (!isPlaintext(buffer)) {
            logBuilder.append("无法解析返回值，可能是图片 ");
            return response;
        }

        if (contentLength != 0) {
            logBuilder.append(wrapParseJson(buffer.clone().readString(charset)));
            HLog.w(TAG, logBuilder.toString());
        }
        return response;
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    static boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false; // Truncated UTF-8 sequence.
        }
    }

    public static String wrapParseJson(String jsonStr) {
        String message;
        if (TextUtils.isEmpty(jsonStr)) return "json为空";
        try {
            if (jsonStr.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(jsonStr);
                message = jsonObject.toString(2);
                message = "================Response================\n"
                        + message + '\n'
                        + "================Response================\n";
            } else if (jsonStr.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(jsonStr);
                message = jsonArray.toString(4);
                message = "\n================Response Array================\n"
                        + message + '\n'
                        + "================Response Array================\n";
            } else {
                message = jsonStr;
            }
        } catch (JSONException e) {
            message = jsonStr;
        }

        return message;
    }

    private HashMap<String, String> urlToHashMap(String url) {
        HashMap<String, String> result = new HashMap<>();
        if (TextUtils.isEmpty(url)) return result;
        String[] params = url.split("&");
        if (params.length > 0) {
            for (String param : params) {
                String[] kv = param.split("=");
                if (kv.length > 1) {
                    result.put(kv[0], kv[1]);
                } else {
                    if (kv.length > 0 && !TextUtils.isEmpty(kv[0])) {
                        result.put(kv[0], "");
                    }
                }
            }
        }
        return result;

    }
}