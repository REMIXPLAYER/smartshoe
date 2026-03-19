# SmartShoe 项目重构计划

## 项目概述
当前项目存在渐进式重构遗留问题，MainActivity职责过重，存在跨层调用等Clean Architecture违规问题。

## 重构目标
1. 消除跨层调用，严格遵循Clean Architecture分层原则
2. 拆分MainActivity职责，提高代码可维护性
3. 移除渐进式重构遗留代码
4. 统一架构风格，确保代码一致性

---

## 第一阶段：消除跨层调用（高优先级）

### 目标
消除MainActivity直接访问Manager和LocalDataSource的问题，所有数据访问通过ViewModel进行。

### 任务清单

#### 1.1 迁移 sensorDataManager 调用到 SensorDataViewModel
**文件**: `ui/viewmodel/SensorDataViewModel.kt`
**新增方法**:
- `uploadDataToServer(dataPoints: List<SensorDataPoint>, onResult: (Boolean, String) -> Unit)`
- `clearCache()` - 替代MainActivity中的缓存清理逻辑

**文件**: `MainActivity.kt`
**修改**:
- 移除 `sensorDataManager` 注入
- 修改 `uploadSensorDataToServer()` 调用为 `sensorDataViewModel.uploadDataToServer()`

#### 1.2 迁移 getUserRecords 到 HistoryRecordViewModel
**文件**: `ui/viewmodel/HistoryRecordViewModel.kt`
**新增方法**:
- `getUserRecords(page: Int, onResult: (Boolean, String, List<SensorDataRecord>?) -> Unit)`

**文件**: `MainActivity.kt`
**修改**:
- 移除 `getUserSensorRecords()` 方法
- 更新调用处使用 `historyRecordViewModel.getUserRecords()`

#### 1.3 消除 localDataSource 直接访问
**文件**: `MainActivity.kt`
**问题代码**:
```kotlin
// 第216-223行
val session = localDataSource.getUserSession()
val token = session?.second

// 第978行
val enabled = localDataSource.getBoolean(KEY_PRESSURE_ALERTS_ENABLED, true)

// 第681行
localDataSource.clear()
```

**解决方案**:
- 将Token同步逻辑移到 `AuthViewModel`
- 将设置读取移到 `SensorDataViewModel`
- 将缓存清理移到 `CacheManager`

#### 1.4 移除 MainActivity 中的 Manager 注入
**文件**: `MainActivity.kt`
**移除注入**:
- `authManager` → 通过 `AuthViewModel` 访问
- `sensorDataManager` → 通过 `SensorDataViewModel` 访问
- `bluetoothResourceManager` → 通过 `BluetoothViewModel` 访问
- `localDataSource` → 完全移除

**保留注入**（需要Activity生命周期）:
- `bluetoothConnectionManager` - 需要设置回调
- `performanceMonitor` - 需要启动/停止监控

---

## 第二阶段：拆分 MainActivity 职责（高优先级）

### 目标
将MainActivity中的UI组件、业务逻辑分离到各自的文件中，使MainActivity只负责协调各组件。

### 任务清单

#### 2.1 创建 MainScreen.kt
**新文件**: `ui/screen/MainScreen.kt`
**迁移内容**:
- `MainAppScreen()` Composable (第312-469行)
- `AppTopBar()` Composable (第477-490行)
- `MainContent()` Composable (第497-550行)

#### 2.2 创建 DataRecordScreen.kt（已存在，需要整合）
**文件**: `ui/screen/DataRecordScreen.kt`
**迁移内容**:
- 将MainActivity中的 `DataRecordScreen()` (第558-591行) 整合到现有文件

#### 2.3 创建 SettingsScreen.kt（已存在，需要整合）
**文件**: `ui/screen/SettingScreen.kt`
**迁移内容**:
- 将MainActivity中的 `SettingsScreen()` (第594-672行) 整合到现有文件

#### 2.4 创建 CacheManager
**新文件**: `data/manager/CacheManager.kt`
**迁移内容**:
- 从MainActivity迁移 `clearAppCache()` 方法
- 统一管理应用缓存清理

#### 2.5 简化后的 MainActivity
**目标代码量**: 从1106行减少到200行以内
**保留内容**:
- onCreate: 初始化ViewModel回调、权限检查
- onDestroy: 资源清理
- 必要的回调方法

---

## 第三阶段：修复 Repository 和 ViewModel 职责（中优先级）

### 目标
确保Repository只负责数据访问，ViewModel负责业务逻辑和状态管理。

### 任务清单

#### 3.1 修复 SensorDataRepository
**文件**: `data/repository/SensorDataRepository.kt`
**移除内容**:
- 压力提醒逻辑 `checkPressureAlerts()`
- 压力提醒回调 `onPressureAlert`
- 压力提醒开关 `pressureAlertsEnabled`

**文件**: `ui/viewmodel/SensorDataViewModel.kt`
**新增内容**:
- 压力提醒逻辑
- 压力提醒状态 StateFlow

#### 3.2 创建 UserProfileRepository
**新文件**: `data/repository/UserProfileRepository.kt`
**职责**:
- 用户体重读写
- 用户偏好设置

**文件**: `ui/viewmodel/UserProfileViewModel.kt`
**修改**:
- 移除 `LocalDataSource` 直接注入
- 改为注入 `UserProfileRepository`

#### 3.3 修复 AuthManager 依赖注入
**文件**: `data/manager/AuthManager.kt`
**修改**:
- 将 `AuthApiService.create()` 改为构造函数注入
- 修改 `AuthModule` 提供 `AuthApiService` 实例

---

## 第四阶段：清理遗留代码（低优先级）

### 目标
删除@Deprecated方法和未使用的代码。

### 任务清单

#### 4.1 删除 @Deprecated 蓝牙方法
**文件**: `MainActivity.kt`
**删除方法**:
- `disconnectFromBluetoothDevice()` (第759-782行)
- `scanForBluetoothDevices()` (第840-863行)
- `connectToBluetoothDevice()` (第881-909行)
- `startListeningForData()` (第926-951行)

**删除变量**:
- `bluetoothSocket` (第140行)
- `hc06UUID` (第144行)
- `dataListeningJob` (第131行)

**注意**: 确认新架构稳定运行后再执行此步骤！

#### 4.2 清理未使用的 import
**文件**: `MainActivity.kt`
**删除**:
- `BluetoothAdapter`
- `BluetoothManager`
- `BluetoothSocket`
- `InputStream`
- `java.util.*` (如果未使用)

#### 4.3 删除未使用方法
**文件**: `ui/screen/HistoryScreen.kt`
**删除**:
- `setupHistoryChartStyle()` (第782-848行)

#### 4.4 优化回调接口
**文件**: `data/repository/HistoryRecordRepository.kt`
**修改**:
- 将回调接口改为 Kotlin Flow
- 移除 `setCallback()` 方法

---

## 第五阶段：可选优化（低优先级）

### 5.1 修复 PerformanceMonitor 位置
**选项A**: 将 `PerformanceMonitor` 移到 `ui/monitor/` 包
**选项B**: 分离FPS监控到UI层，保留其他监控在Data层

### 5.2 统一错误处理
创建全局错误处理机制，统一处理网络错误、权限错误等。

### 5.3 完善单元测试
为Repository、ViewModel添加单元测试。

---

## 验证清单

每阶段完成后，验证以下内容：

- [ ] 应用能正常编译
- [ ] 应用能正常启动
- [ ] 蓝牙功能正常
- [ ] 数据上传功能正常
- [ ] 历史记录查询正常
- [ ] 用户登录/登出正常
- [ ] 设置保存正常

---

## 风险评估

| 阶段 | 风险等级 | 主要风险 | 缓解措施 |
|------|---------|---------|---------|
| 第一阶段 | 中 | 数据流改变可能导致功能异常 | 逐个方法迁移，充分测试 |
| 第二阶段 | 低 | UI组件移动可能导致界面问题 | 使用IDE重构工具，保持参数一致 |
| 第三阶段 | 中 | 职责改变可能影响业务逻辑 | 保持原有行为，只移动代码位置 |
| 第四阶段 | 高 | 删除代码可能导致功能丢失 | 确认新代码完全替代后再删除 |

---

## 时间估算

| 阶段 | 预计时间 | 依赖 |
|------|---------|------|
| 第一阶段 | 2-3小时 | 无 |
| 第二阶段 | 2-3小时 | 第一阶段完成 |
| 第三阶段 | 1-2小时 | 无 |
| 第四阶段 | 1小时 | 前三阶段完成并稳定运行 |
| 总计 | 6-9小时 | - |

---

## 开始修复

建议按顺序执行：
1. 先完成第一阶段（消除跨层调用）
2. 然后完成第二阶段（拆分MainActivity）
3. 测试稳定后，进行第三阶段
4. 运行一段时间后，再进行第四阶段（清理遗留代码）

请确认开始哪个阶段？
