package com.iot.doorunlocker.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.google.gson.GsonBuilder;
import com.iot.doorunlocker.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class DataRepository implements DataRepositoryImpl {

    private static final int SOCKET_TIMEOUT = 30; // sec
    private static final int DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB

    private static final float RECOGNITION_COEFF = 0.8f;

    private static final String PHOTO_GROUP_NAME = "ltst";

    private static final String CATALOG_PATH =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/tmp_photo";

    private RestService mRestService;

    private LinkedList<Call> mCalls = new LinkedList<>();

    private int mCounter;

    public DataRepository(Context context) {

        File cacheDir = new File(context.getApplicationContext().getCacheDir(), "http");
        Cache cache = new Cache(cacheDir, DISK_CACHE_SIZE);

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(final String message) {
                        Timber.tag("OkHttp").v(message);
                    }
                })
                .setLevel(HttpLoggingInterceptor.Level.BASIC);

        CurlLoggingInterceptor curlLoggingInterceptor = new CurlLoggingInterceptor(
                new HttpLoggingInterceptor.Logger() {
                    @Override
                    public void log(final String message) {
                        Timber.tag("Curl").v(message);
                    }
                });

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(SOCKET_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(SOCKET_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(SOCKET_TIMEOUT, TimeUnit.SECONDS)
                .cache(cache)
                .addInterceptor(loggingInterceptor)
//                .addInterceptor(curlLoggingInterceptor)
                .build();

        Retrofit retrofit = new Retrofit.Builder() //
                .client(client) //
                .baseUrl(BuildConfig.REST_API_ENDPOINT) //
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create())) //
                .build();

        mRestService = retrofit.create(RestService.class);
    }

    public void cancelAllCalls() {
        while (mCalls.size() > 0) {
            Call call = mCalls.poll();
            if (call != null) {
                call.cancel();
            }
        }
        mCounter = 0;
        File fileDir = new File(CATALOG_PATH);
        if (fileDir.exists()) {
            fileDir.delete();
        }
    }


    synchronized public void sendPhotoMessage(@NonNull final Bitmap bitmap,
                                              @NonNull final Handler mainHandler,
                                              @NonNull final OnRecognitionListener callback) {

        String descriptionString = "message photo";
        RequestBody description = RequestBody.create(MediaType.parse("multipart/form-data"), descriptionString);

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part body = getMultiPartBody(bitmap, descriptionString, "messagePhoto_" + mCounter++);

        Call<List<RecognizeResponse>> call = mRestService.sendPhoto(description, PHOTO_GROUP_NAME, body);
        mCalls.add(call);

        Response<List<RecognizeResponse>> response = null;
        try {
            response = call.execute();
        } catch (IOException e) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onError(e);
                }
            });
            mCalls.remove(call);
            return;
        } finally {
            mCalls.remove(call);
        }

        Timber.d("Response: %s", response);

        if (response == null || response.code() == 500 || response.body() == null) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onDisallowed();
                }
            });
            return;
        }

        boolean isRecognized = false;
        for (final RecognizeResponse r : response.body()) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onQuality(r.getDistance());
                }
            });
            if (r.getDistance() < RECOGNITION_COEFF) {

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onRecognized();
                    }
                });

                isRecognized = true;
                break;
            }
        }

        if (!isRecognized) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onDisallowed();
                }
            });
        }
    }

    private MultipartBody.Part getMultiPartBody(Bitmap bitmap, String description, String tmpFileName) {

        File fileDir = new File(CATALOG_PATH);
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }

        File file = new File(fileDir, tmpFileName + ".jpg");
        if (file.exists()) {
            file.delete();
        }

        FileOutputStream out = null;

        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            Timber.e(e, "Can't write bitmap to FileOutputStream.");
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                Timber.e(e, "Can't close FileOutputStream.");
            }
        }

        // create RequestBody instance from file
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", description, requestFile);

        return body;
    }
}
