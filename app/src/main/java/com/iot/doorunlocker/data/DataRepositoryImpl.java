package com.iot.doorunlocker.data;

import android.graphics.Bitmap;
import android.os.Handler;
import android.support.annotation.NonNull;

public interface DataRepositoryImpl {

    void sendPhotoMessage(@NonNull final Bitmap bitmap,
                          @NonNull final Handler mainHandler,
                          @NonNull final OnRecognitionListener callback);
    void cancelAllCalls();

    public interface OnRecognitionListener {
        public void onRecognized();
        public void onDisallowed();
        // TODO remove this param
        public void onQuality(double quality);
        public void onError(Throwable t);
    }
}
