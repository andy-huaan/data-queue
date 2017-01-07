package com.xiaoujia.dataqueue.utils;

/**
 * 常量工具类
 * Created by Andy
 * email zhaohuaan0925@163.com
 * created 2017/1/5 13:42
 */
public class Constants {

    //回车换行符
    public static final String ENTER = "\r\n";

    //缓存文件默认大小(单位：字节)100MB
    public static final long DEFAULT_MAXSIZE = 100000000;

    //删除缓存文件(true:删除 false:保留，重命名)
    public static final boolean DELETE_LOG = false;

    //marker文件后缀名(扩展名)
    public static final String MARKER_SUFFIX = ".marker";

    //文件操作权限：只读
    public static final String READ = "r";

    //文件操作权限：读写
    public static final String READ_WRITE = "rw";

    //文件编码：ISO-8859-1
    public static final String ENCODE_ISO = "ISO-8859-1";
}
