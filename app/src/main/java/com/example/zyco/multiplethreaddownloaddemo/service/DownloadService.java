package com.example.zyco.multiplethreaddownloaddemo.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zyco.multiplethreaddownloaddemo.R;
import com.example.zyco.multiplethreaddownloaddemo.service.entity.PackageEntity;
import com.example.zyco.multiplethreaddownloaddemo.ui.MainActivity;

import java.io.File;
import java.util.HashMap;

/**
 * 用于开启异步处理任务(AsyncTask)的Service
 * tips:当多个线程下载同一文件时，会出现SocketException:Software cause connection abort
 */
public class DownloadService extends Service {
    private Context mContext;
    private RetrofitHelper mHelper=null;
    private HashMap<String,Task> taskList=new HashMap<>(); //每一个任务ID对应一个AsyncTask实现类(Task)
    private HashMap<String,View> views=new HashMap<>(); //每一个任务ID对应一个下载任务视图
    private HashMap<String,String> paths=new HashMap<>(); //每一个任务ID对应一个‘baseUrl拼接路径’
    private int taskNumbers=0; //任务数目，只增加不减，用该值设置每次新开任务的ID
    @Override
    public void onCreate() {
        super.onCreate();
        mHelper=RetrofitHelper.getInstance();
    }

    public void setContext(Context context){
        this.mContext=context;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private DownloadBinder mBinder=new DownloadBinder();

    public class DownloadBinder extends Binder{
        /**
         * 返回Service实例，主要作用是在activity处调用{@link #setContext(Context)}，
         * 传递activity上下文对象给service
         * @return
         */
        public DownloadService getService(){
            return DownloadService.this;
        }

        /**
         * 用于新增下载任务
         */
        public void newTask(){
            ++taskNumbers;
            //构造下载任务所需的 1.任务ID 2.retrofit实例 3.拼接path
            int urlIndex=mHelper.buildTask(taskNumbers);
            PackageEntity entity=new PackageEntity();
            entity.setTaskId(taskNumbers);
            entity.setRetrofit(mHelper.getRetrofitInstance(taskNumbers));
            paths.put(String.valueOf(taskNumbers),RetrofitHelper.paths[urlIndex]);

            if(mContext instanceof MainActivity){
                MainActivity activity= (MainActivity) mContext;
                LinearLayout mainLinearLayout=activity.findViewById(R.id.ll_task_list);
                //构造下载任务的视图，并添加到父视图中
                View view= LayoutInflater.from(activity).inflate(R.layout.task_item,mainLinearLayout,false);
                ViewGroup.LayoutParams lp=view.getLayoutParams();
                lp.width=ViewGroup.LayoutParams.MATCH_PARENT;
                lp.height=ViewGroup.LayoutParams.WRAP_CONTENT;
                view.setLayoutParams(lp);
                initChildView(view,taskNumbers);
                mainLinearLayout.addView(view);
                views.put(String.valueOf(taskNumbers),view);

                Task task=new Task(new ChangeUI(),paths.get(String.valueOf(taskNumbers)));
                taskList.put(String.valueOf(taskNumbers),task);
                task.setDownloadListener(mDownloadResultListener);
                //AsyncTask默认是串行处理任务，设置THREAD_POOL_EXECUTOR能够并行处理
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,entity);
            }
        }

        /**
         * 暂停后，从断点出开始继续下载
         * @param taskId
         */
        public void start(int taskId){
            if(taskList!=null){
                PackageEntity entity=new PackageEntity();
                entity.setTaskId(taskId);
                entity.setRetrofit(mHelper.getRetrofitInstance(taskId));
                //new 一个新的Task，但仍然使用暂停前的retrofit实例，相同的ID，相同的拼接path
                Task restartTask=new Task(new ChangeUI(),paths.get(String.valueOf(taskId)));
                taskList.put(String.valueOf(taskId),restartTask);
                restartTask.setDownloadListener(mDownloadResultListener);
                restartTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,entity);
            }
        }

        /**
         * 暂停下载任务
         * @param taskId
         */
        public void pause(int taskId){
            if(taskList!=null && taskList.size()>0){
                Task task=taskList.get(String.valueOf(taskId));
                if(task!=null){
                    task.onPause(); //改变task对象中控制位isPause的值
                }
            }
        }

        /**
         * 取消下载任务
         * @param taskId
         */
        public void cancel(int taskId){
            if(taskList!=null){
                Task task=taskList.get(String.valueOf(taskId));
                if(task!=null){
                    task.onCancel(); //改变task对象中控制位isCancel的值
                    taskList.remove(String.valueOf(taskId));
                }else{
                    deleteFileAndView(taskId);
                }
            }
        }
    }

    /**
     * 更新对应任务的下载进度条
     */
    public class ChangeUI implements ChangeUIListener{
        @Override
        public void updateUI(int taskId,int progress) {
            View view=views.get(String.valueOf(taskId));
            ProgressBar pb=view.findViewById(R.id.pb_progress_bar);
            pb.setProgress(progress);
            TextView tvProgress=view.findViewById(R.id.tv_progress_text);
            tvProgress.setText(String.format("%s",progress).concat("%"));
        }
    }

    interface ChangeUIListener{
        void updateUI(int taskId,int progress);
    }

    /**
     * 下载结果回调
     */
    private DownloadResultListener mDownloadResultListener=new DownloadResultListener() {
        @Override
        public void success(int taskId) {
            if(taskList!=null && taskList.size()>0){
                taskList.remove(String.valueOf(taskId));
                mHelper.removeRetrofitInstance(taskId);
                Toast.makeText(mContext,String.format("任务%s已经下载完成!",taskId),Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void fail(int taskId) {
            if(taskList!=null && taskList.size()>0){
                taskList.remove(String.valueOf(taskId));
                mHelper.removeRetrofitInstance(taskId);
                views.remove(String.valueOf(taskId));
                Toast.makeText(mContext,String.format("任务%s下载失败!",taskId),Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void cancel(int taskId) {
            if(taskList!=null){
                deleteFileAndView(taskId);
            }
        }

        @Override
        public void pause(int taskId) {
            if(taskList!=null && taskList.size()>0){
                taskList.remove(String.valueOf(taskId));
                Toast.makeText(mContext,String.format("任务%s已经暂停!",taskId),Toast.LENGTH_SHORT).show();
            }
        }
    };

    interface DownloadResultListener{
        void success(int taskId);
        void fail(int taskId);
        void cancel(int taskId);
        void pause(int taskId);
    }

    private void initChildView(View view, final int taskId){
        TextView mTaskName=view.findViewById(R.id.tv_task_name);
        mTaskName.setText(String.format("下载任务:%s",taskId));
        Button mStartButton=view.findViewById(R.id.btn_startOrPause);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v instanceof Button){
                    Button btn= (Button) v;
                    if(btn.getText().equals("开始")){
                        btn.setText("暂停");
                        mBinder.start(taskId);
                    }else{
                        btn.setText("开始");
                        mBinder.pause(taskId);
                    }
                }
            }
        });
        Button mCancelButton=view.findViewById(R.id.btn_cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBinder.cancel(taskId);
            }
        });
    }

    /**
     * 删除下载文件和在父视图中的view
     * @param taskId
     */
    private void deleteFileAndView(int taskId){
        String filePath= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        String fileName="software:"+taskId;
        File file=new File(filePath+"/"+fileName);
        if(file.exists()) {
            file.delete();
            taskList.remove(String.valueOf(taskId));
            mHelper.removeRetrofitInstance(taskId);
            Toast.makeText(mContext,String.format("任务%s已经取消!",taskId),Toast.LENGTH_SHORT).show();
            View cancelView=views.get(String.valueOf(taskId));
            if(mContext instanceof MainActivity) {
                MainActivity activity = (MainActivity) mContext;
                LinearLayout mainLinearLayout = activity.findViewById(R.id.ll_task_list);
                mainLinearLayout.removeView(cancelView);
            }
            views.remove(String.valueOf(taskId));
        }
    }
}
