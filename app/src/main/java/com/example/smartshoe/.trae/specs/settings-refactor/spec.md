# SettingScreenSection 重构 Spec

## Why
SettingScreenSection.kt 约2300行，包含20+个Composable函数，单文件难以维护。需要拆分为独立的组件文件，提高代码可维护性和团队协作效率。

## What Changes
- 将 SettingScreenSection.kt 拆分为6个独立的Section文件
- 提取通用组件：SettingItemCard、ExpandableSection
- 保留SettingScreenSection.kt作为入口文件，只包含组合逻辑
- 所有拆分后的文件控制在300行以内

## Impact
- Affected code: SettingScreenSection.kt
- New files: section/settings/ 目录下的6个新文件
- No breaking changes，保持原有API兼容

## ADDED Requirements

### Requirement: AccountSettingsSection
The system SHALL 提供独立的账户设置组件

#### Scenario: 账户设置显示
- **WHEN** 用户进入设置页面
- **THEN** 显示账户登录/注册/编辑资料功能

### Requirement: DeviceSettingsSection
The system SHALL 提供独立的设备管理组件

#### Scenario: 蓝牙设备管理
- **WHEN** 用户展开设备管理
- **THEN** 显示已连接设备和扫描到的设备列表

### Requirement: WeightSettingsSection
The system SHALL 提供独立的体重设置组件

#### Scenario: 体重数据编辑
- **WHEN** 用户点击体重设置
- **THEN** 显示当前体重并允许编辑

### Requirement: DataManagementSection
The system SHALL 提供独立的数据管理组件

#### Scenario: 数据备份和清除
- **WHEN** 用户展开数据管理
- **THEN** 显示数据备份上传和本地数据清除功能

### Requirement: NotificationSettingsSection
The system SHALL 提供独立的通知设置组件

#### Scenario: 压力提醒开关
- **WHEN** 用户查看通知设置
- **THEN** 显示压力提醒开关

### Requirement: AboutSection
The system SHALL 提供独立的关于页面组件

#### Scenario: 应用信息展示
- **WHEN** 用户展开关于页面
- **THEN** 显示版本信息、使用帮助、隐私政策

### Requirement: 通用组件
The system SHALL 提供可复用的设置项卡片和展开区域组件

#### Scenario: 统一UI风格
- **WHEN** 多个设置项使用相同样式
- **THEN** 使用SettingItemCard和ExpandableSection保持风格一致

## MODIFIED Requirements

### Requirement: SettingScreenSection入口
**Current**: 包含所有设置功能的2300行单文件
**Modified**: 只保留组合逻辑，调用独立的Section组件

## REMOVED Requirements

### Requirement: 无
**Reason**: 本次重构只移动代码，不删除功能
**Migration**: 所有功能保持完整
