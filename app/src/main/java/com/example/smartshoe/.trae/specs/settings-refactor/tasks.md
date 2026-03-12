# SettingScreenSection 重构任务列表

## 阶段1: 创建通用组件 ✅ 已完成

- [x] Task 1: 创建 SettingItemCard 通用组件
  - [x] SubTask 1.1: 提取设置项卡片UI
  - [x] SubTask 1.2: 支持图标、标题、副标题、箭头
  - [x] SubTask 1.3: 支持点击事件和展开状态

- [x] Task 2: 创建 ExpandableSection 通用组件
  - [x] SubTask 2.1: 提取可展开区域动画
  - [x] SubTask 2.2: 使用 AnimationDefaults 统一动画
  - [x] SubTask 2.3: 支持自定义展开内容

## 阶段2: 拆分独立Section ⏸️ 暂停（后续迭代）

- [ ] Task 3: 创建 AccountSettingsSection
  - [ ] SubTask 3.1: 从原文件提取账户相关代码（第90-250行）
  - [ ] SubTask 3.2: 包含登录/注册/编辑资料功能
  - [ ] SubTask 3.3: 使用 rememberSaveable 保存状态

- [ ] Task 4: 创建 DeviceSettingsSection
  - [ ] SubTask 4.1: 从原文件提取设备管理代码（第1250-1400行）
  - [ ] SubTask 4.2: 包含设备列表和连接功能
  - [ ] SubTask 4.3: 使用 LazyColumn 优化列表

- [ ] Task 5: 创建 WeightSettingsSection
  - [ ] SubTask 5.1: 从原文件提取体重设置代码（第1480-1600行）
  - [ ] SubTask 5.2: 包含体重显示和编辑功能
  - [ ] SubTask 5.3: 使用 rememberSaveable 保存编辑状态

- [ ] Task 6: 创建 DataManagementSection
  - [ ] SubTask 6.1: 从原文件提取数据管理代码（第1600-1700行）
  - [ ] SubTask 6.2: 包含数据备份上传功能
  - [ ] SubTask 6.3: 包含本地数据清除功能

- [ ] Task 7: 创建 NotificationSettingsSection
  - [ ] SubTask 7.1: 从原文件提取通知设置代码
  - [ ] SubTask 7.2: 包含压力提醒开关
  - [ ] SubTask 7.3: 状态同步到MainActivity

- [ ] Task 8: 创建 AboutSection
  - [ ] SubTask 8.1: 从原文件提取关于页面代码（第1980-2100行）
  - [ ] SubTask 8.2: 包含版本信息、使用帮助、隐私政策
  - [ ] SubTask 8.3: 使用 rememberSaveable 保存展开状态

## 阶段3: 重构入口文件 ⏸️ 暂停

- [ ] Task 9: 重构 SettingScreenSection.kt 入口
  - [ ] SubTask 9.1: 删除已提取的代码
  - [ ] SubTask 9.2: 导入新的Section组件
  - [ ] SubTask 9.3: 保持原有API不变
  - [ ] SubTask 9.4: 确保所有回调正确传递

## 阶段4: 代码审查和优化 ✅ 已完成

- [x] Task 10: 检查隐藏问题
  - [x] SubTask 10.1: 检查未使用的导入
  - [x] SubTask 10.2: 检查硬编码的字符串和颜色
  - [x] SubTask 10.3: 检查重复的Composable
  - [x] SubTask 10.4: 检查内存泄漏风险

- [x] Task 11: 使用工具类优化
  - [x] SubTask 11.1: 创建 DateTimeUtils 日期工具类
  - [x] SubTask 11.2: 创建 AnimationDefaults 动画配置
  - [x] SubTask 11.3: 创建 ChartConfigUtils 图表配置

## 阶段5: 验证和测试 ✅ 已完成

- [x] Task 12: 编译验证
  - [x] SubTask 12.1: 确保无编译错误
  - [x] SubTask 12.2: 确保无警告

- [x] Task 13: 功能验证（工具类）
  - [x] SubTask 13.1: AnimationDefaults 编译通过
  - [x] SubTask 13.2: DateTimeUtils 编译通过
  - [x] SubTask 13.3: ChartConfigUtils 编译通过
  - [x] SubTask 13.4: SettingItemCard 编译通过
  - [x] SubTask 13.5: ExpandableSection 编译通过

---

## 已完成的优化清单

### 工具类（utils/）
- [x] `AnimationDefaults.kt` - 统一动画配置
- [x] `DateTimeUtils.kt` - 统一日期格式化
- [x] `ChartConfigUtils.kt` - 统一图表配置

### 通用组件（section/settings/）
- [x] `SettingItemCard.kt` - 设置项卡片
- [x] `ExpandableSection.kt` - 可展开区域

### 修复的编译错误
- [x] ChartConfigUtils - setFillColor() 修复
- [x] AnimationDefaults - 动画类型修复

---

## 合并准备状态

**状态**: ✅ 已准备好合并

**包含文件**:
1. `utils/AnimationDefaults.kt`
2. `utils/DateTimeUtils.kt`
3. `utils/ChartConfigUtils.kt`
4. `section/settings/SettingItemCard.kt`
5. `section/settings/ExpandableSection.kt`

**后续工作**:
- 阶段2-3的Section拆分可在后续迭代中进行
- 新的通用组件已为未来重构做好准备

# Task Dependencies
- Task 3-8 依赖 Task 1-2（通用组件）✅ 已完成
- Task 9 依赖 Task 3-8（所有Section）⏸️ 暂停
- Task 10-11 依赖 Task 9（入口重构完成）✅ 已完成工具类
- Task 12-13 依赖 Task 10-11（优化完成）✅ 已完成
