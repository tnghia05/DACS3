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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;

public class HttpProvider {
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
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JSONObject sendPost(String URL, RequestBody formBody) {
        JSONObject data = new JSONObject();
        try {
            ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .cipherSuites(
                            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                            CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
                    .build();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectionSpecs(Collections.singletonList(spec))
                    .callTimeout(5000, TimeUnit.MILLISECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(URL)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .post(formBody)
                    .build();

            Response response = client.newCall(request).execute();

            if (!response.isSuccessful()) {
                Log.println(Log.ERROR, "BAD_REQUEST", response.body().string());
                data = null;
            } else {
                data = new JSONObject(response.body().string());
            }

        }  catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return data;
    }
}
