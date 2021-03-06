package com.xiaoujia.dataqueue.core;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoujia.dataqueue.utils.BufferedRandomAccessFile;
import com.xiaoujia.dataqueue.utils.Constants;
import com.xiaoujia.dataqueue.utils.FileUtils;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 数据读写缓存文件处理
 * Created by 赵华安(Andy)
 * email zhaohuaan0925@163.com
 * created 2017/1/5 12:13
 * 处理缓存文件初始化、读写、删除、重命名等操作
 */
public class LogAccessFile {

    //缓存文件名(也可以是全路径名)
    private final String fileName;
    //每个缓存文件大小，单位字节
    private final long maxSize;
    //是否删除读取完成的缓存文件
    private final boolean delLog;
    //对象锁
    private final Object lock;
    //marker标记文件操作类
    private RandomAccessFile marker;
    //当前操作行
    private long currentPointer;
    //当前操作的缓存文件
    private File currentFile;
    //写缓存文件操作类
    private FileWriter fileWriter;
    //读缓存文件操作类
    private BufferedRandomAccessFile fileReader;
    //JackSon操作类
    private ObjectMapper objectMapper;

    /**
     * 构造函数,每个缓存文件大小100MB,重命名缓存文件
     * @param fileName 缓存文件名(也可以是全路径名)
     * @throws IOException
     */
    public LogAccessFile(String fileName) throws IOException {
        this(fileName, Constants.DEFAULT_MAXSIZE);
    }

    /**
     * 构造函数,重命名缓存文件
     * @param fileName 缓存文件名(也可以是全路径名)
     * @param maxSize 每个缓存文件大小，单位字节
     * @throws IOException
     */
    public LogAccessFile(String fileName, long maxSize) throws IOException {
        this(fileName, maxSize, Constants.DELETE_LOG);
    }

    /**
     * 构造函数
     * @param fileName 缓存文件名(也可以是全路径名)
     * @param maxSize 每个缓存文件大小，单位字节
     * @param delLog 是否删除读取过的缓存文件(true:删除 false:不删除，重命名)
     * @throws IOException
     */
    public LogAccessFile(String fileName, long maxSize, boolean delLog) throws IOException {
        this.fileName = fileName;
        this.maxSize = maxSize;
        this.delLog = delLog;
        this.lock = new Object();
        //初始化文件信息
        initFile(fileName);
        //初始化JackSon操作对象
        initObjectMapper();
        //文件检查：文件已达到最大字节数更名或删除之
        dataFileCheck();
    }

    //初始化相关文件配置
    private void initFile(String fileName) throws IOException{
        //文件路径不存在则自动创建
        if (fileName.lastIndexOf('/') > 0) {
            FileUtils.mkdirs(fileName.substring(0, fileName.lastIndexOf('/')));
        }
        this.marker = FileUtils.getMarkerFile(fileName,this);
        //打开缓存文件
        this.currentFile = new File(fileName);
        this.fileWriter = new FileWriter(currentFile, true);
        this.fileReader = new BufferedRandomAccessFile(currentFile,Constants.READ);

        //跳到mark标记的已读位置
        fileReader.seek(currentPointer);
    }

    //初始化Jackson配置
    private void initObjectMapper(){
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    /**
     * 判断缓存文件是否已准备好被读取
     * @return true:已达到 false:未达到
     * @throws IOException
     */
    synchronized public boolean dataFileCheck() throws IOException {
        //判断此流是否已准备好被读取
        //如果缓冲区有内容，返回true，否则为false
        boolean ready = fileReader.length() != fileReader.getFilePointer();
        if (!ready) {
            if (currentFile.length() > maxSize) {
                synchronized (lock) {
                    if (fileReader.length() != fileReader.getFilePointer()){ //防止前后两次ready()之间又有内容写入数据文件
                        return true;
                    }else{
                        fileWriter.close();
                        fileReader.close();
                        renameOrDeleteFile();
                        fileWriter = new FileWriter(currentFile, true);
                        fileReader = new BufferedRandomAccessFile(currentFile,Constants.READ);
                    }
                }
            }
        }
        return ready;
    }

    //修改缓存文件名或删除
    private void renameOrDeleteFile() throws IOException {
        if (delLog) {
            deleteFile();
        } else {
            renameFile();
        }
        currentPointer = 0;
        while (true) {
            try {
                FileUtils.initMarker(marker, currentPointer);
                break;
            } catch (IOException e) {
                System.out.println("计数mark归零失败" + e.getMessage());
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //删除文件
    private void deleteFile(){
        if(!currentFile.delete()){ //删除文件失败
            System.gc();
            if(!currentFile.delete()){
                System.gc();
                while (!currentFile.delete()) {
                    System.gc();
                    System.out.println("文件：" + fileName + " 删除时失败！");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    //重命名文件
    private void renameFile(){
        File tempFile = new File(fileName
                + new SimpleDateFormat("_yyyyMMddHHmmss").format(new Date()));
        if(!currentFile.renameTo(tempFile)){
            System.gc();
            if(!currentFile.renameTo(tempFile)){
                System.gc();
                while (!currentFile.renameTo(tempFile)) {
                    System.gc();
                    System.out.println("文件：" + fileName + " 改名为 " + tempFile.getName() + " 时失败！");
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 向缓存文件中写入JSON格式数据
     * @param object 待写入数据
     * @throws IOException
     */
    public void writeCacheAsJson(Object object) throws IOException {
        writeCacheAsString(objectMapper.writeValueAsString(object));
    }

    /**
     * 向缓存文件中写入字符串数据
     * @param str 待写入数据
     * @throws IOException
     */
    public void writeCacheAsString(String str) throws IOException {
        synchronized (lock) {
            fileWriter.write(str + Constants.ENTER);
            fileWriter.flush();
        }
    }

    /**
     * 从缓存文件中读取JSON数据并转化为Java对象
     * @param tClass 缓存数据类的Class实例
     * @return
     * @throws IOException
     */
    public <T> List<T> readDatasFromJson(Class<T> tClass,int maxCount,String encode) throws IOException {
        List<T> list = new ArrayList<>();
        while (dataFileCheck() && list.size() < maxCount) {
            String line = fileReader.readLine();
            if (line != null && !"".equals(line)) { //如果为空则跳过此行
                line = new String(line.getBytes(Constants.ENCODE_ISO), encode);
                list.add(objectMapper.readValue(line, tClass));
            }
        }
        currentPointer = fileReader.getFilePointer();
        return list;
    }

    /**
     * 更改marker计数器值
     * @throws IOException
     */
    public void changeMarker() throws IOException {
        changeMarker(currentPointer);
    }

    /**
     * 更改marker计数器值
     * @param currentPointer
     */
    public void changeMarker(long currentPointer) throws IOException{
        FileUtils.initMarker(marker,currentPointer);
    }

    /**
     * 启动时，利用marker文件记录数，初始化currentPointer
     * @param currentPointer
     */
    public void initcurrentPointer(long currentPointer){
        this.currentPointer = currentPointer;
    }

    /**
     * 获取marker文件中值
     * @return 已处理数据条数
     * @throws IOException
     */
    public long getMarker() throws IOException {
        marker.seek(0);
        return marker.readLong();
    }
}
