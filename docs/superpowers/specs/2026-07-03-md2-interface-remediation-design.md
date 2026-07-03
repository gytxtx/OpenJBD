# OpenJBD MD2 界面修复设计

## 目标

在不改变 BLE、页面导航和数据展示业务逻辑的前提下，使现有 Android Views 界面与 Material Design 2 的主题、排版、形状、层级、组件状态及无障碍要求保持一致。

## 范围

- 补全浅色与深色主题的 MD2 颜色角色。
- 将主工具栏统一为 MD2 Primary Top App Bar。
- 使用 MD2 排版样式替代布局中的硬编码常规字号。
- 统一卡片、菜单和底部导航的 MD2 elevation。
- 为深色主题提供按 elevation 区分的 surface 资源。
- 明确装饰图标的无障碍语义。
- 尊重系统的减少动画设置。
- 增加静态资源测试，验证主题角色和关键组件令牌。

不修改以下内容：

- 页面信息架构和底部三个导航目的地。
- BLE 扫描、连接、重连和协议处理。
- Dashboard 的数据布局及大字号远距离阅读用途。
- 当前语言、温度单位和刷新频率设置行为。

## 主题与颜色

`AppTheme` 继续继承 `Theme.MaterialComponents.DayNight.NoActionBar`。

主题必须显式映射以下 MD2 角色：

- `colorPrimary`
- `colorPrimaryVariant`
- `colorSecondary`
- `colorSecondaryVariant`
- `android:colorBackground`
- `colorSurface`
- `colorError`
- `colorOnPrimary`
- `colorOnSecondary`
- `colorOnSurface`
- `colorOnBackground`
- `colorOnError`

主工具栏使用 `colorPrimary` 背景及 `colorOnPrimary` 标题、图标。状态栏使用 `colorPrimaryVariant`。

深色主题保留 `#121212` 作为 background 基准，并为 1dp、2dp、4dp、8dp、12dp、16dp elevation 建立独立 surface 资源。组件根据其 MD2 elevation 使用对应 surface，避免所有层级共享单一固定颜色。

所有最终颜色组合必须满足：

- 正文文本对比度不低于 4.5:1。
- 大文本、图标和组件边界对比度不低于 3:1。

## 排版

新增项目级 MD2 `TextAppearance`，只组合 Material Components 已有的 MD2 类型：

- `Headline3`
- `Headline4`
- `Headline5`
- `Headline6`
- `Subtitle1`
- `Subtitle2`
- `Body1`
- `Body2`
- `Caption`
- `Button`

普通页面标题、空状态标题、指标标题、指标值、说明文字和菜单项必须引用这些样式。Dashboard 的 58sp、80sp 自适应大字号属于其远距离仪表用途，保留为页面专用显示样式。

按钮采用 MD2 Button 类型样式。英文按钮使用 Material Components 的默认大写转换；中文文本不受大小写转换影响。

## 形状与层级

- 小型组件与普通卡片使用 4dp 圆角。
- 普通卡片使用 2dp elevation。
- Top App Bar 使用 4dp elevation。
- 菜单使用 8dp elevation。
- Bottom Navigation 使用 8dp elevation，与 Material Components MD2 默认实现保持一致。
- Dialog 使用 Material Components 的 MD2 默认形状和 24dp elevation 行为，不自定义 M3 形状令牌。

## 组件与状态

- Bottom Navigation 保持三个目的地、56dp 高度及常显标签。
- 设置项和设备项保持不低于 48dp 的可点击区域。
- 设置开关继续由整行点击触发，开关本身不形成重复焦点。
- 重连取消操作继续使用 Text Button。
- 所有可点击组件使用 Material/Android selectable background，以提供 pressed、focused 和 selected 状态。
- 动态连接状态继续由可见文本表达，不只依赖颜色。

## 无障碍

- 可操作的纯图标控件必须具有 `contentDescription`。
- 行项目中的前导图标和箭头标记为装饰内容，不单独进入无障碍树。
- Bottom Navigation 依赖 Material Components 提供选中状态语义。
- 状态变化文本使用适当的 live region，使连接状态能够被辅助技术获知。
- 动画启动前读取系统 animator duration scale；禁用动画时直接应用最终状态。
- 所有交互区域不小于 48dp × 48dp。

## 验证

### 自动验证

- 新增 JVM 静态资源测试，读取真实 XML 并验证：
  - MD2 主题角色齐全。
  - Toolbar 使用 `colorPrimary` 和 `colorOnPrimary`。
  - 装饰图标具有明确无障碍标记。
  - 普通布局不再使用本次纳入治理的硬编码字号。
- 运行现有单元测试。
- 在 Android SDK 可用时运行 `assembleDebug` 与 `lintDebug`。

### 人工静态审计

逐页检查：

- MainActivity
- DeviceListActivity
- OverviewFragment
- ParametersFragment
- SettingsFragment
- AboutActivity
- LicensesActivity
- DashboardActivity

按 MD2 的颜色、排版、形状、层级、组件、状态和无障碍清单复核。由于当前没有连接的 ADB 设备，实际设备渲染验证不作为本次静态修复已完成的证明；若后续连接设备，应补充浅色和深色截图验证。

## 风险控制

- 仅修改资源与表现层代码，避免影响 BLE 和协议功能。
- 资源测试使用精确资源名和 XML 属性，不进行大小写或结构推断。
- 不引入 M3 tonal palette、surface container、M3 type scale 或 Expressive motion。
