package com.iot.doorunlocker.data;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface RestService {

    @Multipart
    @POST("faces/identify")
    Call<List<RecognizeResponse>> sendPhoto(
            @Part("filedata") RequestBody description,
            @Query("group_id") String groupId,
            @Part MultipartBody.Part file);
}
