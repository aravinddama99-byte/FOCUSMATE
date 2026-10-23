package com.focusmate.lms;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.util.*;

public class MainActivity extends Activity {
    private WebView web;
    private FocusStore store;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new FocusStore(this);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);

        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                return loader.shouldInterceptRequest(r.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if ("appassets.androidplatform.net".equals(r.getUrl().getHost())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (Exception ignored) {}
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "FocusBridge");
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }

    private boolean serviceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        String me = new ComponentName(this, BlockerService.class).flattenToString();
        String short_ = new ComponentName(this, BlockerService.class).flattenToShortString();
        for (String x : enabled.split(":")) if (x.equalsIgnoreCase(me) || x.equalsIgnoreCase(short_)) return true;
        return false;
    }

    private void js(final String code) { runOnUiThread(() -> web.evaluateJavascript(code, null)); }

    public class Bridge {
        @JavascriptInterface public String status() {
            try {
                JSONObject o = new JSONObject();
                o.put("serviceEnabled", serviceEnabled());
                o.put("active", store.active());
                o.put("endAt", store.endAt());
                o.put("strict", store.strict());
                o.put("now", System.currentTimeMillis());
                o.put("blocked", new JSONArray(store.blocked()));
                return o.toString();
            } catch (Exception e) { return "{}"; }
        }

        /** Loads installed launchable apps off the UI thread, then calls window.__fmApps(json). */
        @JavascriptInterface public void loadApps() {
            new Thread(() -> {
                try {
                    PackageManager pm = getPackageManager();
                    Intent q = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
                    List<ResolveInfo> list = pm.queryIntentActivities(q, 0);
                    Set<String> seen = new HashSet<>();
                    JSONArray arr = new JSONArray();
                    List<JSONObject> tmp = new ArrayList<>();
                    for (ResolveInfo ri : list) {
                        String pkg = ri.activityInfo.packageName;
                        if (pkg.equals(getPackageName()) || !seen.add(pkg)) continue;
                        JSONObject o = new JSONObject();
                        o.put("pkg", pkg);
                        o.put("name", String.valueOf(ri.loadLabel(pm)));
                        o.put("icon", iconB64(ri.loadIcon(pm)));
                        tmp.add(o);
                    }
                    Collections.sort(tmp, (a, c) -> a.optString("name").compareToIgnoreCase(c.optString("name")));
                    for (JSONObject o : tmp) arr.put(o);
                    js("window.__fmApps(" + JSONObject.quote(arr.toString()) + ")");
                } catch (Exception e) { js("window.__fmApps('[]')"); }
            }).start();
        }

        @JavascriptInterface public void setBlocked(String json) {
            try {
                JSONArray a = new JSONArray(json);
                Set<String> s = new HashSet<>();
                for (int i = 0; i < a.length(); i++) s.add(a.getString(i));
                store.setBlocked(s);
            } catch (Exception ignored) {}
        }

        @JavascriptInterface public boolean start(int minutes, boolean strict) {
            if (!serviceEnabled() || minutes <= 0) return false;
            store.start(minutes, strict);
            return true;
        }

        @JavascriptInterface public boolean stop() { return store.stop(); }

        @JavascriptInterface public void openAccessibilitySettings() {
            Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    }

    private static String iconB64(Drawable d) {
        int sz = 96;
        Bitmap bm = Bitmap.createBitmap(sz, sz, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        d.setBounds(0, 0, sz, sz);
        d.draw(c);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 90, bo);
        return "data:image/png;base64," + Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP);
    }
}
