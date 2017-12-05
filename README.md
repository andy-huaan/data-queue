data-queue
==========
Java Single Thread non blocking data queue

应用场景
------------
可用于批量数据入库，非阻塞方式可提高业务性能，并保障数据不丢失，可多线程并发使用！

快速开始
--------------
##### <font color=#0099ff>方式一：使用Maven</font>
```
<dependency>
  <groupId>com.xiaoujia.dataqueue</groupId>
  <artifactId>data-queue</artifactId>
  <version>1.2.0</version>
  <type>pom</type>
</dependency>
```
##### <font color=#0099ff>方式二：使用Gradle</font>
```
compile 'com.xiaoujia.dataqueue:data-queue:1.2.0'
```
##### <font color=#0099ff>方式三：使用lvy</font>
```
<dependency org='com.xiaoujia.dataqueue' name='data-queue' rev='1.2.0'>
  <artifact name='data-queue' ext='pom' ></artifact>
</dependency>
```
##### <font color=#0099ff>方式四：直接使用源码</font>
(1)如果您的项目使用Grale构建，可以直接将此项目作为一个Module引入
(2)也可以将Java文件直接拷贝到您的项目

<font color="#A52A2A">注意事项</font>
--------------
项目依赖JackSon，注意引入相关Jar包

源码实例
--------------
[data-queue-demo](https://github.com/andy-huaan/data-queue-demo)
相关代码实例持续更新中......

[使用过程中，如有任何问题请联系作者-赵华安](http://andy.cnlod.com/home.html)
[作者博客](http://blog.cnlod.com)
