package com.xiaoujia.dataqueue.utils;

import com.xiaoujia.dataqueue.core.LogAccessFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 文件操作工具类
 * Created by Andy
 * email zhaohuaan0925@163.com
 * created 2017/1/5 14:05
 */
public class FileUtils {

    /**
     * 文件路径不存在则自动创建
     * @param fullPath 文件路径
     */
    public static void mkdirs(String fullPath) throws IOException {
        File dirPath = new File(fullPath);
        if (!dirPath.exists()) {
            if(!dirPath.mkdirs()){
                throw new IOException("建立缓存存储路径失败");
            }
        }
    }

    /**
     * 获取Mark文件
     * @param fileName mark文件全路径
     * @return
     * @throws IOException
     */
    public static RandomAccessFile getMarkerFile(String fileName, LogAccessFile logAccessFile) throws IOException {
        RandomAccessFile marker;
        File markFile = new File(fileName + Constants.MARKER_SUFFIX);
        if (markFile.exists()) {
            if (markFile.isFile() && markFile.canRead() && markFile.canWrite()) {
                marker = new RandomAccessFile(markFile, "rw");
                logAccessFile.initCurrentLine(marker.readLong());
            } else {
                throw new IOException(fileName + ".mark 此文件异常");
            }
        } else {
            marker = new RandomAccessFile(markFile, "rw");
            initMarker(marker,0);
        }
        return marker;
    }

    /**
     * 初始化marker文件信息
     * @param marker marker文件操作对象
     * @param count 当前已处理行数
     * @throws IOException
     */
    public synchronized static void initMarker(RandomAccessFile marker, long count) throws IOException{
        //设置到此文件开头测量到的文件指针偏移量，在该位置发生下一个读取或写入操作
        marker.seek(0);
        marker.writeLong(count);
        //maker.writeBytes(count + "\r\n");
    }
}
