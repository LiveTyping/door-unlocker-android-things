package com.iot.doorunlocker;

import android.app.Application;

import com.iot.doorunlocker.data.DataRepository;
import com.iot.doorunlocker.data.DataRepositoryImpl;

import timber.log.Timber;

public class ProjectApplication extends Application {

    private static ProjectApplication mApplication;

    private DataRepository mDataRepository;

    @Override
    public void onCreate() {
        super.onCreate();

        Timber.plant(new Timber.DebugTree());

        mApplication = this;

        mDataRepository = new DataRepository(this);
    }

    public static DataRepositoryImpl getDataRepository() {
        return mApplication.mDataRepository;
    }
}
