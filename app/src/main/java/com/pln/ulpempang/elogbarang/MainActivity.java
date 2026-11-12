package com.pln.ulpempang.elogbarang;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER = 201;
    private WebView webView;
    private ValueCallback<Uri[]> uploadCallback;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("elog_config", MODE_PRIVATE);
        String url = prefs.getString("web_url", "");
        if (url.isEmpty()) showSetup(); else openApp(url);
    }

    private void showSetup() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(36, 48, 36, 24);
        TextView title = new TextView(this);
        title.setText("E-LOG BARANG ULP EMPANG");
        title.setTextSize(22);
        title.setTextColor(0xff075b48);
        box.addView(title);
        TextView info = new TextView(this);
        info.setText("Masukkan URL Web App Google Apps Script yang sudah di-deploy (akhiran /exec).");
        info.setPadding(0, 18, 0, 12);
        box.addView(info);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("https://script.google.com/macros/s/.../exec");
        box.addView(input);
        new AlertDialog.Builder(this).setView(box).setCancelable(false)
            .setPositiveButton("Simpan & Buka", (d,w) -> {
                String value = input.getText().toString().trim();
                if (!value.startsWith("https://script.google.com/") || !value.endsWith("/exec")) {
                    Toast.makeText(this, "URL deployment tidak valid.", Toast.LENGTH_LONG).show();
                    showSetup();
                    return;
                }
                prefs.edit().putString("web_url", value).apply();
                openApp(value);
            }).setNegativeButton("Keluar", (d,w) -> finish()).show();
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
