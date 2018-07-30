package com.example.zyco.multiplethreaddownloaddemo.service;

import android.util.Log;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit请求工具类
 * 采用单例模式
 */
public class RetrofitHelper {
    //四个baseUrl
    private static String[] urls={"http://down.360safe.com/",
            "http://msoftdl.360.cn/360batterydoctor/",
            "http://down.360safe.com/360so/",
            "http://down.360safe.com/360ra/"};
    //四个与baseUrl拼接的路径
    public static String[] paths={"instmobilemgr.exe",
            "360batterydoctor_home.apk",
            "360so_android_formal.apk",
            "360bangbang_2.6.0.0325_guanwang.apk"};
    private Random random=new Random();
    private GsonConverterFactory gsonConverterFactory=GsonConverterFactory.create(new Gson());
    private static RetrofitHelper instance;
    private HashMap<String,Retrofit> mRetrofits =new HashMap<>();
    public static RetrofitHelper getInstance(){
        if(instance==null){
            synchronized(RetrofitHelper.class){
                instance=new RetrofitHelper();
            }
        }
        return instance;
    }
    private RetrofitHelper(){

    }

    /**
     * 每次构建一个下载任务，以下载任务的ID作为Key，从四个baseUrl随机选择一个下载地址，构建一个retrofit实例，
     * 以retrofit实例作为Value，添加在Map中。
     * @param taskId
     * @return
     */
    public int buildTask(int taskId){
        int urlIndex=random.nextInt(urls.length);
        mRetrofits.put(String.valueOf(taskId),new Retrofit.Builder()
                .baseUrl(urls[urlIndex])
                .client(new OkHttpClient.Builder()
                        .retryOnConnectionFailure(true)
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build())
                .addConverterFactory(gsonConverterFactory)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build());
        return urlIndex;
    }

    /**
     * 根据任务id返回不同的Retrofit实例
     * @param taskId
     * @return
     */
    public Retrofit getRetrofitInstance(int taskId){
        return mRetrofits.get(String.valueOf(taskId));
    }

    /**
     * 根据任务ID删除对应的网络请求实例
     * @param taskId
     */
    public void removeRetrofitInstance(int taskId){
        mRetrofits.remove(String.valueOf(taskId));
    }
}
