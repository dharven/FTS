package com.dharven.fts.app;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
public class FTSApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("TAG", "onCreate: test");
        FirebaseApp.initializeApp(this);
    }
}
