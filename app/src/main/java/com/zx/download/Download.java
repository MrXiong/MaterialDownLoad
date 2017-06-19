package com.zx.download;

import java.io.Serializable;

/**
 * Created by user on 2017/6/19.
 */

public class Download implements Serializable {
    private long lastDownLoadId;
    //weixin.apk
    private String fileName;
    //weixin1.0
    private String name;

    private String downLoadUrl;
    //通知栏
    private String title;
    private String description;

    public String getDownLoadUrl() {
        return downLoadUrl;
    }

    public void setDownLoadUrl(String downLoadUrl) {
        this.downLoadUrl = downLoadUrl;
    }

    public long getLastDownLoadId() {
        return lastDownLoadId;
    }

    public void setLastDownLoadId(long lastDownLoadId) {
        this.lastDownLoadId = lastDownLoadId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
