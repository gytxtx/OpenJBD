# OpenJBD MVP

OpenJBD MVP 是一个面向 JBD / 小象 BMS 的本地 Android BLE 监控应用。目标是做一个轻量、直接、无需账号的替代客户端：打开 App，连接电池，读取关键状态，不经过云端，也不把查看电池电压这种基础操作绑到登录流程上。

当前版本以只读监控为主，暂不提供写参数、校准、开关保护阈值等高风险操作。

## 功能

- BLE 扫描并连接 JBD BMS 设备。
- 显示电池总览：SOC、总电压、电流、功率、剩余/标称容量、循环次数。
- 显示 MOS 状态、温度信息、单体电压列表。
- 显示单体最高电压、最低电压、压差、平均电压。
- 新增“参数”页面，参考原 App 的电池信息页展示只读电池信息。
- 支持横屏 Dashboard：黑色背景、大字号显示 SOC、电压、电流、功率，适合平板或车载屏常亮查看。
- 支持简体中文和 English。
- 支持 App 主题切换：自动 / Light / Dark。
- 支持温度单位切换。
- 支持自动重连上一次设备。

## 参数页

参数页参考原版 App 的 `BluetoothInfoActivity` 设计。原 App 的电池信息页会显示蓝牙名称、序列号、条码、电池型号、制造商、BMS 版本、BMS 型号、生产日期、BMS 地址、额定充放电参数等。

MVP 当前稳定显示这些从基础帧或连接状态可读出的字段：

- 蓝牙名称
- BLE 地址
- BMS 版本
- 生产日期
- 标称容量
- 剩余容量
- SOC
- 循环次数
- 单体数量
- NTC 数量
- 充电 / 放电 MOS 状态
- 总电压、总电流、功率

序列号、条码、电池型号、制造商、BMS 型号等字段目前保留为“暂未读取”。这些字段需要进一步补充厂商扩展命令读取逻辑，暂未放进 MVP，以避免把写参数或工厂模式相关流程过早带进 App。

## 使用方法

1. 打开 App。
2. 点击顶部栏左侧的列表按钮。
3. 在设备列表中选择电池设备。
4. 回到总览页查看实时数据。
5. 底部导航可切换“总览 / 参数 / 设置”。
6. 连接后可点击顶部栏右侧 Dashboard 图标进入横屏大字模式。
7. 点击顶部栏右侧关闭图标可断开连接。

## 权限说明

App 需要蓝牙相关权限用于 BLE 扫描和连接：

- Android 12 及以上：`BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT`
- Android 6 到 Android 11：系统 BLE 扫描机制仍可能要求定位权限和定位服务开启

OpenJBD MVP 不使用账号登录，不上传电池数据，不依赖云端服务。定位权限只用于兼容 Android BLE 扫描限制，不用于获取或上传位置。

## 构建与安装

项目位于 `OpenJbdMvp` 目录。

```powershell
.\gradlew.bat assembleDebug
```

Debug APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

连接 Android 设备后可以直接安装：

```powershell
.\gradlew.bat installDebug
```

仓库中也提供了一个本地部署脚本：

```powershell
.\deploy-debug.ps1
```

这个脚本依赖本机已有的 Gradle wrapper runtime，并以 offline 方式执行 `deployDebug`。

## 与原版软件对比

| 项目 | OpenJBD MVP | 原版小象 / JBD App |
| --- | --- | --- |
| 基础查看 | 直接连接后查看 | 常见流程会先引导登录 |
| 账号依赖 | 无账号、无云端 | 包含账号、绑定、云端相关逻辑 |
| BLE 扫描 | 直接使用 Android BLE API | 部分设备上会强依赖 GPS / 隐私设置判断 |
| 数据范围 | 当前聚焦实时只读数据 | 功能很多，包含参数读取、写入、校准、OTA 等 |
| 写操作 | 暂不提供 | 提供大量设置和写参数入口 |
| UI | Material Components，三页结构 | 页面较多，逻辑入口复杂 |
| 隐私 | 本地读取，不上传 | 带用户协议、隐私协议、账号和网络相关模块 |
| 适用场景 | 快速、安全地查看电池状态 | 完整厂商工具，适合需要配置/维护时使用 |

## 技术栈

- Java
- Android Gradle Plugin 9.0.1
- Material Components 1.10.0
- `minSdk 23`
- `targetSdk 33`
- 包名：`com.gytxtx.openjbd`

## 当前限制

- 暂不支持写入 BMS 参数。
- 暂不支持 OTA、校准、保护阈值配置。
- 参数页的厂商扩展字段仍需补充读取命令。
- BLE 协议兼容性主要围绕已测试的 JBD BMS，其他兼容设备可能需要调整 UUID 或解析逻辑。

## 参考

- JBD BMS 通信协议资料：<https://shishir-dey.github.io/open_battery/>
- 原版 APK 反编译工程：用于理解页面结构和 BLE 交互逻辑。

## 协议

MIT
