package com.lums.narl.talkingFields.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class LocaleUtils {

    private LocaleUtils(){

    }

    public static void setLocale(String language, Context mContext){
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();

//        Configuration config = mContext.getResources().getConfiguration();

        config.locale = locale;
        mContext.getResources().updateConfiguration(config,mContext.getResources().getDisplayMetrics());
//        getBaseContext().getResources().updateConfiguration(config,getResources().getDisplayMetrics());

        SharedPreferences.Editor mEditor = mContext.getSharedPreferences("Settings", MODE_PRIVATE).edit();
        mEditor.putString("MyLang",language);
        mEditor.apply();

    }

    public static void loadLocale(Context mContext){
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("Settings", MODE_PRIVATE);
        String language = sharedPreferences.getString("MyLang","");
        setLocale(language, mContext);
    }

    public static void getLocale(){

    }

    public static boolean isNetworkAvailable(Context mContext) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
