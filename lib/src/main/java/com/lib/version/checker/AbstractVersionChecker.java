package com.lib.version.checker;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

public abstract class AbstractVersionChecker {
    private final String latestVersionFileUrl;
    private final String newVersionWebpageUrl;
    
    protected final Activity activity;

    protected AbstractVersionChecker(Activity activity) {
        this.activity = activity;
        latestVersionFileUrl = latestVersionFileWebUrl();
        newVersionWebpageUrl = NewVersionWebpageUrl();
    }

    protected abstract String NewVersionWebpageUrl();
    protected abstract String latestVersionFileWebUrl();

    public void checkVersionAsynchronously() {
        new Thread(this::checkVersion).start();
    }

    public void checkVersion() {
        try {
            Log.d("avc-vc-lib", "Checking for newer versions...");
            String latestVersion = requestVersionFromServer();
            Log.d("avc-vc-lib", "Version from server: "+latestVersion);
            String appVersion = getApplicationVersion();
            Log.d("avc-vc-lib", "App version: "+latestVersion);
            if(needsUpdate(appVersion, latestVersion))
                showUpdateDialog();
            Log.d("avc-vc-lib", "Check - complete.");
        } catch (Exception e) {
            // causes: no internet, DNS problem or server is closed
            Log.d("avc-vc-lib", "Could not check for newer versions cause: "+e.getClass().getSimpleName()+", message: "+e.getMessage());
        }
    }

    private boolean needsUpdate(String appV, String latV) {
        double latestVersion = Double.parseDouble(latV);
        double appVersion = Double.parseDouble(appV);
        return appVersion < latestVersion;
    }

    private String getApplicationVersion() throws PackageManager.NameNotFoundException {
        PackageManager pm = activity.getPackageManager();
        PackageInfo info = pm.getPackageInfo(activity.getPackageName(), 0);
        if(info==null||info.versionName==null)
            throw new NullPointerException("Cannot get version code. Null returned by android system.");
        return info.versionName;
    }

    private String requestVersionFromServer() throws IOException {
        URL url = URI.create(latestVersionFileUrl).toURL();
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        String versionStr = br.readLine();
        br.close();
        return versionStr;
    }

    private void showUpdateDialog() {
        activity.runOnUiThread(() -> new MaterialAlertDialogBuilder(activity)
                .setTitle("Update available!")
                .setMessage("Do you want to update?")
                .setCancelable(false)
                .setNegativeButton("Not now.", (a, b) -> a.dismiss())
                .setPositiveButton("Yes!", (a, b) -> {
                    a.dismiss();
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(newVersionWebpageUrl));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                })
                .show());
    }
}
