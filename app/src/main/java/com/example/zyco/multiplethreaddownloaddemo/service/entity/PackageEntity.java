package com.example.zyco.multiplethreaddownloaddemo.service.entity;

import retrofit2.Retrofit;

/**
 * 封装Retrofit和任务ID
 * 每一个下载任务对应一个retrofit实例
 */
public class PackageEntity {
    private Retrofit retrofit;
    private int taskId;

    public Retrofit getRetrofit() {
        return retrofit;
    }

    public void setRetrofit(Retrofit retrofit) {
        this.retrofit = retrofit;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }
}
