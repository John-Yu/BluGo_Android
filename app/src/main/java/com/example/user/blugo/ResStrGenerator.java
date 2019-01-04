package com.example.user.blugo;

import android.content.Context;

/**
 * Created by user on 2016-07-06.
 */
public class ResStrGenerator {
    private static ResStrGenerator instance;

    private ResStrGenerator() {

    }

    public static synchronized ResStrGenerator getInstance() {
        if (instance == null) {
            instance = new ResStrGenerator();
        }
        return instance;
    }

    public String get_res_string(int resId) {
        return App.getAppContext().getString(resId);
    }
}
