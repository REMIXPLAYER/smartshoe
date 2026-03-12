# App代码优化计划

## 现状分析

通过对当前代码库的检查，发现以下主要文件存在冗余和可优化空间：

### 文件规模统计
- MainActivity.kt: ~2000行（过大）
- SettingScreenSection.kt: ~2300行（过大）
- DataRecordScreenSection.kt: ~640行
- HistoryScreenSection.kt: ~900行
- SensorDataManager.kt: ~580行
- SensorDataApiService.kt: ~700行

---

## 优化建议清单

### 1. MainActivity.kt 拆分（高优先级）

**问题**: MainActivity.kt 约2000行，承担了过多职责

**优化方案**:
- [ ] 将蓝牙相关逻辑提取到 `BluetoothManager` 类
- [ ] 将数据记录逻辑提取到 `DataRecorder` 类
- [ ] 将状态管理逻辑提取到 `AppStateManager` 类
- [ ] MainActivity只保留生命周期管理和组件协调

**预期收益**: 
- 代码行数减少60%
- 职责分离，便于测试
- 降低耦合度

---

### 2. SettingScreenSection.kt 组件拆分（高优先级）

**问题**: 2300行的单文件，包含20+个Composable函数

**优化方案**:
- [ ] 拆分为独立文件：
  - `AccountSettingsSection.kt` - 账户设置
  - `DeviceSettingsSection.kt` - 设备管理
  - `WeightSettingsSection.kt` - 体重设置
  - `DataManagementSection.kt` - 数据管理
  - `NotificationSettingsSection.kt` - 通知设置
  - `AboutSection.kt` - 关于页面
- [ ] 提取通用组件：`SettingItemCard`、`ExpandableSection`

**预期收益**:
- 单文件控制在300行以内
- 提高可维护性
- 便于团队协作

---

### 3. 重复代码提取（中优先级）

**发现的问题**:

#### 3.1 图表配置重复
- DataRecordScreenSection 和 HistoryScreenSection 都有图表配置代码
- 重复代码约150行

**优化方案**:
- [ ] 创建 `ChartConfigUtils` 工具类
- [ ] 统一图表样式配置

#### 3.2 动画配置重复
- 多个文件使用相同的 AnimatedVisibility 配置
- 动画时长、缓动函数分散在各处

**优化方案**:
- [ ] 创建 `AnimationDefaults` 常量对象
- [ ] 统一动画参数

#### 3.3 日期格式化重复
- SimpleDateFormat 实例在多处创建
- 格式字符串硬编码

**优化方案**:
- [ ] 创建 `DateTimeUtils` 工具类
- [ ] 复用 DateFormat 实例

---

### 4. 性能监控代码可选化（中优先级）

**问题**: 性能监控相关代码（PerformanceMonitor等）在Release模式下仍然存在

**优化方案**:
- [ ] 使用 BuildConfig.DEBUG 条件编译
- [ ] 将性能监控代码标记为 @DebugOnly
- [ ] Release包完全移除性能监控代码

**预期收益**:
- Release APK减小约50KB
- 减少不必要的运行时开销

---

### 5. 资源管理优化（中优先级）

**发现的问题**:

#### 5.1 过多的调度器创建
```kotlin
// 当前代码创建了多个自定义调度器
private val highPriorityIoDispatcher = Executors.newSingleThreadExecutor(...)
private val lowPriorityBackgroundDispatcher = Executors.newSingleThreadExecutor(...)
```

**优化方案**:
- [ ] 复用 Dispatchers.IO
- [ ] 或使用统一的 CoroutineScope 管理

#### 5.2 缓存策略优化
- RequestCache、historyCache 等缓存分散管理

**优化方案**:
- [ ] 创建统一的 `CacheManager`
- [ ] 统一缓存策略和过期处理

---

### 6. UI常量统一管理（低优先级）

**问题**: UIConstants 中定义了颜色、尺寸等，但部分代码仍硬编码

**优化方案**:
- [ ] 将所有硬编码的颜色、尺寸提取到 UIConstants
- [ ] 创建语义化的命名规范

---

### 7. 网络层优化（低优先级）

**问题**: SensorDataApiService 约700行，包含接口和实现

**优化方案**:
- [ ] 分离接口定义和实现
- [ ] 创建 Retrofit 风格的 API 定义
- [ ] 使用依赖注入管理 API 服务

---

### 8. 测试友好性改进（低优先级）

**问题**: 当前代码耦合度高，难以单元测试

**优化方案**:
- [ ] 提取接口，便于Mock
- [ ] 使用依赖注入框架（如Hilt）
- [ ] 分离业务逻辑和UI逻辑

---

## 优化优先级排序

| 优先级 | 优化项 | 影响范围 | 预估工作量 |
|--------|--------|----------|------------|
| P0 | MainActivity拆分 | 高 | 2天 |
| P0 | SettingScreenSection拆分 | 高 | 2天 |
| P1 | 图表配置统一 | 中 | 0.5天 |
| P1 | 动画配置统一 | 低 | 0.5天 |
| P1 | 日期工具类 | 低 | 0.5天 |
| P2 | 性能监控可选化 | 中 | 1天 |
| P2 | 调度器优化 | 中 | 0.5天 |
| P3 | 缓存统一管理 | 中 | 1天 |
| P3 | UI常量完善 | 低 | 0.5天 |
| P4 | 网络层重构 | 高 | 2天 |
| P4 | 测试友好性 | 高 | 3天 |

---

## 实施建议

### 阶段1（第1-2周）
1. MainActivity.kt 拆分
2. SettingScreenSection.kt 拆分

### 阶段2（第3周）
1. 重复代码提取（图表、动画、日期）
2. 性能监控可选化

### 阶段3（第4周）
1. 资源管理优化
2. 缓存统一管理

### 阶段4（后续）
1. 网络层重构
2. 测试友好性改进

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 重构引入Bug | 高 | 充分测试，小步提交 |
| 功能回归 | 中 | 保持API兼容，渐进式迁移 |
| 工期延误 | 中 | 分阶段实施，优先核心功能 |

---

## 预期收益总结

- **代码行数**: 预计减少30-40%
- **可维护性**: 显著提升
- **测试覆盖率**: 更容易达到80%+
- **APK大小**: Release包减小约100KB
- **启动速度**: 减少类加载时间
