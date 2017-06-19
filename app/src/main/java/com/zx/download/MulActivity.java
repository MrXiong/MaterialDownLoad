package com.zx.download;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.daimajia.numberprogressbar.NumberProgressBar;
import com.zhy.adapter.recyclerview.CommonAdapter;
import com.zhy.adapter.recyclerview.base.ViewHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MulActivity extends AppCompatActivity {
    public static final String DOWNLOAD_ID = "download_id";
    @Bind(R.id.rv_list)
    RecyclerView mRvList;
    private DownloadChangeObserver downloadObserver;
    public static final Uri CONTENT_URI = Uri.parse("content://downloads/my_downloads");

    private ExecutorService cachedThreadPool;

    private List<Download> mList = new ArrayList<>();
    private CommonAdapter mDownloadAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mul);
        ButterKnife.bind(this);
        initData();
        cachedThreadPool = Executors.newCachedThreadPool();

    }

    private void initData() {
        for (int i = 0; i < Contans.URLS.length; i++) {
            Download download = new Download();
            download.setFileName(Contans.FILE_NAMES[i]);
            download.setName(Contans.NAMES[i]);
            download.setTitle(Contans.TITLES[i]);
            download.setDescription(Contans.DESCRIPTIONS[i]);
            download.setDownLoadUrl(Contans.URLS[i]);
            mList.add(download);
        }

    }

    private void initView() {
        mRvList.setLayoutManager(new LinearLayoutManager(this));
        mDownloadAdapter = new CommonAdapter<Download>(this, R.layout.listitem_mul, mList) {
            @Override
            protected void convert(ViewHolder holder, final Download download, int position) {
                holder.setText(R.id.tv_name, download.getName());

                cachedThreadPool.execute(new DownLoadTask(holder, download, position));
                final NumberProgressBar numberBar = holder.getView(R.id.number_download);
                holder.setOnClickListener(R.id.btn_install, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (numberBar.getProgress() == 100) {
                            install(MulActivity.this, download.getLastDownLoadId());
                        }
                    }
                });
            }
        };
        mRvList.setAdapter(mDownloadAdapter);
    }

    @OnClick(R.id.btn_downs)
    public void onViewClicked() {
        initView();
    }


    public class DownLoadTask implements Runnable {
        private ViewHolder holder;
        private Download downLoad;
        private int position;

        public DownLoadTask(ViewHolder holder, Download download, int position) {
            this.holder = holder;
            this.downLoad = download;
            this.position = position;

        }

        @Override
        public void run() {
            downLoadFile(downLoad);
        }

        private void downLoadFile(Download downLoad) {
            //1.得到下载对象
            DownloadManager dowanloadmanager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            //2.创建下载请求对象，并且把下载的地址放进去
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downLoad.getDownLoadUrl()));
            //3.给下载的文件指定路径
            request.setDestinationInExternalFilesDir(MulActivity.this, Environment.DIRECTORY_DOWNLOADS, downLoad.getFileName());
            //4.设置显示在文件下载Notification（通知栏）中显示的文字。6.0的手机Description不显示
            request.setTitle(downLoad.getTitle());
            request.setDescription(downLoad.getDescription());
            //5更改服务器返回的minetype为android包类型
            request.setMimeType("application/vnd.android.package-archive");
            //6.设置在什么连接状态下执行下载操作
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE | DownloadManager.Request.NETWORK_WIFI);
            //7. 设置为可被媒体扫描器找到
            request.allowScanningByMediaScanner();
            //8. 设置为可见和可管理
            request.setVisibleInDownloadsUi(true);

            long lastDownloadId = dowanloadmanager.enqueue(request);
            downLoad.setLastDownLoadId(lastDownloadId);

            //10.采用内容观察者模式实现进度
            downloadObserver = new DownloadChangeObserver(null, holder, downLoad, position);
            getContentResolver().registerContentObserver(CONTENT_URI, true, downloadObserver);
        }
    }


    //用于显示下载进度
    class DownloadChangeObserver extends ContentObserver {
        private ViewHolder holder;
        private Download downLoad;
        private int position;

        public DownloadChangeObserver(Handler handler, ViewHolder holder, Download download, int position) {
            super(handler);
            this.holder = holder;
            this.downLoad = download;
            this.position = position;
        }

        @Override
        public void onChange(boolean selfChange) {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downLoad.getLastDownLoadId());

            DownloadManager dManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            final Cursor cursor = dManager.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                final int totalColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                final int currentColumn = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                int totalSize = cursor.getInt(totalColumn);
                int currentSize = cursor.getInt(currentColumn);
                float percent = (float) currentSize / (float) totalSize;
                final int progress = Math.round(percent * 100);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        NumberProgressBar numberBar = holder.getView(R.id.number_download);
                        numberBar.setProgress(progress);
                    }
                });

                Log.v("progress" + downLoad.getLastDownLoadId(), progress + "");
            }
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(downloadObserver);
    }


    //安装
    private void install(Context context, long downloadId) {
        Intent install = new Intent(Intent.ACTION_VIEW);
        File apkFile = queryDownloadedApk(context, downloadId);
        install.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(install);
    }

    //通过downLoadId查询下载的apk，解决6.0以后安装的问题
    public static File queryDownloadedApk(Context context, long downloadId) {
        File targetApkFile = null;
        DownloadManager downloader = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
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