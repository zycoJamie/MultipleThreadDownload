package com.example.zyco.multiplethreaddownloaddemo.service;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Streaming;

public interface RetrofitService {
    @Streaming //retrofit默认是将下载文件全部载入内存，使用@Streaming 能使得retrofit不必全部加载文件进内存，一边下载一边写入文件
    @GET("{path}")
    Observable<ResponseBody> downloadSoft(@Path("path") String path, @Header("RANGE") String bytes);
}
