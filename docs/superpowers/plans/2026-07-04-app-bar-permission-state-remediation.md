# App Bar And Permission State Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一 OpenJBD 的 MD2 56dp 单行 App Bar，补全导航按钮语义，并为 BLE 权限拒绝状态提供准确、可恢复的错误界面。

**Architecture:** 视觉约束继续由 Android XML 资源和 `Material2ResourceTest` 管理。`DeviceListActivity` 保留现有扫描流程，只增加权限错误占位状态、恢复按钮和一个纯 Java 权限恢复决策方法；打开系统设置后的恢复检查由 `waitingForAppSettings` 限定。

**Tech Stack:** Java、Android Views、Material Components 1.10.0、Android XML resources、JUnit 4、Android 14 模拟器

---

### Task 1: 建立 App Bar 与权限错误资源约束

**Files:**
- Modify: `app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java`
- Test: `app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java`

- [ ] **Step 1: 编写 App Bar 失败测试**

在 `Material2ResourceTest` 中增加：

```java
@Test
public void appBarsUseStandardHeightInsetsAndNavigationDescriptions() throws Exception {
    assertToolbar(
            "layout/activity_main.xml",
            "top_app_bar",
            "@dimen/top_app_bar_height",
            "@string/action_select_device");
    assertToolbar(
            "layout/activity_device_list.xml",
            "device_top_app_bar",
            "@dimen/top_app_bar_height",
            "@string/action_back");
    assertToolbar(
            "layout/activity_about.xml",
            "about_top_app_bar",
            "@dimen/top_app_bar_height",
            "@string/action_back");
    assertToolbar(
            "layout/activity_licenses.xml",
            "licenses_top_app_bar",
            "@dimen/top_app_bar_height",
            "@string/action_back");

    assertViewOmitsAndroidAttribute("layout/activity_main.xml", "top_app_bar", "paddingStart");
    assertViewOmitsAndroidAttribute("layout/activity_main.xml", "top_app_bar", "paddingEnd");
    assertViewOmitsAppAttribute("layout/activity_main.xml", "top_app_bar", "subtitle");
    assertViewOmitsAppAttribute("layout/activity_device_list.xml", "device_top_app_bar", "subtitle");
}
```

新增精确 helper：

```java
private static final String APP_NAMESPACE = "http://schemas.android.com/apk/res-auto";

private static void assertToolbar(
        String relativePath,
        String id,
        String expectedHeight,
        String expectedNavigationDescription) throws Exception {
    Element toolbar = elementById(parse(relativePath), id);
    assertEquals(expectedHeight, toolbar.getAttributeNS(ANDROID_NAMESPACE, "layout_height"));
    assertEquals(
            expectedNavigationDescription,
            toolbar.getAttributeNS(APP_NAMESPACE, "navigationContentDescription"));
}

private static void assertViewOmitsAndroidAttribute(
        String relativePath, String id, String attribute) throws Exception {
    Element view = elementById(parse(relativePath), id);
    assertEquals("", view.getAttributeNS(ANDROID_NAMESPACE, attribute));
}

private static void assertViewOmitsAppAttribute(
        String relativePath, String id, String attribute) throws Exception {
    Element view = elementById(parse(relativePath), id);
    assertEquals("", view.getAttributeNS(APP_NAMESPACE, attribute));
}

private static Element elementById(Document document, String id) {
    NodeList elements = document.getElementsByTagName("*");
    for (int index = 0; index < elements.getLength(); index++) {
        Element element = (Element) elements.item(index);
        if (("@+id/" + id).equals(element.getAttributeNS(ANDROID_NAMESPACE, "id"))) {
            return element;
        }
    }
    throw new AssertionError("Missing view: " + id);
}
```

- [ ] **Step 2: 编写权限错误资源失败测试**

增加：

```java
@Test
public void devicePermissionErrorDefinesLocalizedRecoveryAction() throws Exception {
    Element button = elementById(
            parse("layout/activity_device_list.xml"),
            "btn_device_permission_action");
    assertEquals("gone", button.getAttributeNS(ANDROID_NAMESPACE, "visibility"));
    assertEquals(
            "@style/Widget.MaterialComponents.Button.TextButton",
            button.getAttribute("style"));

    assertString("values/strings.xml", "action_select_device", "Select device");
    assertString("values/strings.xml", "action_grant_permission", "Grant permission");
    assertString("values/strings.xml", "action_open_app_settings", "Open app settings");
    assertString(
            "values/strings.xml",
            "device_placeholder_permission_denied_title",
            "Bluetooth permission required");
    assertString(
            "values/strings.xml",
            "device_placeholder_permission_denied_subtitle",
            "Allow Bluetooth permission to scan for nearby BMS devices.");

    assertString("values-zh-rCN/strings.xml", "action_select_device", "选择设备");
    assertString("values-zh-rCN/strings.xml", "action_grant_permission", "授予权限");
    assertString("values-zh-rCN/strings.xml", "action_open_app_settings", "打开应用设置");
    assertString(
            "values-zh-rCN/strings.xml",
            "device_placeholder_permission_denied_title",
            "需要蓝牙权限");
    assertString(
            "values-zh-rCN/strings.xml",
            "device_placeholder_permission_denied_subtitle",
            "授予蓝牙权限后才能扫描附近的 BMS 设备。");
}
```

新增：

```java
private static void assertString(
        String relativePath,
        String name,
        String expectedValue) throws Exception {
    Document document = parse(relativePath);
    for (Element string : elements(document, "string")) {
        if (name.equals(string.getAttribute("name"))) {
            assertEquals(
                    "Unexpected value for string " + name,
                    expectedValue,
                    string.getTextContent().trim());
            return;
        }
    }
    throw new AssertionError("Missing string " + name + " in " + relativePath);
}
```

- [ ] **Step 3: 运行资源测试并确认按预期失败**

Run:

```powershell
$env:ANDROID_HOME='D:\Android\Sdk'
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'
.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.ui.Material2ResourceTest
```

Expected: FAIL，首个失败原因是 `top_app_bar` 仍为 `72dp`，随后缺少
`btn_device_permission_action` 和新增字符串。

- [ ] **Step 4: 提交失败测试**

```powershell
git add -- app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java
git commit -m "test: 添加 App Bar 与权限错误资源约束"
```

### Task 2: 统一 56dp 单行 App Bar 与导航语义

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/layout/activity_device_list.xml`
- Modify: `app/src/main/res/layout/activity_about.xml`
- Modify: `app/src/main/res/layout/activity_licenses.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/main/java/com/gytxtx/openjbd/MainActivity.java`
- Modify: `app/src/main/java/com/gytxtx/openjbd/DeviceListActivity.java`
- Test: `app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java`

- [ ] **Step 1: 增加导航语义字符串**

在英文资源中增加：

```xml
<string name="action_select_device">Select device</string>
```

在简体中文资源中增加：

```xml
<string name="action_select_device">选择设备</string>
```

- [ ] **Step 2: 修改四个 Toolbar**

`activity_main.xml` 的 `top_app_bar` 改为：

```xml
<com.google.android.material.appbar.MaterialToolbar
    android:id="@+id/top_app_bar"
    style="@style/Widget.OpenJbd.Toolbar"
    android:layout_width="match_parent"
    android:layout_height="@dimen/top_app_bar_height"
    app:menu="@menu/main_toolbar_menu"
    app:navigationContentDescription="@string/action_select_device"
    app:navigationIcon="@drawable/ic_list_24"
    app:title="OpenJBD" />
```

`activity_device_list.xml` 的 `device_top_app_bar` 删除 padding 和 subtitle，
改为：

```xml
<com.google.android.material.appbar.MaterialToolbar
    android:id="@+id/device_top_app_bar"
    style="@style/Widget.OpenJbd.Toolbar"
    android:layout_width="match_parent"
    android:layout_height="@dimen/top_app_bar_height"
    app:menu="@menu/device_toolbar_menu"
    app:navigationContentDescription="@string/action_back"
    app:navigationIcon="@drawable/ic_arrow_back_24"
    app:title="@string/toolbar_devices_title" />
```

`activity_about.xml` 的 `about_top_app_bar` 改为：

```xml
<com.google.android.material.appbar.MaterialToolbar
    android:id="@+id/about_top_app_bar"
    style="@style/Widget.OpenJbd.Toolbar"
    android:layout_width="match_parent"
    android:layout_height="@dimen/top_app_bar_height"
    app:navigationContentDescription="@string/action_back"
    app:navigationIcon="@drawable/ic_arrow_back_24"
    app:title="@string/about_title" />
```

`activity_licenses.xml` 的 `licenses_top_app_bar` 改为：

```xml
<com.google.android.material.appbar.MaterialToolbar
    android:id="@+id/licenses_top_app_bar"
    style="@style/Widget.OpenJbd.Toolbar"
    android:layout_width="match_parent"
    android:layout_height="@dimen/top_app_bar_height"
    app:navigationContentDescription="@string/action_back"
    app:navigationIcon="@drawable/ic_arrow_back_24"
    app:title="@string/licenses_title" />
```

- [ ] **Step 3: 删除运行时副标题写入**

从 `MainActivity.updateToolbar()` 删除：

```java
toolbar.setSubtitle(
        shouldShowDeviceSubtitle()
                ? connectedDeviceName
                : getString(R.string.toolbar_subtitle_local));
```

删除不再有调用者的 `shouldShowDeviceSubtitle()`。

从 `DeviceListActivity.setStatus(String)` 删除：

```java
if (toolbar != null) {
    toolbar.setSubtitle(getString(R.string.toolbar_devices_subtitle));
}
```

- [ ] **Step 4: 运行资源测试**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.ui.Material2ResourceTest
```

Expected: App Bar 断言通过；测试仍因权限按钮和权限错误字符串缺失而失败。

- [ ] **Step 5: 提交 App Bar 修复**

```powershell
git add -- app/src/main/res/layout/activity_main.xml app/src/main/res/layout/activity_device_list.xml app/src/main/res/layout/activity_about.xml app/src/main/res/layout/activity_licenses.xml app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/main/java/com/gytxtx/openjbd/MainActivity.java app/src/main/java/com/gytxtx/openjbd/DeviceListActivity.java
git commit -m "fix: 统一 MD2 单行 App Bar"
```

### Task 3: 实现权限错误空状态与恢复决策

**Files:**
- Create: `app/src/test/java/com/gytxtx/openjbd/PermissionRecoveryTest.java`
- Modify: `app/src/main/res/layout/activity_device_list.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/main/java/com/gytxtx/openjbd/DeviceListActivity.java`
- Test: `app/src/test/java/com/gytxtx/openjbd/PermissionRecoveryTest.java`
- Test: `app/src/test/java/com/gytxtx/openjbd/ui/Material2ResourceTest.java`

- [ ] **Step 1: 编写权限恢复决策失败测试**

创建：

```java
package com.gytxtx.openjbd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PermissionRecoveryTest {
    @Test
    public void rationaleAvailableRequestsPermissionAgain() {
        assertFalse(DeviceListActivity.shouldOpenAppSettings(true));
    }

    @Test
    public void rationaleUnavailableOpensAppSettings() {
        assertTrue(DeviceListActivity.shouldOpenAppSettings(false));
    }
}
```

- [ ] **Step 2: 运行测试并确认缺少方法**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.PermissionRecoveryTest
```

Expected: FAIL，`DeviceListActivity.shouldOpenAppSettings(boolean)` 不存在。

- [ ] **Step 3: 实现纯 Java 决策方法**

在 `DeviceListActivity` 增加：

```java
static boolean shouldOpenAppSettings(boolean canShowAnyPermissionRationale) {
    return !canShowAnyPermissionRationale;
}
```

- [ ] **Step 4: 运行决策测试并确认通过**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.PermissionRecoveryTest
```

Expected: PASS。

- [ ] **Step 5: 增加权限错误资源**

在英文资源中增加：

```xml
<string name="action_grant_permission">Grant permission</string>
<string name="action_open_app_settings">Open app settings</string>
<string name="device_placeholder_permission_denied_title">Bluetooth permission required</string>
<string name="device_placeholder_permission_denied_subtitle">Allow Bluetooth permission to scan for nearby BMS devices.</string>
```

在简体中文资源中增加：

```xml
<string name="action_grant_permission">授予权限</string>
<string name="action_open_app_settings">打开应用设置</string>
<string name="device_placeholder_permission_denied_title">需要蓝牙权限</string>
<string name="device_placeholder_permission_denied_subtitle">授予蓝牙权限后才能扫描附近的 BMS 设备。</string>
```

在 `placeholder_devices` 的说明文字后增加：

```xml
<com.google.android.material.button.MaterialButton
    android:id="@+id/btn_device_permission_action"
    style="@style/Widget.MaterialComponents.Button.TextButton"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginTop="@dimen/space_16"
    android:minHeight="@dimen/touch_target_min"
    android:text="@string/action_grant_permission"
    android:visibility="gone" />
```

- [ ] **Step 6: 绑定权限错误状态**

在 `DeviceListActivity` 增加字段：

```java
private MaterialButton permissionActionButton;
private boolean waitingForAppSettings;
```

在 `onCreate()` 中精确绑定：

```java
permissionActionButton = findViewById(R.id.btn_device_permission_action);
permissionActionButton.setOnClickListener(view -> recoverBlePermission());
```

让现有 `showPlaceholder(String, String, int)` 结尾始终执行：

```java
permissionActionButton.setVisibility(View.GONE);
```

新增：

```java
private void showPermissionDeniedPlaceholder() {
    showPlaceholder(
            getString(R.string.device_placeholder_permission_denied_title),
            getString(R.string.device_placeholder_permission_denied_subtitle),
            R.drawable.ic_bluetooth_disabled_24);
    boolean canShowAnyRationale = canShowAnyPermissionRationale();
    boolean openSettings = shouldOpenAppSettings(canShowAnyRationale);
    permissionActionButton.setText(openSettings
            ? R.string.action_open_app_settings
            : R.string.action_grant_permission);
    permissionActionButton.setTag(Boolean.valueOf(openSettings));
    permissionActionButton.setVisibility(View.VISIBLE);
}

private boolean canShowAnyPermissionRationale() {
    for (String permission : requiredPermissions()) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
                && shouldShowRequestPermissionRationale(permission)) {
            return true;
        }
    }
    return false;
}

private void recoverBlePermission() {
    boolean openSettings = Boolean.TRUE.equals(permissionActionButton.getTag());
    if (openSettings) {
        waitingForAppSettings = true;
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
        return;
    }
    setStatus(getString(R.string.status_requesting_permissions));
    requestPermissions(requiredPermissions(), REQUEST_BLE_PERMISSIONS);
}
```

增加所需精确 import：

```java
import android.net.Uri;
import android.provider.Settings;
import com.google.android.material.button.MaterialButton;
```

- [ ] **Step 7: 修复权限结果与设置返回流程**

将权限拒绝分支改为：

```java
setRefreshing(false);
toast(getString(R.string.toast_ble_permission_denied));
setStatus(getString(R.string.status_ble_permission_denied));
showPermissionDeniedPlaceholder();
```

增加：

```java
@Override
protected void onResume() {
    super.onResume();
    if (!waitingForAppSettings) {
        return;
    }
    waitingForAppSettings = false;
    if (hasBlePermissions()) {
        startScanWithPermissions();
    } else {
        setStatus(getString(R.string.status_ble_permission_denied));
        showPermissionDeniedPlaceholder();
    }
}
```

- [ ] **Step 8: 运行相关测试**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.gytxtx.openjbd.PermissionRecoveryTest --tests com.gytxtx.openjbd.ui.Material2ResourceTest
```

Expected: PASS。

- [ ] **Step 9: 提交权限恢复实现**

```powershell
git add -- app/src/test/java/com/gytxtx/openjbd/PermissionRecoveryTest.java app/src/main/res/layout/activity_device_list.xml app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml app/src/main/java/com/gytxtx/openjbd/DeviceListActivity.java
git commit -m "fix: 修复 BLE 权限拒绝状态"
```

### Task 4: 完整验证与模拟器复核

**Files:**
- Modify: 仅修改验证明确发现仍有问题的文件
- Add: `docs/audits/2026-07-04-md2-interface/07-fixed-overview.png`
- Add: `docs/audits/2026-07-04-md2-interface/08-fixed-permission-denied.png`
- Add: `docs/audits/2026-07-04-md2-interface/09-fixed-about.png`

- [ ] **Step 1: 运行静态检查**

```powershell
git diff --check
rg -n '72dp|list_top_app_bar_height|app:subtitle=|android:paddingStart=|android:paddingEnd=' app/src/main/res/layout/activity_main.xml app/src/main/res/layout/activity_device_list.xml
```

Expected: `git diff --check` 无输出；`rg` 无匹配。

- [ ] **Step 2: 运行全部单元测试**

```powershell
$env:ANDROID_HOME='D:\Android\Sdk'
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'
.\gradlew.bat :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: 构建并运行 Lint**

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

Expected: `BUILD SUCCESSFUL`，无 Lint error。

- [ ] **Step 4: 安装并重置权限**

```powershell
.\gradlew.bat :app:installDebug
D:\Android\Sdk\platform-tools\adb.exe shell pm clear com.gytxtx.openjbd
D:\Android\Sdk\platform-tools\adb.exe shell monkey -p com.gytxtx.openjbd -c android.intent.category.LAUNCHER 1
```

Expected: 应用显示 56dp 单行 App Bar。

- [ ] **Step 5: 截图并检查修复状态**

使用 ADB 保存三张截图和对应 UI hierarchy：

- 总览：App Bar 高度为 154px，导航按钮 `content-desc="选择设备"`。
- 权限拒绝：页面显示“需要蓝牙权限”，不显示“正在扫描设备”，并显示恢复按钮。
- 关于：App Bar 高度为 154px，返回按钮 `content-desc="返回"`。

逐张使用图像查看工具检查截图不是空白、加载中、裁切或错误页面。

- [ ] **Step 6: 更新审计报告**

在 `docs/audits/2026-07-04-md2-interface/audit.md` 增加“修复验证”章节，
列出三张修复后截图、UI bounds 和仍未检查的真实 BMS 状态。

- [ ] **Step 7: 提交验证材料**

```powershell
git add -- docs/audits/2026-07-04-md2-interface
git commit -m "docs: 记录界面修复验证结果"
```
