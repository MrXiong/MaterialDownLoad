首先来个图：
![这里写图片描述](http://img.blog.csdn.net/20160622205902025)

附个链接：
[material-dialogs](https://github.com/afollestad/material-dialogs)

再附个链接（以微信的APK下载地址为例吧）：
http://gdown.baidu.com/data/wisegame/8d5889f722f640c8/weixin_800.apk

**系统下载器DownLoadManager本身下载在通知栏中都有进度条，但是我想自己手动弹出一个dialog显示进度，所有就有了使用内容观察者ContentObserver实现进度。**
1、ContentObserver作用：显示进度
2、BroadcastReceiver的作用：监听系统已经下载完毕发出的广播

>1、主Activity

```
package com.zx.version;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.afollestad.materialdialogs.MaterialDialog;

import cn.trinea.android.common.util.PreferencesUtils;

public class DownloadActivity extends AppCompatActivity {
    public static final String DOWNLOAD_ID = "download_id";
    private DownloadChangeObserver downloadObserver;
    private long lastDownloadId = 0;
    //"content://downloads/my_downloads"必须这样写不可更改
    public static final Uri CONTENT_URI = Uri.parse("content://downloads/my_downloads");
    private MaterialDialog materialDialog;
    private String NetUrl = "http://gdown.baidu.com/data/wisegame/8d5889f722f640c8/weixin_800.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        if (materialDialog == null) {
            materialDialog = new MaterialDialog.Builder(DownloadActivity.this)
                    .title("版本升级")
                    .content("正在下载安装包，请稍候")
                    .progress(false, 100, false)
                    .cancelable(false)
                    .show();
        }
        //1.得到下载对象
        DownloadManager dowanloadmanager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        //2.创建下载请求对象，并且把下载的地址放进去
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(NetUrl));
        //3.给下载的文件指定路径
        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "weixin.apk");
        //4.设置显示在文件下载Notification（通知栏）中显示的文字。6.0的手机Description不显示
        request.setTitle("微信1.3.0");
        request.setDescription("1.新增扫一扫\\r\\n2.首页添加热门\\r\\n3.优化和修复\\t\\t\\t\\t\\t");
        //5更改服务器返回的minetype为android包类型
        request.setMimeType("application/vnd.android.package-archive");
        //6.设置在什么连接状态下执行下载操作
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
        //7. 设置为可被媒体扫描器找到
        request.allowScanningByMediaScanner();
        //8. 设置为可见和可管理
        request.setVisibleInDownloadsUi(true);
        lastDownloadId = dowanloadmanager.enqueue(request);
        //9.保存id到缓存
        PreferencesUtils.putLong(this, DOWNLOAD_ID, lastDownloadId);
        //10.采用内容观察者模式实现进度
        downloadObserver = new DownloadChangeObserver(null);
        getContentResolver().registerContentObserver(CONTENT_URI, true, downloadObserver);
    }

    //用于显示下载进度
    class DownloadChangeObserver extends ContentObserver {

        public DownloadChangeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(lastDownloadId);
            DownloadManager dManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            final Cursor cursor = dManager.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                final int totalColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                final int currentColumn = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                int totalSize = cursor.getInt(totalColumn);
                int currentSize = cursor.getInt(currentColumn);
                float percent = (float) currentSize / (float) totalSize;
                int progress = Math.round(percent * 100);
                materialDialog.setProgress(progress);
                 if(progress == 100) {
                    materialDialog.dismiss();
                }
            }
        }


    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(downloadObserver);
    }
}
```
>2、系统下载完毕会发送广播

```
package com.zx.version;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.io.File;

import cn.trinea.android.common.util.PreferencesUtils;

/**
 * Created by zx on 2016/6/22.
 */
public class UpdataBroadcastReceiver extends BroadcastReceiver {

    @SuppressLint("NewApi")
    public void onReceive(Context context, Intent intent) {
        long downLoadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        long cacheDownLoadId = PreferencesUtils.getLong(context, DownloadActivity.DOWNLOAD_ID);
        if (cacheDownLoadId == downLoadId) {
            Intent install = new Intent(Intent.ACTION_VIEW);
            File apkFile = queryDownloadedApk(context);
            install.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(install);
        }
    }

    //通过downLoadId查询下载的apk，解决6.0以后安装的问题
    public static File queryDownloadedApk(Context context) {
        File targetApkFile = null;
        DownloadManager downloader = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = PreferencesUtils.getLong(context, DownloadActivity.DOWNLOAD_ID);
        if (downloadId != -1) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL);
            Cursor cur = downloader.query(query);
            if (cur != null) {
                if (cur.moveToFirst()) {
                    String uriString = cur.getString(cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    if (!TextUtils.isEmpty(uriString)) {
                        targetApkFile = new File(Uri.parse(uriString).getPath());
                    }
                }
                cur.close();
            }
        }
        return targetApkFile;
    }
}
```
>3、别忘了在AndroidManifest中注册广播，这样即使关闭了app，也能收到下载完成的广播，打开安装界面。
```
<receiver
            android:name=".UpdataBroadcastReceiver">
            <intent-filter>
                <action android:name="android.intent.action.DOWNLOAD_COMPLETE" />
            </intent-filter>
        </receiver>
```


温馨提示：
>6.0以后采用此方法：

      Intent install = new Intent(Intent.ACTION_VIEW);
        Uri downloadFileUri = dManager.getUriForDownloadedFile(myDwonloadID);
        install.setDataAndType(downloadFileUri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(install);

>安装程序会出现此错误：

```
08-20 14:58:56.127 26222 26222 E AndroidRuntime: Caused by: android.content.ActivityNotFoundException: Unable to find explicit activity class {com.android.packageinstaller/com.android.packageinstaller.PackageInstallerActivity}; have you declared this activity in your AndroidManifest.xml?
```

>解决方案，转成File再引入：

```
install.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
```
温馨提示2：

```
apply plugin: 'com.android.application'

android {
    compileSdkVersion 23
    buildToolsVersion "23.0.3"

    defaultConfig {
        applicationId "com.zx.version"
        minSdkVersion 15
        targetSdkVersion 22
        versionCode 1
        versionName "1.0"
        generatedDensities = []
    }
```
>在下载的时候需要把文件写入存储卡，所以如果你的targetSdkVersion >=23的话那么就需要请求权限才可以下载了，测试可以先设置
targetSdkVersion 22



温馨提示3：
>如果下载完成之后，出现“”应用未安装“”
那说明你的Menifest或者build.gradle里面的versionCode 20204设置出错了，在android中只能用大的versionCode替换（更新）小的versionCode，现在去检查一下versionCode的大小吧。


>附个源码：https://github.com/MrXiong/MaterialDownLoad  
>我是2.2的gradle，如果编译失败，请自行更改你的版本就可以了![这里写图片描述](http://img.blog.csdn.net/20160920174945525)

权限别忘了：
 

```
<uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

