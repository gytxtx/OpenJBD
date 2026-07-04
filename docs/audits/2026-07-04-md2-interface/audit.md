# OpenJBD MD2 界面审计

## 审计范围

- 设备：Pixel 5 Android 14 模拟器
- 分辨率：1080 × 2340
- 密度：440dpi
- 主题：浅色
- 主要任务：检查主界面、参数页、设置页、设备选择和关于页，重点检查 App Bar
- 规范范围：Material Design 2

## 流程步骤

1. `01-launch.png`：启动应用，显示总览空状态。健康度：一般。
2. `02-parameters.png`：切换到参数页空状态。健康度：一般；该截图出现渲染异常，不用于尺寸判断。
3. `03-settings.png`：切换到设置页。健康度：良好。
4. `04-device-list.png`：进入设备列表并显示系统权限请求。健康度：一般。
5. `05-device-list-denied.png`：拒绝权限后的设备列表状态。健康度：较差。
6. `06-about.png`：进入关于页。健康度：良好。

## 已确认的优点

- `Widget.OpenJbd.Toolbar` 使用 `colorPrimary`、`colorOnPrimary` 和 4dp elevation，浅色主题中的主色层级清楚。
- 关于页的 `about_top_app_bar` 实际高度为 154px，即 56dp，符合 M2 标准单行 Top App Bar。
- 主界面 Bottom Navigation 实际高度为 154px，即 56dp；三个目的地始终显示图标和标签，当前选中状态清楚。
- 设置页使用一致的标题、列表行、分隔线和 48dp 以上触控区域。
- 总览、参数和设备列表均提供明确的空状态标题与说明，不只依赖图标表达状态。

## 主要问题

### P1：Toolbar 导航按钮没有可访问名称

证据：

- `01-launch-ui.xml` 中主界面导航 `ImageButton` 的 `content-desc` 为空，并被标记为 `NAF="true"`。
- `06-about-ui.xml` 中关于页返回 `ImageButton` 的 `content-desc` 为空，并被标记为 `NAF="true"`。
- `activity_main.xml`、`activity_device_list.xml`、`activity_about.xml` 和 `activity_licenses.xml` 只设置了 `app:navigationIcon`，没有设置导航内容说明。

影响：

- TalkBack 无法说明该按钮是“选择设备”还是“返回”。
- 图标本身不足以形成可访问名称。

建议：

- 主界面 `top_app_bar` 使用精确的“选择设备”字符串作为 navigation content description。
- `device_top_app_bar`、`about_top_app_bar` 和 `licenses_top_app_bar` 使用现有返回操作字符串作为 navigation content description。
- 增加资源测试，检查所有 `MaterialToolbar` 的导航图标均有非空可访问名称。

### P1：权限拒绝后的设备页同时表达两个互相冲突的状态

证据：

- `05-device-list-denied.png` 顶部状态文本显示“BLE 权限被拒绝。”
- 同一屏幕中央仍显示“正在扫描设备”和“请靠近 BMS，并确认蓝牙已开启。”
- 底部 Toast 再次显示“BLE 权限被拒绝”。

影响：

- 用户无法判断应用是否仍在扫描。
- 页面没有给出恢复权限的下一步操作。

建议：

- 权限被拒绝后，将 `txt_device_placeholder_title` 和 `txt_device_placeholder_subtitle` 切换为权限错误状态。
- 停止使用扫描中的图标和文案。
- 提供明确操作：重新请求权限；若系统不再显示权限对话框，则打开应用设置。

### P2：主界面 App Bar 使用非标准 72dp 双行结构

证据：

- `top_app_bar` 的 UI bounds 为 `[0,136][1080,334]`，高度 198px，即 72dp。
- `activity_main.xml` 直接声明 `android:layout_height="72dp"`，并同时显示标题和副标题。
- `device_top_app_bar` 使用 `@dimen/list_top_app_bar_height`，该资源值也是 72dp。
- 关于页使用 `@dimen/top_app_bar_height`，值为 56dp。

影响：

- 项目中形成 72dp 双行栏和 56dp 单行栏两套垂直节奏。
- 72dp 不是当前项目所引用的 M2 标准单行 Top App Bar 尺寸。
- 主界面每个底部目的地都重复显示固定副标题“本地 BMS 监控”，占用首屏内容高度但没有增加当前任务信息。

建议：

- 主界面改为 56dp 单行栏，仅显示当前页面标题。
- 设备名称或连接状态放入页面状态区域，避免用固定副标题扩高全局导航栏。
- 设备列表若必须保留设备类别说明，可将说明移至 Toolbar 下方的 `txt_device_status` 区域。

### P2：主界面导航图标水平位置与其他 App Bar 不一致

证据：

- `activity_main.xml` 在 `MaterialToolbar` 上设置 `android:paddingStart="16dp"`。
- 主界面导航按钮 bounds 为 `[44,153][198,307]`，图标中心约为 44dp。
- 关于页没有这项额外 padding，导航按钮 bounds 为 `[0,136][154,290]`，中心为 28dp。
- 两者使用同一个 `Widget.OpenJbd.Toolbar`。

影响：

- 主界面列表按钮比关于页和设备列表页返回按钮向右偏移 16dp。
- 标题起始位置也随之向右移动，页面切换时 App Bar 对齐关系不一致。

建议：

- 删除 `top_app_bar` 上额外的 `android:paddingStart` 和 `android:paddingEnd`。
- 让 `MaterialToolbar` 自身处理导航图标、标题和菜单的标准 inset。

### P3：设备列表错误状态缺少持久操作入口

证据：

- `05-device-list-denied.png` 只有状态文本和刷新图标。
- 刷新操作的含义在权限被拒绝时不明确，页面没有说明它是否会重新请求权限。

建议：

- 错误空状态增加文本按钮，例如“授予权限”。
- 如果权限被永久拒绝，按钮文案切换为“打开应用设置”。

## App Bar 结论

当前颜色、前景色和 elevation 已符合项目的 M2 主题约束；主要问题不在视觉皮肤，而在结构与语义：

1. 主界面和设备列表使用 72dp 双行栏，与关于页、许可证页的 56dp 单行栏不一致。
2. 主界面的额外 16dp 水平 padding 破坏了 MaterialToolbar 默认 inset。
3. Toolbar 导航按钮缺少可访问名称。
4. 固定副标题占用 App Bar 空间，却没有传达当前连接对象或可操作状态。

## 无法从本次截图确认的事项

- 未连接真实 BMS，因此未检查已连接状态、Dashboard 入口、断开操作和数据密集页面。
- 未检查深色主题的实际 elevation overlay。
- 未执行 TalkBack、外接键盘或字体放大测试；截图和 UI hierarchy 不能证明完整无障碍合规。
- `02-parameters.png` 存在一次模拟器截图渲染异常，不用于颜色或布局尺寸结论。

## 修复验证

修复后使用同一 Pixel 5 Android 14 模拟器重新安装应用，并清除应用数据后复核。

### 总览

- 截图：`07-fixed-overview.png`
- UI hierarchy：`07-fixed-overview-ui.xml`
- `top_app_bar` bounds 为 `[0,136][1080,290]`，高度 154px，即 56dp。
- 导航按钮 bounds 为 `[0,136][154,290]`，不再包含额外 16dp 水平偏移。
- 导航按钮 `content-desc` 为“选择设备”。
- App Bar 只显示当前页面标题“总览”，不再显示固定副标题。

### 权限拒绝

- 截图：`08-fixed-permission-denied.png`
- UI hierarchy：`08-fixed-permission-denied-ui.xml`
- `device_top_app_bar` bounds 为 `[0,136][1080,290]`，高度 154px，即 56dp。
- 返回按钮 `content-desc` 为“返回”。
- 页面显示“需要蓝牙权限”和“授予蓝牙权限后才能扫描附近的 BMS 设备。”
- 页面显示“授予权限”操作。
- UI hierarchy 中不存在“正在扫描设备”文本，权限错误状态不再与扫描状态冲突。

### 关于

- 截图：`09-fixed-about.png`
- UI hierarchy：`09-fixed-about-ui.xml`
- `about_top_app_bar` bounds 为 `[0,136][1080,290]`，高度 154px，即 56dp。
- 返回按钮 `content-desc` 为“返回”。

### 自动验证

- `:app:testDebugUnitTest`：通过。
- `:app:assembleDebug`：通过。
- `:app:lintDebug`：通过。

真实 BMS 已连接状态、深色主题、TalkBack、外接键盘和字体放大仍属于后续人工验证范围。
