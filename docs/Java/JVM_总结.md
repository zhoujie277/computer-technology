## JVM_总结

### JVM 参数一览表

JVM 参数                      | 含义       | 配置格式
---------------------------- | --------   | ------
-Xms                         | 堆初始大小   | -Xms16m
-Xmx 或 -XX:MaxHeapSize=size | 最大堆大小   | -Xmx16m
-Xmn 或 -XX:NewSize=size + -XX:MaxNewSize=size | 新生代大小
-XX:InitialSurvivorRatio=ratio 和 -XX:+UseAdaptiveSizePolicy | 幸存区比例（动态）
-XX:SurvivorRatio=ratio               | 幸存区比例 | -
-XX:MaxTenuringThreshold=threshold    | 晋升阈值 | -
-XX:+PrintTenuringDistribution   | 晋升详情 | -
-XX:+PrintGCDetails -verbose:gc  | GC 详情 | -
-XX:+PrintGCDateStamps  | 打印 GC 的耗时 | -
-XX:+ScavengeBeforeFullGC        | FullGC 前 MinorGC
-Xss	| 设置线程栈空间大小 | -Xss256k
XX:PermSize	 | 永久代空间大小（jdk8已废弃）	| -XX:PermSize=256m
-XX:MaxPermSize	| 最大永久区大小  | -XX:MaxPermSize=256m
-XX:+UseConcMarkSweepGC	| 老年代使用cms收集器 |	-
-XX:+UseParNewGC | 新生代使用并行收集器 | -
-XX:+UserSerialGC | 使用串行收集器 | -
XX:+ParallelGCThreads | 设置并行线程数量 | -XX:+ParallelGCThreads=4
-XX:+HeapDumpOnOutOfMemoryError	| 当抛出OOM时进行HeapDump | -
-XX:+DisableExplicitGC | 禁止显示GC | -


常用组合



Young | Old | JVM Option
----- | ---- | --------
Serial | Serial | -XX:+UserSerialGC
Parallel | Parallel/Serial | -XX:+UseParallelGC  -XX:+UseParallelOldGC
Serial/Parnllel | CMS | -XX:+UseParNewGC -XX:+UseConcSweepGC
G1 | -  | -XX:+UseG1GC


### 编译 JDK 
默认编译时警告会当成错误终止编译运行，可在编译前增加过滤警告参数，
```
bash configure --disable-warnings-as-errors --enable-debug --with-jvm-variants=server
```