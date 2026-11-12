package com.pln.ulpempang.elogbarang;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER = 201;
    private static final String WEB_APP_URL = "https://script.google.com/macros/s/AKfycbyTLwH-5sYJ8s6iIQfiyRkim9bhGy4nA2wfF1Bci0J7c7e-4-dwr7vGPqJzjAh1pCtz/exec";
    private WebView webView;
    private ValueCallback<Uri[]> uploadCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        openApp(WEB_APP_URL);
    }

    private void openApp(String url) {
        webView = new WebView(this);
        webView.setBackgroundColor(0xfff3f6f8);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String next) {
                if (next.startsWith("https://script.google.com/") || next.startsWith("https://script.googleusercontent.com/") || next.startsWith("https://drive.google.com/")) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(next)));
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = callback;
                Intent intent = params.createIntent();
                try { startActivityForResult(intent, FILE_CHOOSER); }
                catch (Exception e) { uploadCallback = null; return false; }
                return true;
            }
        });
        setContentView(webView);
        webView.loadUrl(url);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER && uploadCallback != null) {
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            uploadCallback.onReceiveValue(results);
            uploadCallback = null;
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}