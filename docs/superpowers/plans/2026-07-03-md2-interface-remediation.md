# OpenJBD MD2 Interface Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 OpenJBD 现有 Android Views 界面的明显 MD2 规范问题，并用自动检查和全量静态审计验证结果。

**Architecture:** 保留现有 Activity、Fragment 与业务逻辑，通过 Android 颜色、样式和布局资源建立完整 MD2 视觉令牌。使用 JVM 测试直接读取真实资源 XML，约束主题角色、工具栏、排版及无障碍属性；表现层 Java 只增加系统动画缩放判断。

**Tech Stack:** Java、Android Views、Material Components 1.10.0、Android XML resources、JUnit 4

---

### Task 1: 建立 MD2 资源约束测试

**Files:**
- Create: `app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java`
- Test: `app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java`

- [ ] **Step 1: 编写失败测试**

测试从 `src/main/res` 精确读取：

```java
@Test
public void appThemeDefinesCompleteMaterial2ColorRoles() throws Exception {
    Document document = parse("values/styles.xml");
    Element style = style(document, "AppTheme");
    assertItem(style, "colorSecondaryVariant", "@color/accent_dark");
    assertItem(style, "colorError", "@color/error");
    assertItem(style, "colorOnError", "@color/on_error");
}

@Test
public void toolbarUsesPrimaryMaterial2Colors() throws Exception {
    Document document = parse("values/styles.xml");
    Element style = style(document, "Widget.OpenJbd.Toolbar");
    assertItem(style, "android:background", "?attr/colorPrimary");
    assertItem(style, "titleTextColor", "?attr/colorOnPrimary");
    assertItem(style, "navigationIconTint", "?attr/colorOnPrimary");
}

@Test
public void decorativeRowIconsAreExcludedFromAccessibilityTree() throws Exception {
    assertDecorativeImage("layout/row_setting_item.xml", "img_setting_icon");
    assertDecorativeImage("layout/row_setting_item.xml", "img_setting_chevron");
    assertDecorativeImage("layout/row_device_list_item.xml", "img_device_type");
}
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.ui.Material2ResourceTest`

Expected: 测试因缺少 `colorSecondaryVariant`、Toolbar 仍使用 `@color/app_bg`、装饰图标缺少 `importantForAccessibility="no"` 而失败。

- [ ] **Step 3: 提交测试**

```powershell
git add -- app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java
git commit -m "test: 添加 MD2 资源约束"
```

### Task 2: 补全 MD2 颜色主题与 elevation surface

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values-night/colors.xml`
- Modify: `app/src/main/res/values/styles.xml`
- Modify: `app/src/main/res/values-v23/styles.xml`
- Modify: `app/src/main/res/values-night-v23/styles.xml`
- Test: `app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java`

- [ ] **Step 1: 扩展失败测试**

精确断言浅色与深色资源均包含：

```java
assertColor("values/colors.xml", "accent_dark", "#007A55");
assertColor("values/colors.xml", "error", "#B00020");
assertColor("values/colors.xml", "on_error", "#FFFFFF");
assertColor("values-night/colors.xml", "app_bg", "#121212");
assertColor("values-night/colors.xml", "surface_elevation_2", "#242424");
assertColor("values-night/colors.xml", "surface_elevation_4", "#272727");
assertColor("values-night/colors.xml", "surface_elevation_8", "#2D2D2D");
```

- [ ] **Step 2: 运行测试并确认新增断言失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.ui.Material2ResourceTest`

Expected: 颜色资源断言失败。

- [ ] **Step 3: 实现完整颜色角色**

浅色资源增加 `accent_dark`、`error`、`on_error` 和 elevation surface；深色背景改为 `#121212`，按 MD2 白色 overlay 结果增加 `surface_elevation_1`、`surface_elevation_2`、`surface_elevation_4`、`surface_elevation_8`、`surface_elevation_12`、`surface_elevation_16`。

三个 `AppTheme` 定义必须映射：

```xml
<item name="colorSecondaryVariant">@color/accent_dark</item>
<item name="colorError">@color/error</item>
<item name="colorOnError">@color/on_error</item>
```

- [ ] **Step 4: 运行测试并确认通过**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.ui.Material2ResourceTest`

Expected: 颜色主题相关测试通过。

### Task 3: 统一 Toolbar、卡片和菜单层级

**Files:**
- Modify: `app/src/main/res/values/styles.xml`
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/layout/activity_device_list.xml`
- Modify: `app/src/main/res/layout/activity_about.xml`
- Modify: `app/src/main/res/layout/activity_licenses.xml`
- Modify: `app/src/main/res/layout/popup_setting_menu.xml`
- Modify: `app/src/main/java/com/gytxtx/openjbd/MainActivity.java`
- Test: `app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java`

- [ ] **Step 1: 扩展失败测试**

断言 `Widget.OpenJbd.Toolbar`：

```xml
android:background=?attr/colorPrimary
titleTextColor=?attr/colorOnPrimary
subtitleTextColor=?attr/colorOnPrimary
navigationIconTint=?attr/colorOnPrimary
itemIconTint=?attr/colorOnPrimary
elevation=4dp
```

断言普通卡片使用 `@color/surface_elevation_2`，菜单使用 `@color/surface_elevation_8`。

- [ ] **Step 2: 运行测试并确认失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.ui.Material2ResourceTest`

Expected: Toolbar 和 surface elevation 断言失败。

- [ ] **Step 3: 修改资源与运行时代码**

删除布局中覆盖 Toolbar 内容色的 `app:titleTextColor`、`app:subtitleTextColor`、`app:navigationIconTint`、`app:itemIconTint`。`MainActivity` 中 Toolbar 图标 tint 改为 `R.color.on_primary`，确保动态设置不覆盖主题语义。

- [ ] **Step 4: 运行测试并确认通过**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.ui.Material2ResourceTest`

Expected: Toolbar 与 elevation 测试通过。

### Task 4: 统一 MD2 排版

**Files:**
- Modify: `app/src/main/res/values/styles.xml`
- Modify: `app/src/main/res/layout/fragment_overview.xml`
- Modify: `app/src/main/res/layout/fragment_parameters.xml`
- Modify: `app/src/main/res/layout/activity_device_list.xml`
- Modify: `app/src/main/res/layout/view_metric_voltage.xml`
- Modify: `app/src/main/res/layout/view_metric_current.xml`
- Modify: `app/src/main/res/layout/view_metric_power.xml`
- Modify: `app/src/main/res/layout/view_metric_cycles.xml`
- Modify: `app/src/main/res/layout/view_metric_capacity.xml`
- Modify: `app/src/main/res/layout/view_metric_pack.xml`
- Modify: `app/src/main/res/layout/row_cell_voltage.xml`
- Test: `app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java`

- [ ] **Step 1: 编写硬编码字号失败测试**

测试扫描上述普通页面布局，若存在 `android:textSize` 即失败。`activity_dashboard.xml` 明确排除，因为其自适应大字号属于远距离仪表用途。

- [ ] **Step 2: 运行测试并确认失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.ui.Material2ResourceTest`

Expected: 输出首个仍含 `android:textSize` 的文件和元素。

- [ ] **Step 3: 新增并应用项目级 MD2 TextAppearance**

在 `styles.xml` 定义语义样式，例如：

```xml
<style name="TextAppearance.OpenJbd.EmptyTitle" parent="TextAppearance.MaterialComponents.Headline5" />
<style name="TextAppearance.OpenJbd.EmptyBody" parent="TextAppearance.MaterialComponents.Body1" />
<style name="TextAppearance.OpenJbd.MetricLabel" parent="TextAppearance.MaterialComponents.Caption" />
<style name="TextAppearance.OpenJbd.MetricValue" parent="TextAppearance.MaterialComponents.Headline6" />
<style name="TextAppearance.OpenJbd.Status" parent="TextAppearance.MaterialComponents.Body2" />
```

布局通过 `android:textAppearance` 使用语义样式；不改变文本内容和数据绑定 ID。

- [ ] **Step 4: 运行测试并确认通过**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.ui.Material2ResourceTest`

Expected: 普通页面无硬编码字号。

### Task 5: 完善无障碍语义和减少动画

**Files:**
- Modify: `app/src/main/res/layout/row_setting_item.xml`
- Modify: `app/src/main/res/layout/row_device_list_item.xml`
- Modify: `app/src/main/res/layout/row_about_list_item.xml`
- Modify: `app/src/main/res/layout/fragment_overview.xml`
- Modify: `app/src/main/java/com/gytxtx/openjbd/MainActivity.java`
- Test: `app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java`
- Create: `app/src/test/java/com/gytxtx/openjbd/AnimatorScaleTest.java`

- [ ] **Step 1: 编写失败测试**

资源测试断言装饰图标同时具有：

```xml
android:contentDescription="@null"
android:importantForAccessibility="no"
```

`AnimatorScaleTest` 对纯 Java 方法 `animationsEnabled(float animatorDurationScale)` 断言 `0f` 返回 `false`，正值返回 `true`。

- [ ] **Step 2: 运行测试并确认失败**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.ui.Material2ResourceTest --tests com.gytxtx.openjbd.AnimatorScaleTest`

Expected: 装饰图标属性和动画判断方法缺失导致失败。

- [ ] **Step 3: 实现无障碍与动画判断**

为装饰图标增加精确属性。连接状态文本增加 `android:accessibilityLiveRegion="polite"`。

在 `MainActivity` 增加：

```java
static boolean animationsEnabled(float animatorDurationScale) {
    return animatorDurationScale > 0f;
}
```

通过 `Settings.Global.ANIMATOR_DURATION_SCALE` 读取系统值；为 0 时跳过页面和 Bottom Navigation 缩放动画，直接应用最终状态。

- [ ] **Step 4: 运行测试并确认通过**

Run: `.\gradlew.bat :app:testDebugUnitTest`

Expected: 全部 JVM 测试通过。

### Task 6: 构建、Lint 与 MD2 完成审计

**Files:**
- Modify: 仅修改验证发现仍有问题的文件

- [ ] **Step 1: 运行格式与资源扫描**

Run:

```powershell
git diff --check
rg -n 'android:textSize=' app/src/main/res/layout -g '!activity_dashboard.xml'
rg -n '#[0-9A-Fa-f]{6,8}' app/src/main/res/layout
```

Expected: 无空白错误；普通页面无硬编码字号；除 Dashboard 的专用黑色界面外，布局无硬编码颜色。

- [ ] **Step 2: 运行完整单元测试**

Run: `.\gradlew.bat :app:testDebugUnitTest`

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: 构建 Debug APK**

Run: `.\gradlew.bat :app:assembleDebug`

Expected: `BUILD SUCCESSFUL`，生成 `app/build/outputs/apk/debug/app-debug.apk`。

- [ ] **Step 4: 运行 Android Lint**

Run: `.\gradlew.bat :app:lintDebug`

Expected: `BUILD SUCCESSFUL`，无 error。

- [ ] **Step 5: 按 Material Design 技能清单复核**

逐项确认颜色角色、MD2 typography、4dp shape、shadow elevation、Bottom Navigation、Toolbar、菜单、按钮状态、48dp 触摸区域、对比度、装饰图片语义、live region 和减少动画行为。

- [ ] **Step 6: 提交实现**

```powershell
git add -- app/src/main app/src/test
git commit -m "fix: 统一界面至 Material Design 2"
```
