package com.cwdt.junnan.nopassword_vivo;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.net.VpnService;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qmuiteam.qmui.widget.dialog.QMUITipDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int REQUEST_VPN = 1;


    private LinearLayout white_l;
    private LinearLayout open_l;
    private LinearLayout setting_l;
    private ImageView open_img;
    private TextView open_text;
    private TextView content_text;
    private TextView upgrade_text;
    private SharedPreferences prefs;
    private RelativeLayout upgrade_r;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        white_l = findViewById(R.id.white_l);
        open_l = findViewById(R.id.open_l);
        setting_l = findViewById(R.id.setting_l);

        open_img = findViewById(R.id.open_img);
        open_text = findViewById(R.id.open_text);
        content_text = findViewById(R.id.content_text);
        upgrade_text = findViewById(R.id.upgrade_text);
        upgrade_r = findViewById(R.id.upgrade_r);


        white_l.setOnClickListener(onClickListener);
        open_l.setOnClickListener(onClickListener);
        setting_l.setOnClickListener(onClickListener);
        upgrade_text.setOnClickListener(onClickListener);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean enabled = prefs.getBoolean("enabled", false);
        prefs.registerOnSharedPreferenceChangeListener(this);
        open_img.setImageResource(enabled ? R.drawable.stop : R.drawable.open);
        open_text.setText(enabled ? "停止" : "开启");
        GetVersion();

    }

    private void GetVersion() {
        String urlString = "https://raw.githubusercontent.com/tjn648609716/resources/master/NoPasswordVersion.json";
        GetVersionAsyncTask version = new GetVersionAsyncTask();
        version.execute(urlString);

    }

    public void PlayWith(View view, final float toY, long durationMillis, final boolean open) {
        ObjectAnimator anim3;
        if (open) {
            anim3 = ObjectAnimator.ofFloat(view, "translationY", toY);
        } else {
            anim3 = ObjectAnimator.ofFloat(view, "translationY", 0);
        }
        AnimatorSet animSet = new AnimatorSet();
        animSet.play(anim3);
        animSet.setDuration(durationMillis);
        animSet.start();
    }

    @SuppressLint("StaticFieldLeak")
    private class GetVersionAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            String result = "";
            try {
                String url = params[0];
                Request request = new Request.Builder().url(url).build();
                OkHttpClient client = new OkHttpClient();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    return response.body().string();
                } else {
                    throw new IOException("Unexpected code " + response);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return result;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            try {
                JSONObject upgrade = new JSONObject(result);
                final single_upgrade_Info upgrade_info = new single_upgrade_Info();
                upgrade_info.fromJson(upgrade);


                try {
                    PackageManager pm = MainActivity.this.getPackageManager();
                    PackageInfo pi = pm.getPackageInfo(MainActivity.this.getPackageName(), 0);
                    float local_version = Float.valueOf(pi.versionName);
                    float net_version = Float.valueOf(upgrade_info.versionName);
                    if (net_version > local_version) {
                        content_text.setText("监测到新版本   v" + net_version);
                        PlayWith(upgrade_r, dip2px(50), 500, true);

                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.white_l:
                    IntentWrapper.whiteListMatters(MainActivity.this, "拒绝密码的持续运行");
                    break;
                case R.id.open_l:
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    boolean enabled = prefs.getBoolean("enabled", false);
                    if (enabled) {
                        prefs.edit().putBoolean("enabled", false).apply();
                        NetFilterService.stop(MainActivity.this);
                    } else {
                        Intent prepare = VpnService.prepare(MainActivity.this);
                        if (prepare == null) {
                            onActivityResult(REQUEST_VPN, RESULT_OK, null);
                        } else {
                            try {
                                startActivityForResult(prepare, REQUEST_VPN);
                            } catch (Throwable ex) {
                                onActivityResult(REQUEST_VPN, RESULT_CANCELED, null);
                            }
                        }
                    }
                    break;
                case R.id.setting_l:
                    final QMUITipDialog tipDialog = new QMUITipDialog.Builder(MainActivity.this)
                            .setIconType(QMUITipDialog.Builder.ICON_TYPE_INFO)
                            .setTipWord("没得设置")
                            .create();
                    tipDialog.show();
                    v.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            tipDialog.dismiss();
                        }
                    }, 1500);
                    break;

                case R.id.upgrade_text:
                    Uri uri = Uri.parse("https://www.coolapk.com/apk/com.cwdt.junnan.nopassword_vivo");
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    PackageManager pm = getPackageManager();
                    List<ResolveInfo> resolveList = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);

                    if (resolveList.size() > 0) {
                        String title = "请选择浏览器";
                        Intent intentChooser = Intent.createChooser(intent, title);
                        startActivity(intentChooser);
                    }
                    break;
            }
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String name) {
        if ("enabled".equals(name)) {
            boolean enabled = prefs.getBoolean(name, false);
            open_img.setImageResource(enabled ? R.drawable.stop : R.drawable.open);
            open_text.setText(enabled ? "停止" : "开启");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_VPN) {
            prefs.edit().putBoolean("enabled", resultCode == RESULT_OK).apply();

            if (resultCode == RESULT_OK)
                NetFilterService.start(this);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

    public int dip2px(float dpValue) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
