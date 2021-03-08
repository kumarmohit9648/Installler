package com.mohit.installer;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface Api {
    @GET()
    @Streaming
    Call<ProgressResponseBody> getApk(@Url String url);
}
