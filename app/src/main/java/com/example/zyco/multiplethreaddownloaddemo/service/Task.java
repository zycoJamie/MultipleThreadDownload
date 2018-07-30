package com.example.zyco.multiplethreaddownloaddemo.service;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.example.zyco.multiplethreaddownloaddemo.service.entity.PackageEntity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;

/**
 * AsyncTask实现类 异步执行下载任务
 * PackageEntity参数里面封装了该任务所需的retrofit实例，以及该任务的ID
 * long参数是更新进度条的返回值
 * Integer参数是下载任务执行结果
 */
public class Task extends AsyncTask<PackageEntity,Long,Integer> {
    public static final int DOWNLOAD_FAIL=-1;
    public static final int DOWNLOAD_CANCEL=0;
    public static final int DOWNLOAD_PAUSE=1;
    public static final int DOWNLOAD_SUCCESS=2;
    private Retrofit mRetrofit=null;
    private CompositeDisposable mCompositeDisposable=new CompositeDisposable();
    private int taskId;
    private long contentLength;
    private long fileLength;
    private boolean isPause; //控制位：是否暂停下载
    private boolean isCancel; //控制位：是否取消下载
    private int result;
    private RandomAccessFile randomAccessFile; //能够读取文件的任意位置，将断点处开始下载的内容，写入文件指定位置
    private DownloadService.ChangeUIListener mListener;
    private long lastProgress;
    private DownloadService.DownloadResultListener listener;
    private String urlPath; //baseUrl的拼接path


    public Task(DownloadService.ChangeUIListener listener,String path){
        mListener=listener;
        urlPath=path;
    }

    public void onPause(){
        isPause=!isPause;
    }

    public void onCancel(){
        isCancel=!isCancel;
    }

    public void setDownloadListener(DownloadService.DownloadResultListener listener){
        this.listener=listener;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        switch(integer){
            case DOWNLOAD_SUCCESS:{
                if(listener!=null){
                    listener.success(taskId);
                }
                break;
            }
            case DOWNLOAD_PAUSE:{
                if(listener!=null){
                    listener.pause(taskId);
                }
                break;
            }
            case DOWNLOAD_FAIL:{
                if(listener!=null){
                    listener.fail(taskId);
                }
                break;
            }
            case DOWNLOAD_CANCEL:{
                if(listener!=null){
                    listener.cancel(taskId);
                }
                break;
            }
            default:{
                if(listener!=null){
                    listener.fail(taskId);
                }
            }
        }
    }

    /**
     * 参数类型为long，因为下载较大文件时，文件长度过大，int参数精度不够，造成进度条出现负值
     * @param values
     */
    @Override
    protected void onProgressUpdate(Long... values) {
        if(values[0]>lastProgress && mListener!=null){
            int progress= (int) (values[0]*100/(contentLength+fileLength));
            lastProgress=progress;
            mListener.updateUI(taskId,progress);
        }
    }

    @Override
    protected Integer doInBackground(PackageEntity... packageEntities) {
        Log.e("testZZ","后台下载开始!");
        mRetrofit=packageEntities[0].getRetrofit();
        taskId=packageEntities[0].getTaskId();
        String filePath= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        String fileName="software:"+taskId;
        File file=new File(filePath+"/"+fileName);
        try{
            if(!file.exists()){
                file.createNewFile();
            }
            randomAccessFile=new RandomAccessFile(file,"rw");
            long downloadLength=file.length();
            fileLength=file.length();
            //文件有内容 则继续下载 无内容 则从最开始下载
            executeDownload(downloadLength);
            return result;
        }catch(IOException e){
            e.printStackTrace();
        }
        return result;
    }


    private void executeDownload(final long downloadLength){
        if(mRetrofit==null){
            result=DOWNLOAD_SUCCESS; //下载成功后，该任务的retrofit实例已经销毁，所以单击暂停/开始后，进入该代码块提示下载已经完成
            return;
        }
        mCompositeDisposable.add(mRetrofit.create(RetrofitService.class).downloadSoft(urlPath,"bytes="+downloadLength+"-")
                .subscribeOn(Schedulers.trampoline()) //采用队列的数据结构，在该线程中执行
                .observeOn(Schedulers.trampoline())
                .subscribe(new Consumer<ResponseBody>() {
                    @Override
                    public void accept(ResponseBody responseBody) throws Exception {
                        contentLength=responseBody.contentLength(); //获取下载文件的大小
                        if (contentLength <= 0) {
                            result = DOWNLOAD_FAIL;
                        } else {
                            int readLen=0;
                            long progress= readLen+downloadLength;
                            byte[] b = new byte[1024];
                            randomAccessFile.seek(downloadLength);
                            InputStream is = responseBody.byteStream(); //获取字节流
                            while ((readLen = is.read(b)) != -1) {
                                if (isPause) {
                                    result = DOWNLOAD_PAUSE;
                                    break;
                                } else if (isCancel) {
                                    result = DOWNLOAD_CANCEL;
                                    break;
                                } else {
                                    randomAccessFile.write(b, 0, readLen); //写入文件
                                    result = DOWNLOAD_SUCCESS;
                                    progress= readLen+progress;
                                    publishProgress(progress); //更新进度
                                }
                            }
                            is.close(); //关闭流
                            responseBody.close();
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e("testZZ","throwable "+throwable.getMessage());
                        throwable.printStackTrace();
                    }
                }));
    }
}
