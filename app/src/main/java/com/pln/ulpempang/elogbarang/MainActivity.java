package com.pln.ulpempang.elogbarang;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.view.View;

import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final int CAMERA_REQUEST = 201;
    private WebView webView;
    private ValueCallback<Uri[]> uploadCallback;
    private Uri cameraPhotoUri;

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
        settings.setAllowFileAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(false);
        settings.setLoadWithOverviewMode(false);
        settings.setTextZoom(100);
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
                openCameraOnly();
                return true;
            }
        });
        setContentView(webView);
        webView.loadUrl("https://script.google.com/macros/s/AKfycbyTLwH-5sYJ8s6iIQfiyRkim9bhGy4nA2wfF1Bci0J7c7e-4-dwr7vGPqJzjAh1pCtz/exec?embedded=true");
    }

    private void openCameraOnly() {
        try {
            File dir = new File(getCacheDir(), "barang-camera");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("Tidak dapat membuat folder foto");
            File photo = File.createTempFile("elog-barang-", ".jpg", dir);
            cameraPhotoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photo);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (intent.resolveActivity(getPackageManager()) == null) throw new IOException("Kamera tidak tersedia");
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
}