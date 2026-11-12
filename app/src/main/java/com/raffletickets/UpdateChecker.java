package com.raffletickets;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Checks GitHub Releases for a newer APK and installs it in place. */
public class UpdateChecker {

    private static final String REPO = "Predator04/raffle-tickets";
    private static final String API = "https://api.github.com/repos/" + REPO + "/releases/latest";
    private static final long COOLDOWN_MS = 12L * 3600_000L; // 12 hours

    public static void check(final Activity activity, final boolean manual) {
        final SharedPreferences sp = activity.getSharedPreferences("updater", Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (!manual && now - sp.getLong("last_check", 0) < COOLDOWN_MS) return;
        sp.edit().putLong("last_check", now).apply();

        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(API).openConnection();
                c.setRequestProperty("User-Agent", "RaffleTickets-Updater");
                c.setConnectTimeout(10000);
                c.setReadTimeout(10000);
                if (c.getResponseCode() != 200) {
                    if (manual) toast(activity, "Couldn't reach updates (HTTP " + c.getResponseCode() + ")");
                    return;
                }
                InputStream is = c.getInputStream();
                StringBuilder sb = new StringBuilder();
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) sb.append(new String(buf, 0, n));
                is.close();

                JSONObject rel = new JSONObject(sb.toString());
                String tag = rel.getString("tag_name").replaceFirst("^v", "");
                final int latest = Integer.parseInt(tag);
                final int current = BuildConfig.VERSION_CODE;

                if (latest <= current) {
                    if (manual) toast(activity, "You're up to date (v" + current + ")");
                    return;
                }
                if (!manual && sp.getInt("skipped", -1) == latest) return;

                String apkUrl = null;
                JSONArray assets = rel.getJSONArray("assets");
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject a = assets.getJSONObject(i);
                    if (a.getString("name").endsWith(".apk")) {
                        apkUrl = a.getString("browser_download_url");
                        break;
                    }
                }
                if (apkUrl == null) {
                    if (manual) toast(activity, "No APK in the latest release");
                    return;
                }

                final String urlF = apkUrl;
                activity.runOnUiThread(() -> prompt(activity, latest, urlF));
            } catch (Exception e) {
                if (manual) toast(activity, "Update check failed: " + e.getMessage());
            }
        }).start();
    }

    private static void prompt(final Activity activity, final int latest, final String apkUrl) {
        new AlertDialog.Builder(activity)
                .setTitle("Update available")
                .setMessage("Version " + latest + " is available. Download and install?")
                .setPositiveButton("Update", (d, w) -> download(activity, apkUrl))
                .setNegativeButton("Later", null)
                .setNeutralButton("Skip this version", (d, w) ->
                        activity.getSharedPreferences("updater", Context.MODE_PRIVATE)
                                .edit().putInt("skipped", latest).apply())
                .show();
    }

    private static void download(final Activity activity, final String apkUrl) {
        toast(activity, "Downloading update...");
        new Thread(() -> {
            try {
                File dir = new File(activity.getCacheDir(), "updates");
                dir.mkdirs();
                File apk = new File(dir, "raffle-tickets.apk");
                HttpURLConnection c = (HttpURLConnection) new URL(apkUrl).openConnection();
                c.setRequestProperty("User-Agent", "RaffleTickets-Updater");
                c.setConnectTimeout(20000);
                c.setReadTimeout(60000);
                InputStream is = c.getInputStream();
                FileOutputStream fos = new FileOutputStream(apk);
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
                fos.close();
                is.close();

                activity.runOnUiThread(() -> install(activity, apk));
            } catch (Exception e) {
                toast(activity, "Download failed: " + e.getMessage());
            }
        }).start();
    }

    private static void install(Activity activity, File apk) {
        Uri uri = FileProvider.getUriForFile(activity,
                activity.getPackageName() + ".fileprovider", apk);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, "application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(i);
    }

    private static void toast(final Activity activity, final String msg) {
        activity.runOnUiThread(() -> Toast.makeText(activity, msg, Toast.LENGTH_LONG).show());
    }
}
