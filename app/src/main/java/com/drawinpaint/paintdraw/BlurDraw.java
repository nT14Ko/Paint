package com.drawinpaint.paintdraw;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BlurDraw extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;

    private String mCM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR = 1;

    private SwipeRefreshLayout swipe;

    private String marap = "";

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_result);

        sharedPreferences = getApplicationContext().getSharedPreferences("DATA", Context.MODE_PRIVATE);
        marap = sharedPreferences.getString("marap", "");

        webView = (WebView) findViewById(R.id.webView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        if (isNetworkConnected()) {

            WebSettings mWebSettings = webView.getSettings();

            mWebSettings.setJavaScriptEnabled(true);

            mWebSettings.setAppCacheEnabled(true);
            mWebSettings.setDomStorageEnabled(true);
            mWebSettings.setDatabaseEnabled(true);
            mWebSettings.setSupportZoom(false);
            mWebSettings.setAllowFileAccess(true);
            mWebSettings.setAllowFileAccess(true);
            mWebSettings.setAllowContentAccess(true);
            webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

            try {
                CookieSyncManager.createInstance(this);
            } catch (Exception ex) {
            }

            CookieManager cookieManager = CookieManager.getInstance();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.setAcceptThirdPartyCookies(webView, true);
            } else {
                cookieManager.setAcceptCookie(true);
            }

            mWebSettings.setLoadWithOverviewMode(true);
            mWebSettings.setUseWideViewPort(true);

            mWebSettings.setJavaScriptCanOpenWindowsAutomatically(true);

            mWebSettings.setPluginState(WebSettings.PluginState.ON);
            mWebSettings.setSavePassword(true);

            webView.requestFocus(View.FOCUS_DOWN);

            webView.setWebViewClient(new ProWebViewClient());

            if(!marap.equals("")) {
                webView.loadUrl(marap);
            }

            webView.setWebChromeClient(new WebChromeClient() {

                @Override
                public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                    showMessageOKCancel(message);
                    result.cancel();
                    return true;
                }

                public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                    mUM = uploadMsg;
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    BlurDraw.this.startActivityForResult(Intent.createChooser(i, "File Chooser"), FCR);
                }

                public void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                    mUM = uploadMsg;
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    BlurDraw.this.startActivityForResult(
                            Intent.createChooser(i, "File Browser"),
                            FCR);
                }

                public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                    mUM = uploadMsg;
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    BlurDraw.this.startActivityForResult(Intent.createChooser(i, "File Chooser"), BlurDraw.FCR);
                }

                public boolean onShowFileChooser(
                        WebView webView, ValueCallback<Uri[]> filePathCallback,
                        FileChooserParams fileChooserParams) {
                    int permissionStatus = ContextCompat.checkSelfPermission(BlurDraw.this, Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
                        if (mUMA != null) {
                            mUMA.onReceiveValue(null);
                        }
                        mUMA = filePathCallback;
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(BlurDraw.this.getPackageManager()) != null) {
                            File photoFile = null;
                            try {
                                photoFile = createImageFile();
                                takePictureIntent.putExtra("PhotoPath", mCM);
                            } catch (IOException ex) {
                            }
                            if (photoFile != null) {
                                mCM = "file:" + photoFile.getAbsolutePath();
                                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                            } else {
                                takePictureIntent = null;
                            }
                        }
                        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                        contentSelectionIntent.setType("*/*");
                        Intent[] intentArray;
                        if (takePictureIntent != null) {
                            intentArray = new Intent[]{takePictureIntent};
                        } else {
                            intentArray = new Intent[0];
                        }

                        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                        startActivityForResult(chooserIntent, FCR);

                        return true;
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            check_permission();
                        }
                        return false;
                    }
                }

                @Override
                public void onProgressChanged(WebView view, int progress) {
                    progressBarShow(progressBar, progress);
                }
            });
        } else {
            webView.setWebViewClient(new ProWebViewClient());
            showPageErrorInternet(webView, 1);
        }

        swipe = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
            }
        });
    }

    private void showMessageOKCancel(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.mipmap.ic_launcher_round);
        builder.setTitle("Уведомление");
        builder.setMessage(message)
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (Build.VERSION.SDK_INT >= 21) {
            Uri[] results = null;
            if (resultCode == BlurDraw.RESULT_OK) {
                if (requestCode == FCR) {
                    if (null == mUMA) {
                        return;
                    }
                    if (intent == null) {
                        if (mCM != null) {
                            results = new Uri[]{Uri.parse(mCM)};
                        }
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        } else {
            if (requestCode == FCR) {
                if (null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void check_permission() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                }, 1);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(R.mipmap.ic_launcher_round);
            builder.setTitle("Уведомление");
            builder.setMessage("Вы действительно хотите выйти с приложения?")
                    .setCancelable(true)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.dismiss();
                            BlurDraw.super.onBackPressed();
                        }
                    })
                    .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.dismiss();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            CookieSyncManager.getInstance().startSync();
        } catch (Exception ex) {
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            CookieSyncManager.getInstance().stopSync();
        } catch (Exception ex) {
        }
    }

    public void progressBarShow(ProgressBar pr_bar, int progress_page) {
        pr_bar.setActivated(true);
        pr_bar.setVisibility(View.VISIBLE);
        pr_bar.setProgress(progress_page);
        if (progress_page == 100) {
            pr_bar.setVisibility(View.GONE);
            pr_bar.setActivated(false);
            swipe.setRefreshing(false);
        }
    }

    public void showPageErrorInternet(WebView view_error_internet, int error_code) {
        String text_error = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"utf-8\" />" +
                "<title>No connection to the internet</title>" +
                "<style>" +
                "html,body { margin:0; padding:0; }" +
                "html {" +
                "background: #191919 -webkit-linear-gradient(top, #000 0%, #191919 100%) no-repeat;" +
                "background: #191919 linear-gradient(to bottom, #000 0%, #191919 100%) no-repeat;" +
                "}" +
                "body {" +
                "font-family: sans-serif;" +
                "color: #FFF;" +
                "text-align: center;" +
                "font-size: 150%;" +
                "}" +
                "h1, h2 { font-weight: normal; }" +
                "h1 { margin: 0 auto; padding: 0.15em; font-size: 10em; text-shadow: 0 2px 2px #000; }" +
                "h2 { margin-bottom: 2em; }" +
                "</style>" +
                "</head>" +
                "<body>";
        if (error_code == 1) {
            text_error += "<h1>⚠</h1>" +
                    "<h2>No connection to the internet</h2>" +
                    "<p style=\"margin: 10px;\">This Display has a connection to your network but no connection to the internet.</p>" +
                    "<p class=\"desc\" style=\"margin: 10px;\">The connection to the outside world is needed for updates and keeping the time.</p>";
        } else {
            text_error += "<h1>⚠</h1>" +
                    "<h2>Error</h2>" +
                    "<p style=\"margin: 10px;\">Unable to load page.</p>" +
                    "<p class=\"desc\" style=\"margin: 10px;\">Refresh the page.</p>";
        }
        text_error += "</body>" +
                "</html>";
        view_error_internet.loadDataWithBaseURL(null, text_error, "text/html", "utf-8", null);
        view_error_internet.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int progress) {
                progressBarShow(progressBar, progress);
            }
        });
    }

    public void openDeepLink(WebView view_data) {
        try {
            WebView.HitTestResult result = view_data.getHitTestResult();
            String data = result.getExtra();
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
            view_data.getContext().startActivity(intent);
        } catch (Exception ex) {
        }
    }

    public void openOtherApp(String url_intent) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url_intent)));
        } catch (Exception ex) {
        }
    }

    private class ProWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleUri(view, url);
        }

        @TargetApi(Build.VERSION_CODES.N)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleUri(view, request.getUrl().toString());
        }

        private boolean handleUri(WebView view, final String url) {
            if (url.startsWith("mailto:")) {
                Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
                startActivity(i);
                return true;
            } else if (url.startsWith("whatsapp://")) {
                openDeepLink(view);
                return true;
            } else if (url.startsWith("tel:")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception ex) {
                }
                return true;
            } else if (url.contains("youtube.com")) {
                openOtherApp(url);
                return true;
            } else if (url.contains("play.google.com/store/apps")) {
                openOtherApp(url);
                return true;
            } else if (url.startsWith("samsungpay://")) {
                openOtherApp(url);
                return true;
            } else if (url.startsWith("viber://")) {
                openDeepLink(view);
                return true;
            } else if (url.startsWith("tg://")) {
                openDeepLink(view);
                return true;
            } else if (url.startsWith("https://t.me")) {
                openOtherApp(url);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            SharedPreferences.Editor ed = sharedPreferences.edit();
            ed.putString("marap", url);
            ed.apply();
            swipe.setRefreshing(false);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            showPageErrorInternet(view, 0);
            swipe.setRefreshing(false);
        }
    }
}