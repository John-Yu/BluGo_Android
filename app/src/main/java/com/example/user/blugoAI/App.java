package com.example.user.blugoAI;

import android.app.Application;
import android.content.Context;

/**
 * Created by user on 2016-07-06.
 */
public class App extends Application {
    private static Context appContext;
    public Leela leela;
    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;
    }

    public static Context getAppContext() {
        return appContext;
    }
}
