# Settings Lightweight Groups Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将设置页从外层描边卡片分组改为轻量分组列表，同时保持现有设置项内容、点击行为、菜单锚点和开关逻辑不变。

**Architecture:** 这次改造只调整 `fragment_settings.xml` 的资源结构，并用现有 `Material2ResourceTest` 固化布局约束。`SettingsFragment.java` 继续动态向 `list_settings_interface`、`list_settings_device`、`list_settings_other` 添加现有 `row_setting_item`，不改分组规则和事件处理。

**Tech Stack:** Java、Android Views、Android XML resources、Material Components 1.10.0、JUnit 4

---

### Task 1: 建立设置页轻量分组资源约束

**Files:**
- Modify: `app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java`
- Test: `app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java`

- [ ] **Step 1: 编写设置页结构失败测试**

在 `Material2ResourceTest` 增加一个新测试方法：

```java
@Test
public void settingsScreenUsesLightweightGroupsWithoutOutlinedCards() throws Exception {
    Document document = parse("layout/fragment_settings.xml");

    assertTextViewExists(document, "@string/settings_group_interface");
    assertTextViewExists(document, "@string/settings_group_device");
    assertTextViewExists(document, "@string/settings_group_other");

    assertLinearLayoutList(document, "list_settings_interface");
    assertLinearLayoutList(document, "list_settings_device");
    assertLinearLayoutList(document, "list_settings_other");

    assertFileDoesNotContain("layout/fragment_settings.xml", "style=\"@style/OpenJbdOutlinedCard\"");
    assertFileDoesNotContain("layout/fragment_settings.xml", "<com.google.android.material.card.MaterialCardView");
}
```

增加三个精确 helper：

```java
private static void assertTextViewExists(
        Document document,
        String expectedText) {
    for (Element textView : elements(document, "TextView")) {
        if (expectedText.equals(textView.getAttributeNS(ANDROID_NAMESPACE, "text"))) {
            return;
        }
    }
    throw new AssertionError("Missing TextView text: " + expectedText);
}

private static void assertLinearLayoutList(Document document, String id) {
    Element list = elementById(document, id);
    assertEquals("LinearLayout", list.getTagName());
    assertEquals("vertical", list.getAttributeNS(ANDROID_NAMESPACE, "orientation"));
    assertEquals("middle", list.getAttributeNS(ANDROID_NAMESPACE, "showDividers"));
    assertEquals("@drawable/divider_card_stroke", list.getAttributeNS(ANDROID_NAMESPACE, "divider"));
}

private static void assertSettingsRowSpacing(
        String relativePath,
        String expectedMinHeight,
        String expectedPaddingStart,
        String expectedPaddingTop,
        String expectedPaddingEnd,
        String expectedPaddingBottom) throws Exception {
    Element row = parse(relativePath).getDocumentElement();
    assertEquals(expectedMinHeight, row.getAttributeNS(ANDROID_NAMESPACE, "minHeight"));
    assertEquals(expectedPaddingStart, row.getAttributeNS(ANDROID_NAMESPACE, "paddingStart"));
    assertEquals(expectedPaddingTop, row.getAttributeNS(ANDROID_NAMESPACE, "paddingTop"));
    assertEquals(expectedPaddingEnd, row.getAttributeNS(ANDROID_NAMESPACE, "paddingEnd"));
    assertEquals(expectedPaddingBottom, row.getAttributeNS(ANDROID_NAMESPACE, "paddingBottom"));
}
```

- [ ] **Step 2: 编写设置项点击热区保护测试**

继续在 `Material2ResourceTest` 增加：

```java
@Test
public void settingsRowKeepsTouchTargetAndPaddingAfterGroupSimplification() throws Exception {
    assertSettingsRowSpacing(
            "layout/row_setting_item.xml",
            "@dimen/touch_target_min",
            "@dimen/space_16",
            "@dimen/space_10",
            "@dimen/space_16",
            "@dimen/space_10");
}
```

- [ ] **Step 3: 运行设置页资源测试并确认失败**

Run:

```powershell
$env:ANDROID_HOME='D:\Android\Sdk'
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'
.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.ui.Material2ResourceTest
```

Expected: FAIL，失败点为 `fragment_settings.xml` 仍包含 `OpenJbdOutlinedCard` 或 `MaterialCardView`。

- [ ] **Step 4: 提交失败测试**

```powershell
git add -- app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java
git commit -m "test(settings): 添加轻量分组资源约束"
```

### Task 2: 移除设置页外层卡片并保留分组列表

**Files:**
- Modify: `app/src/main/res/layout/fragment_settings.xml`
- Test: `app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java`

- [ ] **Step 1: 将三个列表容器提升为分组标题后的直接子节点**

把 `fragment_settings.xml` 改成下面的结构：

```xml
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:clipToPadding="false"
    android:fillViewport="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingStart="16dp"
        android:paddingTop="8dp"
        android:paddingEnd="16dp"
        android:paddingBottom="18dp">

        <TextView
            style="@style/OpenJbdSectionTitle"
            android:text="@string/settings_group_interface" />

        <LinearLayout
            android:id="@+id/list_settings_interface"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="6dp"
            android:divider="@drawable/divider_card_stroke"
            android:orientation="vertical"
            android:showDividers="middle" />

        <TextView
            style="@style/OpenJbdSectionTitle"
            android:text="@string/settings_group_device" />

        <LinearLayout
            android:id="@+id/list_settings_device"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="6dp"
            android:divider="@drawable/divider_card_stroke"
            android:orientation="vertical"
            android:showDividers="middle" />

        <TextView
            style="@style/OpenJbdSectionTitle"
            android:text="@string/settings_group_other" />

        <LinearLayout
            android:id="@+id/list_settings_other"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="8dp"
            android:divider="@drawable/divider_card_stroke"
            android:orientation="vertical"
            android:showDividers="middle" />
    </LinearLayout>
</ScrollView>
```

- [ ] **Step 2: 确认不修改共享样式和 Java 逻辑**

Run:

```powershell
git diff -- app/src/main/res/layout/fragment_settings.xml app/src/main/java/com/gytxtx/openjbd/SettingsFragment.java app/src/main/res/values/styles.xml
```

Expected: diff 只包含 `fragment_settings.xml`；`SettingsFragment.java` 和 `styles.xml` 无改动。

- [ ] **Step 3: 运行设置页资源测试**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.ui.Material2ResourceTest
```

Expected: PASS。

- [ ] **Step 4: 提交设置页布局改造**

```powershell
git add -- app/src/main/res/layout/fragment_settings.xml app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java
git commit -m "fix(settings): 改为轻量分组列表"
```

### Task 3: 完整验证设置页行为与视觉结果

**Files:**
- Modify: 仅在验证明确暴露问题时修改已有设置页相关文件
- Add: `docs/audits/2026-07-04-md2-interface/11-settings-lightweight-groups.png`

- [ ] **Step 1: 运行完整单元测试**

```powershell
$env:ANDROID_HOME='D:\Android\Sdk'
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'
.\gradlew.bat :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: 构建 Debug 包**

```powershell
.\gradlew.bat :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: 安装应用并打开设置页**

```powershell
.\gradlew.bat :app:installDebug
D:\Android\Sdk\platform-tools\adb.exe shell monkey -p com.gytxtx.openjbd -c android.intent.category.LAUNCHER 1
```

Expected: 应用成功启动，可以进入设置页。

- [ ] **Step 4: 人工验证三类设置项行为**

在模拟器设置页逐项检查：

```text
1. 点击 Theme、Language、Temperature unit、Refresh interval 时，仍弹出现有下拉菜单。
2. 点击 Auto connect 行时，仍切换现有 SwitchMaterial，副标题同步变化。
3. 点击 About 行时，仍进入 AboutActivity。
```

Expected: 三类行为均与改造前一致。

- [ ] **Step 5: 截图并检查视觉层级**

保存设置页截图到：

```text
docs/audits/2026-07-04-md2-interface/11-settings-lightweight-groups.png
```

人工检查以下结果：

```text
1. 三个分组没有外层描边卡片。
2. 分组关系由标题和垂直留白表达。
3. 组内仍保留 divider。
4. 行项目没有贴边或压缩，触摸区域视觉上未缩小。
```

- [ ] **Step 6: 提交验证材料**

```powershell
git add -- docs/audits/2026-07-04-md2-interface/11-settings-lightweight-groups.png
git commit -m "docs(settings): 记录轻量分组验证截图"
```
