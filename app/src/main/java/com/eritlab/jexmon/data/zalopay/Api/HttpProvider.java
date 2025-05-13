package com.eritlab.jexmon.data.zalopay.Api;

import android.util.Log;

import com.eritlab.jexmon.data.zalopay.Constant.AppInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;

public class HttpProvider {
    private static final int MAX_RETRIES = 3;
    
    private static final Interceptor retryInterceptor = new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException exception = null;
            int retryCount = 0;

            while (retryCount < MAX_RETRIES && (response == null || !response.isSuccessful())) {
                try {
                    if (response != null) {
                        response.close();
                    }
                    response = chain.proceed(request);
                    if (response.isSuccessful()) {
                        return response;
                    }
                } catch (IOException e) {
                    exception = e;
                    Log.e("ZaloPay API", "Retry " + (retryCount + 1) + " failed", e);
                }
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000L * retryCount);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", e);
                    }
                }
            }

            if (response != null) {
                return response;
            }
            throw exception != null ? exception : new IOException("Failed after " + MAX_RETRIES + " retries");
        }
    };

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(retryInterceptor)
            .connectionSpecs(Collections.singletonList(
                    new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .tlsVersions(TlsVersion.TLS_1_2)
                            .cipherSuites(
                                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                                    CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
                            .build()))
            .build();

    public static String sendPaymentRequest(long amount, String description, String embedData, String items, long appTime, String mac) {
        try {
            FormBody formBody = new FormBody.Builder()
                    .add("app_id", String.valueOf(AppInfo.APP_ID))
                    .add("app_user", "Android_Demo")
                    .add("app_time", String.valueOf(appTime))
                    .add("amount", String.valueOf(amount))
                    .add("app_trans_id", String.valueOf(System.currentTimeMillis()))
                    .add("embed_data", embedData)
                    .add("item", items)
                    .add("description", description)
                    .add("mac", mac)
                    .build();

            JSONObject response = sendPost(AppInfo.URL_CREATE_ORDER, formBody);
            if (response != null) {
                return response.getString("zp_trans_token");
            }
            return null;
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject sendPost(String url, RequestBody formBody) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            Log.d("ZaloPay API", "Response body: " + responseBody);
            
            if (!response.isSuccessful()) {
                Log.e("ZaloPay API", "Error response code: " + response.code());
                throw new IOException("Unexpected response code: " + response.code());
            }

            try {
                return new JSONObject(responseBody);
            } catch (JSONException e) {
                Log.e("ZaloPay API", "Error parsing JSON response", e);
                throw new IOException("Error parsing JSON response", e);
            }
        } catch (Exception e) {
            Log.e("ZaloPay API", "Error sending request", e);
            throw new IOException("Error sending request: " + e.getMessage(), e);
        }
    }
}
