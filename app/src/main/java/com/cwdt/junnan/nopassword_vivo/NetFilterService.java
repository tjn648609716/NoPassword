package com.cwdt.junnan.nopassword_vivo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;

import java.io.IOException;
import java.util.List;

public class NetFilterService extends VpnService {
    private ParcelFileDescriptor vpn = null;
    private static final String EXTRA_COMMAND = "Command";
    private String name = "com.android.packageinstaller";

    private enum Command {start, reload, stop}

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get enabled
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean enabled = prefs.getBoolean("enabled", false);

        // Get command
        Command cmd = (intent == null ? Command.start : (Command) intent.getSerializableExtra(EXTRA_COMMAND));

        // Process command
        switch (cmd) {
            case start:
                if (enabled && vpn == null)
                    vpn = vpnStart();
                break;

            case reload:
                // Seamless handover
                ParcelFileDescriptor prev = vpn;
                if (enabled)
                    vpn = vpnStart();
                if (prev != null)
                    vpnStop(prev);
                break;

            case stop:
                if (vpn != null) {
                    vpnStop(vpn);
                    vpn = null;
                }
                stopSelf();
                break;
        }

        return START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ParcelFileDescriptor vpnStart() {

        // Build VPN service
        final Builder builder = new Builder();
        builder.setSession(getString(R.string.app_name));
        builder.addAddress("10.1.10.1", 32);
        builder.addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128);
        builder.addRoute("0.0.0.0", 0);
        builder.addRoute("0:0:0:0:0:0:0:0", 0);

        // Add list of allowed applications
        for (PackageInfo appInfo : getApps(this))
            if (!name.equals(appInfo.packageName)) {
                try {
                    builder.addDisallowedApplication(appInfo.packageName);
                } catch (PackageManager.NameNotFoundException ex) {
                }
            }

        // Build configure intent
        Intent configure = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, configure, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setConfigureIntent(pi);

        // Start VPN service
        try {
            return builder.establish();
        } catch (Throwable ex) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putBoolean("enabled", false).apply();
            return null;
        }
    }

    public static List<PackageInfo> getApps(Context context) {
        return context.getPackageManager().getInstalledPackages(0);
    }

    private void vpnStop(ParcelFileDescriptor pfd) {
        try {
            pfd.close();
        } catch (IOException ex) {

        }
    }

    private BroadcastReceiver connectivityChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(ConnectivityManager.EXTRA_NETWORK_TYPE) &&
                    intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, ConnectivityManager.TYPE_DUMMY) == ConnectivityManager.TYPE_WIFI)
                reload(null, NetFilterService.this);
        }
    };

    private BroadcastReceiver packageAddedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reload(null, NetFilterService.this);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // Listen for connectivity updates
        IntentFilter ifConnectivity = new IntentFilter();
        ifConnectivity.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityChangedReceiver, ifConnectivity);

        // Listen for added applications
        IntentFilter ifPackage = new IntentFilter();
        ifPackage.addAction(Intent.ACTION_PACKAGE_ADDED);
        ifPackage.addDataScheme("package");
        registerReceiver(packageAddedReceiver, ifPackage);
    }

    @Override
    public void onDestroy() {

        if (vpn != null) {
            vpnStop(vpn);
            vpn = null;
        }

        unregisterReceiver(connectivityChangedReceiver);
        unregisterReceiver(packageAddedReceiver);

        super.onDestroy();
    }

    @Override
    public void onRevoke() {

        if (vpn != null) {
            vpnStop(vpn);
            vpn = null;
        }

        // Disable firewall
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("enabled", false).apply();

        super.onRevoke();
    }

    public static void start(Context context) {
        Intent intent = new Intent(context, NetFilterService.class);
        intent.putExtra(EXTRA_COMMAND, Command.start);
        context.startService(intent);
    }

    public static void reload(String network, Context context) {
        if (network == null || ("wifi".equals(network) == NetworkUtil.isWifiActive(context))) {
            Intent intent = new Intent(context, NetFilterService.class);
            intent.putExtra(EXTRA_COMMAND, Command.reload);
            context.startService(intent);
        }
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, NetFilterService.class);
        intent.putExtra(EXTRA_COMMAND, Command.stop);
        context.startService(intent);
    }
}
