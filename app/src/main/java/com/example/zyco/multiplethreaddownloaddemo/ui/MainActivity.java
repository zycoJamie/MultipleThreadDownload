package com.example.zyco.multiplethreaddownloaddemo.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.zyco.multiplethreaddownloaddemo.R;
import com.example.zyco.multiplethreaddownloaddemo.service.DownloadService;

public class MainActivity extends AppCompatActivity {
    private Button mNewTaskButton;
    private DownloadService.DownloadBinder binder;
    private DownloadService service;
    private ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if(service instanceof DownloadService.DownloadBinder){
                binder= (DownloadService.DownloadBinder) service;
                MainActivity.this.service=binder.getService();
                MainActivity.this.service.setContext(MainActivity.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermission();
        mNewTaskButton=findViewById(R.id.btn_new_task);
        mNewTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binder.newTask();
                Toast.makeText(MainActivity.this, "开始下载任务", Toast.LENGTH_SHORT).show();
            }
        });
        Intent intent=new Intent(MainActivity.this,DownloadService.class);
        bindService(intent,connection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 请求访问外部存储的权限
     */
    private void initPermission(){
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==1 && grantResults[0]==PackageManager.PERMISSION_GRANTED){

        }else{
            finish();
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unbindService(connection);
    }
}
