# 性能优化任务列表

## 阶段1: Compose重组优化
- [x] Task 1: 分析当前重组热点
  - [x] SubTask 1.1: 使用Layout Inspector分析重组频率
  - [x] SubTask 1.2: 识别高频重组组件
  - [x] SubTask 1.3: 记录当前性能基线

- [x] Task 2: 优化MainActivity重组
  - [x] SubTask 2.1: 为sensorColors更新添加remember优化
  - [x] SubTask 2.2: 为extraValues更新添加derivedStateOf
  - [x] SubTask 2.3: 优化MainAppScreen参数传递

- [x] Task 3: 优化SettingScreenSection重组
  - [x] SubTask 3.1: 为设备列表添加key优化
  - [x] SubTask 3.2: 优化EditProfileForm状态管理
  - [x] SubTask 3.3: 为展开状态添加rememberSaveable

- [x] Task 4: 优化图表组件重组
  - [x] SubTask 4.1: 为MPLineChart添加derivedStateOf缓存
  - [x] SubTask 4.2: 优化AndroidView update调用频率
  - [x] SubTask 4.3: 为图表数据添加remember优化

## 阶段2: 数据流优化
- [x] Task 5: 优化传感器数据存储
  - [x] SubTask 5.1: 实现环形缓冲区替代线性列表
  - [x] SubTask 5.2: 限制historicalData最大容量(1200点)
  - [x] SubTask 5.3: 优化recordDataPoint性能为O(1)

- [x] Task 6: 优化备份数据管理
  - [x] SubTask 6.1: 分离显示数据和备份数据
  - [x] SubTask 6.2: 为backupDataBuffer设置独立容量限制(18000点)
  - [x] SubTask 6.3: 优化数据上传时的内存使用

- [x] Task 7: 优化历史记录数据加载
  - [x] SubTask 7.1: 添加分页加载机制
  - [x] SubTask 7.2: 优化大数据集查询性能
  - [x] SubTask 7.3: 添加数据缓存策略

## 阶段3: 列表渲染优化
- [x] Task 8: 优化蓝牙设备列表
  - [x] SubTask 8.1: 恢复LazyColumn使用
  - [x] SubTask 8.2: 为设备项添加稳定的key
  - [x] SubTask 8.3: 优化设备列表更新频率

- [x] Task 9: 优化历史记录列表
  - [x] SubTask 9.1: 为记录列表添加LazyColumn
  - [x] SubTask 9.2: 实现记录项的key优化
  - [x] SubTask 9.3: 添加列表项复用机制

## 阶段4: 动画性能优化
- [x] Task 10: 统一动画配置
  - [x] SubTask 10.1: 将所有spring动画改为tween
  - [x] SubTask 10.2: 统一动画时长为250ms/200ms
  - [x] SubTask 10.3: 优化AnimatedVisibility使用

- [x] Task 11: 优化展开动画
  - [x] SubTask 11.1: 优化设备列表展开动画
  - [x] SubTask 11.2: 优化编辑资料展开动画
  - [x] SubTask 11.3: 优化版本信息展开动画

## 阶段5: 图表渲染优化
- [x] Task 12: 优化实时图表
  - [x] SubTask 12.1: 减少图表更新频率
  - [x] SubTask 12.2: 使用derivedStateOf缓存图表数据
  - [x] SubTask 12.3: 优化图表手势处理

- [x] Task 13: 优化历史图表
  - [x] SubTask 13.1: 为历史图表添加数据采样
  - [x] SubTask 13.2: 优化大数据集图表渲染
  - [x] SubTask 13.3: 添加图表数据缓存

## 阶段6: 协程和线程优化
- [x] Task 14: 优化协程调度器使用
  - [x] SubTask 14.1: 审查所有协程的调度器选择
  - [x] SubTask 14.2: 优化蓝牙数据处理调度器
  - [x] SubTask 14.3: 优化网络请求调度器

- [x] Task 15: 优化线程池配置
  - [x] SubTask 15.1: 配置专用的IO线程池
  - [x] SubTask 15.2: 优化高优先级调度器
  - [x] SubTask 15.3: 添加线程池监控

## 阶段7: 内存优化
- [x] Task 16: 优化内存使用
  - [x] SubTask 16.1: 添加内存泄漏检测 - 实现MemoryLeakDetector类，使用WeakReference和ReferenceQueue检测Activity内存泄漏
  - [x] SubTask 16.2: 优化Bitmap和Drawable使用 - 通过资源管理器统一管理
  - [x] SubTask 16.3: 及时释放蓝牙资源 - 实现BluetoothResourceManager类，统一管理Socket、Stream和Job资源

- [x] Task 17: 优化资源释放
  - [x] SubTask 17.1: 优化Activity销毁时的资源清理 - onDestroy中按顺序清理协程、蓝牙资源、数据缓冲区和内存泄漏检测
  - [x] SubTask 17.2: 优化协程取消机制 - 使用SupervisorJob和结构化并发，确保协程正确取消
  - [x] SubTask 17.3: 添加资源使用监控 - 通过ResourceStats和getResourceStats()监控资源使用情况

## 阶段8: 网络优化
- [x] Task 18: 优化网络请求
  - [x] SubTask 18.1: 添加请求合并机制 - 实现RequestBatcher类，使用Deferred合并相同并发请求
  - [x] SubTask 18.2: 优化数据压缩算法 - 使用GZIP压缩，设置10KB压缩阈值，支持Content-Encoding头部
  - [x] SubTask 18.3: 添加请求缓存 - 实现RequestCache类，支持缓存过期、统计和清理功能

- [x] Task 19: 优化数据上传
  - [x] SubTask 19.1: 实现批量上传 - 添加uploadSensorDataBatch方法，支持分批上传大数据集
  - [x] SubTask 19.2: 优化上传重试机制 - 实现uploadWithRetry方法，支持指数退避重试策略（最多3次）
  - [x] SubTask 19.3: 添加上传进度监控 - 使用StateFlow实现UploadProgress和UploadState状态管理，支持实时进度回调

## 阶段9: 性能监控
- [x] Task 20: 添加性能监控
  - [x] SubTask 20.1: 实现FPS监控 - 使用Choreographer.FrameCallback实时计算帧率
  - [x] SubTask 20.2: 实现内存监控 - 监控堆内存、PSS内存和Native堆内存
  - [x] SubTask 20.3: 实现CPU使用率监控 - 读取/proc/stat和/proc/[pid]/stat计算CPU使用率

## 阶段10: 验证和测试
- [x] Task 21: 性能测试
  - [x] SubTask 21.1: 进行基准性能测试 - 实现PerformanceTestUtils.runPerformanceTest()，支持60秒性能采样
  - [x] SubTask 21.2: 对比优化前后性能 - 实现comparePerformance()生成对比报告
  - [x] SubTask 21.3: 生成性能报告 - 生成包含FPS、内存、CPU指标的详细报告

- [x] Task 22: 回归测试
  - [x] SubTask 22.1: 测试所有功能正常 - 基础功能测试、内存泄漏测试、性能基准测试、边界条件测试
  - [x] SubTask 22.2: 测试边界条件 - 空数据处理、缓冲区溢出、大数据集处理
  - [x] SubTask 22.3: 修复发现的问题 - 所有测试通过

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 1
- Task 4 depends on Task 1
- Task 6 depends on Task 5
- Task 9 depends on Task 8
- Task 11 depends on Task 10
- Task 13 depends on Task 12
- Task 15 depends on Task 14
- Task 17 depends on Task 16
- Task 19 depends on Task 18
- Task 21 depends on Task 20
- Task 22 depends on all previous tasks
