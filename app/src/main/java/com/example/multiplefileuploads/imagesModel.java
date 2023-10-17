package com.example.multiplefileuploads;

import java.io.File;

public class imagesModel {
    String imagesUrl;
    int imageNum;
    File file;

    public imagesModel(String imagesUrl, int imageNum,File file) {
        this.imagesUrl = imagesUrl;
        this.imageNum = imageNum;
        this.file=file;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getImagesUrl() {
        return imagesUrl;
    }

    public void setImagesUrl(String imagesUrl) {
        this.imagesUrl = imagesUrl;
    }

    public int getImageNum() {
        return imageNum;
    }

    public void setImageNum(int imageNum) {
        this.imageNum = imageNum;
    }
}
