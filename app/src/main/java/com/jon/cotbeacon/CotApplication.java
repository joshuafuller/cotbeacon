package com.jon.cotbeacon;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

public class CotApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        /* Enable night mode */
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        /* Initialise logging */
        Timber.plant(new Timber.DebugTree() {
            @Override protected String createStackElementTag(@NotNull StackTraceElement element) {
                return "(" + element.getFileName() + ":" + element.getLineNumber() + ")#" + element.getMethodName();
            }
        });
    }
}
