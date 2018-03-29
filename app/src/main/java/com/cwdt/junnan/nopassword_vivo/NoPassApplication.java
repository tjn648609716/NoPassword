package com.cwdt.junnan.nopassword_vivo;

import android.app.Application;
import android.content.Context;

public class NoPassApplication extends Application {
    private static NoPassApplication mInstance = null;

    public Context getContext() {
        return mInstance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }


    public static NoPassApplication getInstance() {
        return mInstance;
    }

}
