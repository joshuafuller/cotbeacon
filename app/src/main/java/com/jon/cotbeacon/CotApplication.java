package com.jon.cotbeacon;

import android.app.Application;

import org.jetbrains.annotations.NotNull;

import timber.log.Timber;

public class CotApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree() {
            @Override protected String createStackElementTag(@NotNull StackTraceElement element) {
                return "(" + element.getFileName() + ":" + element.getLineNumber() + ")#" + element.getMethodName();
            }
        });
    }
}
