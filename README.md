# OpenJBD

OpenJBD 是一个面向**嘉佰达** / **JBD** / **Jiabaida** / **小象电动** BMS 的本地 Android BLE 监控应用。它的目标很简单：打开 App，连接电池，查看关键状态。

这个项目不是原厂 App 的完整替代品，也不打算覆盖参数写入、校准、OTA、账号绑定等维护功能。OpenJBD 更偏向一个日常查看工具：轻量、直接、尽量少权限、尽量不打扰。

## 为什么做这个

原版“小象电动”App 功能很完整，但也因此比较重。原 App 包含登录/注册、设备绑定/解绑、Web 协议页、二维码扫描、网络接口、OTA、保护参数、均衡设置、温度设置、电压校准、容量设置等大量页面和流程。

OpenJBD 的取舍不同：

- 不要求账号。
- 不依赖云端。
- 不上传电池数据。
- 不提供高风险参数写入入口。
- 使用 Android 原生 BLE API 直接读取 BMS 状态。
- 用更现代的 Material 风格界面承载常用信息。

## 当前功能

- 扫描并连接 JBD / 嘉佰达 BLE BMS。
- 显示实时总览：
  - SOC
  - 总电压
  - 电流
  - 功率
  - 剩余容量 / 学习容量
  - 循环次数
  - MOS 状态
  - 温度
  - 保护状态
  - 均衡状态
- 显示单体电压列表，以及最高、最低、压差、平均电压。
- 显示参数页：
  - 蓝牙名称
  - BLE 地址
  - BMS 版本
  - 生产日期
  - 标称容量
  - 剩余容量
  - 学习容量
  - 序列号
  - 条码
  - 电池型号
  - 制造商
  - BMS 型号
  - 扩展参数字段
- 横屏 Dashboard，大字号显示 SOC、电压、电流、功率，适合平板、车载屏或远距离查看。
- 启动时自动连接上一次设备。
- 连接意外断开后自动重连。
- 支持浅色、深色、跟随系统主题。
- 支持简体中文、英文、跟随系统语言。
- 支持 Android 13+ 每应用语言设置。
- 支持摄氏度 / 华氏度切换。
- 支持状态刷新频率切换。
- 开源许可证页面覆盖全部运行时依赖。

## 与原版 App 的不同

| 项目 | OpenJBD | 原版小象 / 嘉佰达 App |
| --- | --- | --- |
| 定位 | 本地 BLE 状态查看工具 | 完整厂商工具 |
| 账号 | 无账号 | 包含登录、注册、找回密码 |
| 云端 | 不依赖云端 | 包含网络接口和设备权限查询 |
| 数据上传 | 不上传电池数据 | 原 App 包含网络、账号、绑定相关模块 |
| 权限 | 只声明 BLE 扫描/连接及旧版扫描所需定位权限 | 声明网络、存储、相机、通知、安装包、前台服务等更多权限 |
| 参数写入 | 不提供用户可操作的参数写入、校准、阈值修改 | 包含保护参数、均衡、温度、电流、电压校准等设置页 |
| OTA | 不支持 | 包含 OTA 页面和固件资产 |
| 扫码 | 不支持 | 包含二维码扫描 |
| UI | Material Components，底部三页导航 | 页面多，维护功能入口多 |
| 适用场景 | 日常快速查看电池状态 | 设备配置、维护、售后、升级 |

说明：OpenJBD 为读取部分厂商扩展信息，会发送读取相关协议命令，包括进入/退出读取扩展信息所需的模式命令；但它不提供修改保护阈值、校准参数、开关功能或 OTA 的用户入口。

## 权限与隐私

OpenJBD 当前声明的权限：

- Android 12+：
  - `BLUETOOTH_SCAN`
  - `BLUETOOTH_CONNECT`
- Android 6-11：
  - `BLUETOOTH`
  - `BLUETOOTH_ADMIN`
  - `ACCESS_COARSE_LOCATION`
  - `ACCESS_FINE_LOCATION`

旧版 Android 的 BLE 扫描机制可能要求定位权限和定位服务开启。OpenJBD 只把这些权限用于扫描附近 BLE 设备，不读取、不保存、不上传位置。

当前 App 不声明 `INTERNET` 权限。

## 使用方法

1. 打开 OpenJBD。
2. 点击顶部栏左侧列表按钮。
3. 在设备列表中选择 BMS。
4. 回到“总览”页面查看实时状态。
5. 切换到底部“参数”页面查看只读参数。
6. 连接后可点击顶部栏右侧 Dashboard 图标进入横屏大字模式。
7. 点击顶部栏右侧断开按钮可主动断开连接。

开启“启动时自动连接”后，App 会记住最近一次手动选择的 BMS。再次打开 App 时会尝试连接该设备；连接后如果 BLE 意外断开，会从 5 秒开始自动重连，重试间隔最多增加到 30 秒。手动断开连接会停止本轮自动重连。

## 本地化

OpenJBD 支持：

- 简体中文
- English
- 跟随系统

Android 13 及以上系统也可以在系统设置的“应用语言”中单独指定 OpenJBD 的语言。App 内语言设置如果选择“跟随系统”，会尊重系统语言或系统的每应用语言选择；如果在 App 内手动指定中文或英文，则以 App 内设置为准。

## 构建

环境：

- Android Gradle Plugin 9.0.1
- Kotlin 2.3.21
- Gradle 9.1.0 (Kotlin DSL + version catalog)
- `compileSdk 36`
- `minSdk 23`
- `targetSdk 36`
- Hilt 2.60.1 (KSP)
- Jetpack Compose M2 (build dependency)
- Material Components 1.10.0

构建 debug APK：

```powershell
.\gradlew.bat assembleDebug
```

输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

安装到已连接设备：

```powershell
.\gradlew.bat installDebug
```

仓库也提供本地部署脚本：

```powershell
.\deploy-debug.ps1
```

GitHub Actions workflow 位于：

```text
.github/workflows/android-debug.yml
```

## 项目结构

```text
app/src/main/kotlin/com/gytxtx/openjbd/
  MainActivity.kt                 主界面与底部导航 (Kotlin)
  OverviewFragment.kt             实时总览 (Kotlin Fragment)
  ParametersFragment.kt           只读参数页 (Kotlin Fragment)
  SettingsFragment.kt             设置页 (Kotlin Fragment)
  DeviceListActivity.kt           BLE 设备扫描与选择 (Kotlin)
  DashboardActivity.kt            横屏大字 Dashboard (Kotlin)
  AboutActivity.kt                关于页面 (Kotlin)
  LicensesActivity.kt             开源许可证页 (Kotlin)
  BmsConnectionManager.kt         BLE 连接、通知、轮询与自动重连 (Kotlin)
  AppSettings.kt                  设置存储 (Kotlin)
  SystemBars.kt                   系统栏工具 (Kotlin)
  Ext.kt                          扩展函数 (Kotlin)
  OpenJbdApplication.kt           Application (Kotlin, Hilt)
  data/
    BmsRepository.kt              BMS 状态仓库 (StateFlow + Hilt)
  protocol/                       JBD 帧封装与解析 (Kotlin)
  ble/
    BleConstants.kt               BLE UUID 常量 (Kotlin)
```

## 架构

- **语言**: 全部 Kotlin
- **DI**: Hilt (`@Singleton`, `@HiltViewModel`, `@Inject constructor`)
- **状态管理**: `BmsRepository` 暴露 `StateFlow<BmsUiState>`，替换旧的 `BmsStateStore` singleton
- **连接状态**: 18 种状态，由 `ConnectionState` 枚举定义
- **UI**: View-based（Fragment + Activity + XML 布局），Material Components 主题
- **测试**: JUnit 4 + kotlinx-coroutines-test

## 当前限制

- 不支持写入 BMS 参数。
- 不支持 OTA。
- 不支持二维码扫描和设备绑定。
- 不支持账号、云端设备列表或远程查看。
- 不支持历史数据曲线和数据导出。
- BLE 协议兼容性主要围绕已测试的 JBD / 嘉佰达设备；其他兼容 BMS 可能需要调整 UUID、命令或解析逻辑。
- 自动重连依赖 Android BLE 栈回调；如果系统关闭蓝牙、权限被撤销或设备深度休眠，需要恢复环境后再连接。

## 参考

- 原版“小象电动”APK 与反编译参考工程。
- JBD BMS 通信协议资料：<https://shishir-dey.github.io/open_battery/>

## 协议

MIT
