package com.mohit.installer;

import android.app.Application;

import com.downloader.PRDownloader;

public class BaseApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PRDownloader.initialize(getApplicationContext());
    }
}
