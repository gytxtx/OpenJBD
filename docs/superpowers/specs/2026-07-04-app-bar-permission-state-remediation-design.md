# OpenJBD App Bar 与权限状态修复设计

## 目标

修复 2026-07-04 MD2 界面审计确认的全部明确问题：

- 主界面与设备列表使用非标准 72dp 双行 App Bar。
- 主界面 Toolbar 的额外水平 padding 导致导航图标和标题偏移。
- Toolbar 导航按钮缺少可访问名称。
- BLE 权限被拒绝后，设备页仍显示扫描中的空状态。
- 权限错误状态缺少持久、明确的恢复操作。

保持现有 BLE 扫描、设备选择、底部导航和其他页面业务行为不变。

## App Bar

`top_app_bar` 和 `device_top_app_bar` 统一使用现有
`@dimen/top_app_bar_height`，其精确值为 56dp。

主界面：

- 删除 `android:paddingStart`、`android:paddingTop`、
  `android:paddingEnd` 和 `android:paddingBottom`。
- 删除 `app:subtitle`。
- 保留当前页面标题、设备选择导航按钮和已有菜单操作。
- `MainActivity.updateToolbar()` 不再调用 `setSubtitle()`。
- 连接状态继续由现有页面状态和空状态表达。

设备列表：

- 将 `android:layout_height` 从 `@dimen/list_top_app_bar_height`
  改为 `@dimen/top_app_bar_height`。
- 删除上下 padding 和 `app:subtitle`。
- 设备类型说明不迁移到新的永久区域；现有 `txt_device_status`
  继续表达扫描、权限、蓝牙和定位状态。
- `DeviceListActivity.setStatus(String)` 不再重设 Toolbar 副标题。

关于页和许可证页已经使用 `@dimen/top_app_bar_height`，保持高度不变。

## Toolbar 导航语义

新增字符串资源：

- `action_select_device`
  - 英文：`Select device`
  - 简体中文：`选择设备`

导航内容说明映射：

- `top_app_bar` → `@string/action_select_device`
- `device_top_app_bar` → `@string/action_back`
- `about_top_app_bar` → `@string/action_back`
- `licenses_top_app_bar` → `@string/action_back`

内容说明直接声明在对应 `MaterialToolbar` 上，不依赖图标名称或运行时推断。

## 权限错误空状态

在 `activity_device_list.xml` 的 `placeholder_devices` 中新增
`MaterialButton`，精确 ID 为 `btn_device_permission_action`。

新增字符串资源：

- `device_placeholder_permission_denied_title`
  - 英文：`Bluetooth permission required`
  - 简体中文：`需要蓝牙权限`
- `device_placeholder_permission_denied_subtitle`
  - 英文：`Allow Bluetooth permission to scan for nearby BMS devices.`
  - 简体中文：`授予蓝牙权限后才能扫描附近的 BMS 设备。`
- `action_grant_permission`
  - 英文：`Grant permission`
  - 简体中文：`授予权限`
- `action_open_app_settings`
  - 英文：`Open app settings`
  - 简体中文：`打开应用设置`

按钮使用 MD2 Text Button，默认 `visibility="gone"`。所有现有
`showPlaceholder(...)` 路径都隐藏该按钮，避免扫描中、蓝牙关闭、
定位关闭、扫描器不可用和未找到设备状态遗留权限操作。

权限被拒绝时使用独立方法显示权限错误状态：

- 停止刷新动画。
- 设置 `status_ble_permission_denied`。
- 将占位图标设为现有 `ic_bluetooth_disabled_24`。
- 显示权限错误标题和说明。
- 显示 `btn_device_permission_action`。
- 不显示扫描中标题或说明。

## 权限恢复决策

权限恢复决策只使用 Android 提供的精确权限状态：

1. 如果全部 `requiredPermissions()` 已授予，直接开始扫描。
2. 如果任一缺失权限的 `shouldShowRequestPermissionRationale(...)`
   返回 `true`：
   - 按钮文本为 `action_grant_permission`。
   - 点击后调用 `requestPermissions(requiredPermissions(),
     REQUEST_BLE_PERMISSIONS)`。
3. 如果所有缺失权限均不能显示权限说明：
   - 按钮文本为 `action_open_app_settings`。
   - 点击后使用 `Settings.ACTION_APPLICATION_DETAILS_SETTINGS` 和
     当前应用的 `package:` URI 打开应用详情页。

首次进入设备列表且权限尚未请求时，保留现有自动请求权限行为。
动态恢复按钮只在权限请求结果为拒绝后显示。

权限恢复决策提取为精确的包级静态方法：

```java
static boolean shouldOpenAppSettings(boolean canShowAnyPermissionRationale)
```

该方法只在权限请求已经返回拒绝结果后调用，不参与首次权限请求。

点击“打开应用设置”时设置 `waitingForAppSettings = true`。随后只在
`waitingForAppSettings` 为 `true` 的 `onResume()` 中检查权限，避免首次进入
页面或系统权限对话框关闭时触发重复扫描或重复错误状态：

- 权限已经授予时开始扫描。
- 权限仍缺失时继续显示权限错误状态。
- 完成检查后将 `waitingForAppSettings` 设为 `false`。

## 测试

### 资源测试

扩展 `Material2ResourceTest`，精确验证：

- `top_app_bar` 和 `device_top_app_bar` 使用
  `@dimen/top_app_bar_height`。
- 主界面 Toolbar 不含 `android:paddingStart`、
  `android:paddingEnd` 和 `app:subtitle`。
- 设备 Toolbar 不含 `app:subtitle`。
- 四个带导航图标的 `MaterialToolbar` 都具有预期的
  `app:navigationContentDescription`。
- `btn_device_permission_action` 存在、默认隐藏并使用 MD2 Text Button。
- 新增的中英文字符串资源存在且值精确。

### 权限决策测试

对 `shouldOpenAppSettings(boolean)` 使用 JVM 测试覆盖：

- 可显示权限说明时返回重新请求权限。
- 不可显示权限说明时返回打开应用设置。

### 验证

- 运行 `:app:testDebugUnitTest`。
- 运行 `:app:assembleDebug`。
- 运行 `:app:lintDebug`。
- 在 Pixel 5 Android 14 模拟器上重新安装应用。
- 截取并检查总览、设备权限拒绝和关于页。
- 通过 UI hierarchy 确认 App Bar 高度为 56dp，导航按钮具有非空
  `content-desc`，权限拒绝状态不再显示扫描中文案。

## 不在范围内

- 不修改 Dashboard 数据布局。
- 不修改 BLE 扫描过滤器、协议解析或连接流程。
- 不迁移到 Material Design 3。
- 不重新设计设置页、关于页或 Bottom Navigation。
- 不声明完整 WCAG 合规；TalkBack 和外接键盘仍需要单独人工测试。
