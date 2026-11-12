package com.pln.ulpempang.elogbarang;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.view.View;

import androidx.core.content.FileProvider;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int CAMERA_REQUEST = 201;
    private static final String API_URL = "https://script.google.com/macros/s/AKfycbyTLwH-5sYJ8s6iIQfiyRkim9bhGy4nA2wfF1Bci0J7c7e-4-dwr7vGPqJzjAh1pCtz/exec";
    private WebView webView;
    private ValueCallback<Uri[]> uploadCallback;
    private Uri cameraPhotoUri;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(0xff075b48);
        getWindow().setNavigationBarColor(0xfff1f5f4);
        openApp();
    }

    private void openApp() {
        webView = new WebView(this);
        webView.setBackgroundColor(0xfff1f5f4);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(false);
        settings.setLoadWithOverviewMode(false);
        settings.setTextZoom(100);
        webView.addJavascriptInterface(new AppBridge(), "AndroidApp");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = callback;
                openCameraOnly();
                return true;
            }
        });
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private class AppBridge {
        @JavascriptInterface public void getDashboard() {
            executor.execute(() -> {
                try {
                    JSONObject req = new JSONObject();
                    req.put("action", "getDashboard");
                    JSONObject result = post(req);
                    sendJs("window.onDashboard(" + result.toString() + ");");
                } catch (Exception e) {
                    sendJs("window.onApiError(" + JSONObject.quote(e.getMessage()) + ");");
                }
            });
        }

        @JavascriptInterface public void saveTransaction(String rawData) {
            executor.execute(() -> {
                try {
                    JSONObject req = new JSONObject();
                    req.put("action", "saveTransaction");
                    req.put("data", new JSONObject(rawData));
                    JSONObject result = post(req);
                    sendJs("window.onSaveResult(" + result.toString() + ");");
                } catch (Exception e) {
                    JSONObject err = new JSONObject();
                    try { err.put("ok", false); err.put("error", e.getMessage()); } catch (Exception ignored) {}
                    sendJs("window.onSaveResult(" + err.toString() + ");");
                }
            });
        }
    }

    private JSONObject post(JSONObject body) throws Exception {
        URL current = new URL(API_URL);
        String method = "POST";
        byte[] requestBytes = body.toString().getBytes(StandardCharsets.UTF_8);
        for (int hop = 0; hop < 6; hop++) {
            HttpURLConnection conn = (HttpURLConnection) current.openConnection();
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Accept", "application/json, text/plain, */*");
            conn.setRequestProperty("User-Agent", "ELogBarangAndroid/2.1");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestMethod(method);
            if ("POST".equals(method)) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setFixedLengthStreamingMode(requestBytes.length);
                try (OutputStream os = conn.getOutputStream()) { os.write(requestBytes); }
            }
            int status = conn.getResponseCode();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null) throw new Exception("Google mengalihkan permintaan tanpa alamat tujuan.");
                current = new URL(current, location);
                if (status == 303 || ((status == 301 || status == 302) && "POST".equals(method))) method = "GET";
                continue;
            }
            InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String response = readAll(stream);
            String contentType = conn.getContentType();
            String finalUrl = current.toString();
            conn.disconnect();
            String trimmed = response.trim();
            if (trimmed.startsWith("<!DOCTYPE") || trimmed.startsWith("<html") || (contentType != null && contentType.toLowerCase().contains("text/html"))) {
                String preview = trimmed.replaceAll("\\s+", " ");
                throw new Exception("Respons Google bukan JSON. HTTP " + status + ", tipe=" + contentType + ", alamat=" + finalUrl + ", isi=" + preview.substring(0, Math.min(220, preview.length())));
            }
            try {
                JSONObject parsed = new JSONObject(trimmed);
                if (status >= 400) throw new Exception("Server Google (" + status + "): " + parsed.optString("error", trimmed));
                return parsed;
            } catch (org.json.JSONException ex) {
                throw new Exception("Respons server tidak valid (HTTP " + status + "): " + trimmed.substring(0, Math.min(180, trimmed.length())));
            }
        }
        throw new Exception("Terlalu banyak pengalihan dari server Google.");
    }

    private String readAll(InputStream is) throws Exception {
        if (is == null) throw new Exception("Server tidak mengirim respons.");
        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) out.append(line);
        }
        return out.toString();
    }

    private void sendJs(String script) {
        runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(script, null); });
    }

    private void openCameraOnly() {
        try {
            File dir = new File(getCacheDir(), "barang-camera");
            if (!dir.exists() && !dir.mkdirs()) throw new Exception("Tidak dapat membuat folder foto");
            File photo = File.createTempFile("elog-barang-", ".jpg", dir);
            cameraPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photo);
            Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (intent.resolveActivity(getPackageManager()) == null) throw new Exception("Kamera tidak tersedia");
            startActivityForResult(intent, CAMERA_REQUEST);
        } catch (Exception e) {
            if (uploadCallback != null) uploadCallback.onReceiveValue(null);
            uploadCallback = null;
            Toast.makeText(this, "Tidak dapat membuka kamera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && uploadCallback != null) {
            Uri[] result = resultCode == RESULT_OK && cameraPhotoUri != null ? new Uri[]{cameraPhotoUri} : null;
            uploadCallback.onReceiveValue(result);
            uploadCallback = null;
            cameraPhotoUri = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        executor.shutdown();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
