package com.xiaoujia.dataqueue.core;
import java.util.List;

/**
 * 队列监听器，监听缓存文件处理操作
 * Created by Andy
 * email zhaohuaan0925@163.com
 * created 2017/1/5 09:05
 * 监听队列调度器相关操作，自动批量处理队列中数据
 * 批量处理数据量可由用户自定义
 */
public interface DataQueueListener<T> {

	/**
	 * 批量获取队列中数据
	 * @param list 已获取到的队列中数据
	 * @throws Exception
     */
	public void peeks(List<T> list) throws Exception;
}
