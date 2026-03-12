# SettingScreenSection 重构检查清单

## 通用组件检查

- [ ] SettingItemCard 组件已创建
- [ ] SettingItemCard 支持图标、标题、副标题
- [ ] SettingItemCard 支持展开/收起箭头
- [ ] SettingItemCard 支持点击事件
- [ ] ExpandableSection 组件已创建
- [ ] ExpandableSection 使用 AnimationDefaults 动画
- [ ] ExpandableSection 支持自定义内容

## Section组件检查

- [ ] AccountSettingsSection 已创建
- [ ] AccountSettingsSection 包含登录功能
- [ ] AccountSettingsSection 包含注册功能
- [ ] AccountSettingsSection 包含编辑资料功能
- [ ] AccountSettingsSection 使用 rememberSaveable

- [ ] DeviceSettingsSection 已创建
- [ ] DeviceSettingsSection 显示已连接设备
- [ ] DeviceSettingsSection 显示扫描设备列表
- [ ] DeviceSettingsSection 使用 LazyColumn

- [ ] WeightSettingsSection 已创建
- [ ] WeightSettingsSection 显示当前体重
- [ ] WeightSettingsSection 支持编辑体重
- [ ] WeightSettingsSection 使用 rememberSaveable

- [ ] DataManagementSection 已创建
- [ ] DataManagementSection 支持数据备份上传
- [ ] DataManagementSection 支持本地数据清除
- [ ] DataManagementSection 显示数据状态

- [ ] NotificationSettingsSection 已创建
- [ ] NotificationSettingsSection 包含压力提醒开关
- [ ] NotificationSettingsSection 状态同步正确

- [ ] AboutSection 已创建
- [ ] AboutSection 显示版本信息
- [ ] AboutSection 显示使用帮助
- [ ] AboutSection 显示隐私政策
- [ ] AboutSection 使用 rememberSaveable

## 入口文件检查

- [ ] SettingScreenSection.kt 已重构
- [ ] 已删除提取的代码
- [ ] 已导入新的Section组件
- [ ] API保持不变
- [ ] 所有回调正确传递

## 代码质量检查

- [ ] 无未使用的导入
- [ ] 无硬编码字符串（已提取到strings.xml）
- [ ] 无硬编码颜色（已使用UIConstants）
- [ ] 无重复Composable
- [ ] 使用 DateTimeUtils 替换日期格式化
- [ ] 使用 AnimationDefaults 替换动画配置

## 编译检查

- [ ] 无编译错误
- [ ] 无编译警告
- [ ] 所有导入正确

## 功能测试

- [ ] 账户登录功能正常
- [ ] 账户注册功能正常
- [ ] 编辑资料功能正常
- [ ] 设备连接功能正常
- [ ] 设备断开功能正常
- [ ] 体重编辑功能正常
- [ ] 数据备份功能正常
- [ ] 数据清除功能正常
- [ ] 压力提醒开关正常
- [ ] 关于页面展开正常
- [ ] 版本信息显示正确

## 性能检查

- [ ] 无内存泄漏
- [ ] LazyColumn 正确复用
- [ ] 动画流畅
- [ ] 页面加载快速
