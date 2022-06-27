package io.agora.helloagorachat;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.util.Log;

import io.agora.chat.ChatClient;
import io.agora.chat.ChatOptions;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Other init SDK
        initAgoraChatSDK();
    }

    /**
     * Should init in main process
     */
    private void initAgoraChatSDK() {
        if(!isMainProcess(this)) {
            return;
        }
        ChatOptions options = new ChatOptions();
        options.setAppKey("41117440#383391");
        ChatClient.getInstance().init(this, options);
    }

    public boolean isMainProcess(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo appProcess : activityManager.getRunningAppProcesses()) {
            Log.e("process", "appProcess.pid: "+appProcess.pid);
            if (appProcess.pid == pid) {
                return context.getApplicationInfo().packageName.equals(appProcess.processName);
            }
        }
        return false;
    }

}
