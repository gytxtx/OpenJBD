# OpenJBD 设置页轻量分组改造设计

## 目标

将设置页从「分组标题 + 外层描边卡片」调整为更轻的 MD2 分组列表，解决当前层级重复、边框过重的问题，同时保持以下内容不变：

- 分组顺序仍为 `Interface`、`Device`、`Other`。
- 六个设置项的标题、副标题、图标、开关、跳转和弹出菜单行为保持不变。
- `SettingsFragment` 的分组规则、点击逻辑、自动连接开关逻辑和菜单锚点逻辑保持不变。
- 不修改共享样式 `OpenJbdOutlinedCard`，避免影响其他页面。

## 现状

当前 `fragment_settings.xml` 使用三段相同结构：

- 一个 `OpenJbdSectionTitle`
- 一个 `MaterialCardView`
- 卡片内一个带 `@drawable/divider_card_stroke` 的 `LinearLayout`

这导致每组同时出现：

- 分组标题的层级提示
- 外层卡片描边的分组提示
- 行间 divider 的列表提示

同一层级被表达了三次，视觉密度偏重。`row_setting_item.xml` 本身已经具备 48dp 触控下限、16dp 水平内边距、选择态反馈、24dp 图标和副标题层级，不需要再依赖外层卡片建立结构。

## 设计决策

采用你确认的「轻量分组」方案：

- 删除三个 `MaterialCardView` 外层容器。
- 保留三个 `OpenJbdSectionTitle` 作为唯一的组层级提示。
- 每组直接使用现有 `LinearLayout` 承载动态添加的行项目。
- 继续使用现有 `@drawable/divider_card_stroke` 作为组内分隔线。
- 保留当前滚动结构和整体信息密度，不增加说明文案，不引入新的装饰元素。

这是一次视觉层级收敛，不是交互或信息架构调整。

## 布局结构

`fragment_settings.xml` 调整为：

- 根节点仍为 `ScrollView`，继续保持 `clipToPadding="false"` 和 `fillViewport="true"`。
- 内容根 `LinearLayout` 继续保留 `16dp` 左右内边距。
- 三个分组标题继续存在，文本资源保持不变。
- `list_settings_interface`、`list_settings_device`、`list_settings_other` 三个 `LinearLayout` 从卡片内部提升为分组标题后的直接子节点。
- 三个列表容器继续保留 `android:orientation="vertical"`、`android:showDividers="middle"` 和 `android:divider="@drawable/divider_card_stroke"`。
- 原先依附于卡片的底部间距改为直接声明在各列表容器或分组标题间，保证组与组之间仍有稳定呼吸感，但不引入卡片轮廓。

结果是：

- 组间关系靠标题和垂直留白表达。
- 组内关系靠行项目和 divider 表达。
- 不再出现边框与标题重复定义层级。

## 行项目行为

`row_setting_item.xml` 不改结构和交互语义：

- 继续保持 `@dimen/touch_target_min` 的最小高度，精确值为 `48dp`。
- 继续保持 `@dimen/space_16` 左右内边距与 `@dimen/space_10` 上下内边距。
- 继续使用 `?attr/selectableItemBackground` 提供触摸反馈。
- `SwitchMaterial` 和右侧 chevron 的显隐逻辑保持不变。
- 标题、副标题、图标尺寸和当前文本样式保持不变。

因为外层卡片被移除，行项目本身承担全部点击承载职责，但这不会改变 `SettingsFragment.renderSettingsRows()` 已有的点击绑定方式。

## 代码边界

实现阶段只允许触达与设置页布局直接相关的文件：

- `app/src/main/res/layout/fragment_settings.xml`
- 对应的资源测试文件

以下内容不在本次改造范围内：

- `SettingsFragment.java` 的业务分发逻辑
- `row_setting_item.xml` 的现有控件结构
- `popup_setting_menu` 和菜单项布局
- `OpenJbdOutlinedCard` 样式定义
- 参数页、概览页、设备页或其他任何使用卡片的界面

## 视觉与可访问性要求

- 浅色和深色主题下都必须保持分组标题、主文本、副文本和 divider 的可辨识度。
- 行项目点击热区不能因容器调整而缩小。
- 移除卡片后，组边界仍需在滚动中清晰可读，不能出现连续长列表失去分组感的问题。
- 开关项与可点击菜单项在视觉上仍要能区分，继续依赖现有右侧控件状态表达，不新增额外标签。

## 测试与验证

实现完成后需要覆盖以下验证：

### 资源结构验证

- `fragment_settings.xml` 中不再包含三个设置分组对应的 `MaterialCardView`。
- `list_settings_interface`、`list_settings_device`、`list_settings_other` 仍然存在且 ID 不变。
- 三个列表容器仍然使用 `divider_card_stroke` 和 `showDividers="middle"`。
- 三个分组标题仍然存在且文本资源不变。

### 行为回归验证

- 点击主题、语言、温度单位、刷新频率，弹出菜单位置和选中态与当前行为一致。
- 点击自动连接行会切换现有 `SwitchMaterial`，且副标题即时刷新。
- 点击关于行仍进入 `AboutActivity`。

### 视觉验证

- 在设置页顶部、中段、底部滚动位置截屏检查分组关系。
- 在浅色与深色主题下各检查一次。
- 确认去除卡片后没有出现 divider 贴边、标题悬空或组间距不一致的问题。

## 不在范围内

- 不新增设置项。
- 不调整设置项排序。
- 不修改字符串内容。
- 不改动设置菜单选项。
- 不迁移到 Material Design 3。
