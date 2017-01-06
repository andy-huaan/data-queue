package com.xiaoujia.dataqueue.core;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 数据读写缓存文件调度器
 * Created by Andy
 * email zhaohuaan0925@163.com
 * created 2017/1/5 09:21
 * 调度缓存文件读写
 */
public class DataQueueExecutor<T> implements Runnable {

	//数据队列监听器
	private final DataQueueListener<T> queueListener;
	//缓存文件操作类
	private LogAccessFile logAccessFile;
	//每次最多从缓存文件中取出的数据量
	private int maxCount;
	//缓存数据类的Class实例
	private Class<T> clazz;
	//对象锁
	private final Object lock;
	//线程池，用户新线程管理
	private ExecutorService pool;
	//用于判断 Runnable 是否结束执行
	private Future<?> future;
	private boolean timeout = false;

	/**
	 * 构造写缓存文件调度器
	 * @param queueListener 数据队列监听器
	 * @param clazz 缓存数据类的Class实例
	 * @param maxCount 每次最多从缓存文件中取出的数据量
	 * @param fileName 缓存文件名(也可以是全路径名)
     * @throws IOException
     */
	public DataQueueExecutor(DataQueueListener<T> queueListener,Class<T> clazz,int maxCount,
							 String fileName) throws IOException {
		this(queueListener,clazz,maxCount);
		this.logAccessFile = new LogAccessFile(fileName);
	}

	/**
	 * 构造写缓存文件调度器
	 * @param queueListener 数据队列监听器
	 * @param clazz 缓存数据类的Class实例
	 * @param maxCount 每次最多从缓存文件中取出的数据量
	 * @param fileName 缓存文件名(也可以是全路径名)
	 * @param maxSize 每个缓存文件大小，单位字节
	 *
	 * @throws IOException
	 */
	public DataQueueExecutor(DataQueueListener<T> queueListener,Class<T> clazz,int maxCount,
							 String fileName, long maxSize) throws IOException {
		this(queueListener,clazz,maxCount);
		this.logAccessFile = new LogAccessFile(fileName, maxSize);
	}

    /**
     * 构造写缓存文件调度器
     * @param queueListener 数据队列监听器
	 * @param clazz 缓存数据类的Class实例
	 * @param maxCount 每次最多从缓存文件中取出的数据量
     * @param fileName 缓存文件名(也可以是全路径名)
     * @param maxSize 每个缓存文件大小，单位字节
     * @param delLog 是否删除读取过的缓存文件
     *
     * @throws IOException
     */
	public DataQueueExecutor(DataQueueListener<T> queueListener,Class<T> clazz,int maxCount,
							 String fileName, long maxSize, boolean delLog) throws IOException {
		this(queueListener,clazz,maxCount);
		this.logAccessFile = new LogAccessFile(fileName, maxSize, delLog);
	}

	//私有构造函数
	private DataQueueExecutor(DataQueueListener<T> queueListener,Class<T> clazz,int maxCount){
		this.queueListener = queueListener;
		this.clazz=clazz;
		this.maxCount = maxCount;
		//创建对象锁
		lock = new Object();
		//创建一个使用单个 worker 线程的 Executor，以无界队列方式来运行该线程
		pool = Executors.newSingleThreadExecutor();
		//将任务提交到任务队列
		future = pool.submit(this);
	}
	
	/**
	 * 向缓存文件中写入数据
	 * @param data 待写入数据
	 * @throws IOException
	 */
	public void offer(T data) throws IOException {
		logAccessFile.writeCacheAsJson(data);
		if (timeout){
			synchronized (lock) {
				if (pool == null || pool.isShutdown()) {
					pool = Executors.newSingleThreadExecutor();
				}
				if (future == null || future.isDone()){
					future = pool.submit(this);
				}
			}
		}else {
			if (pool == null || pool.isShutdown()) {
				pool = Executors.newSingleThreadExecutor();
			}
			if (future == null || future.isDone()){
				future = pool.submit(this);
			}
		}
	}

	/**
	 * 线程run方法,自动执行，调取用户重写的peeks方法，处理缓存文件中的数据
	 */
	public void run() {
		long idel = 0;
		List<T> list = new ArrayList<T>();
		while (pool != null && !pool.isShutdown()) {
			try {
				while (logAccessFile.dataFileCheck()) { //判断缓存文件缓冲区中是否存在未处理数据
					idel = System.currentTimeMillis();
					timeout = false;
					while (logAccessFile.dataFileCheck() && list.size() < maxCount) {
						T str = logAccessFile.readLineFromJson(clazz);
						if (str != null) {
							list.add(str);
						}
					}
					if (list.size() > 0) {
						if (queueListener != null)
                            try {
                                queueListener.peeks(list);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        logAccessFile.changeMarker();
						list.clear();
					}
				}
				if (System.currentTimeMillis() - idel > 5 * 1000) { //规定时间内没有数据进入缓存文件，停止此线程
					timeout = true;
					synchronized (lock) {
						if (!logAccessFile.dataFileCheck()) {
							//此时线程池不会立刻退出，直到添加到线程池中的任务都已经处理完成，才会退出
							pool.shutdown();
							pool = null;
						}
					}
				} else {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException ex) {
				//磁盘IO错误，不可读写
				ex.printStackTrace();
			}
		}
	}
}
