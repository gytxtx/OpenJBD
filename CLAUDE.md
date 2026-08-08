# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```powershell
# Build debug APK
.\gradlew.bat assembleDebug

# Build and install on connected device
.\gradlew.bat installDebug

# Deploy debug build (offline mode)
.\deploy-debug.ps1

# Run all unit tests
.\gradlew.bat testDebugUnitTest
```

## Architecture Overview

This is a single-module Android app (Kotlin) that monitors JBD/Jiabaida BMS batteries over BLE. It targets `minSdk 23`, `compileSdk 36`, with AGP 9.0.1, Material Components 1.10.0, and Hilt for dependency injection (`@HiltAndroidApp OpenJbdApplication`, `@AndroidEntryPoint` activities/fragments).

### State Distribution

A single reactive state store replaces what would normally be ViewModels:

- **`BmsRepository`** — `@Singleton` (Hilt) holding a `MutableStateFlow<BmsUiState>`. `BmsUiState` is an immutable data class carrying connection state, device identity, `JbdBasicInfo`, `JbdCellVoltages`, `JbdDeviceInfo`, and status text. `BmsConnectionManager` is the only writer (`repository.update`); activities and fragments collect `repository.uiState` via `repeatOnLifecycle(STARTED)` + `collect` and render the latest state.
- All state mutations are serialized on the main thread: `BmsConnectionManager.updateState` performs the read-modify-write inline on the main looper and posts it there when called from a BLE binder thread (GATT callbacks run off-main because `connectGatt` is called without a Handler).
- Because `StateFlow` suppresses values equal to the current one, every `with*` mutation bumps `updatedAtMillis`; do not remove that field without replacing the re-emission mechanism (`refreshLocalizedStatus` relies on it).

Updates replace the entire immutable snapshot reference — never mutate, always replace.

### BLE Connection (BmsConnectionManager)

`BmsConnectionManager` is a `@Singleton` injected via `@Inject constructor(@ApplicationContext context, BmsRepository repository)` that owns the entire BLE lifecycle:

1. **Connection**: `connectGatt` → service discovery → characteristic lookup → enable notifications via CCCD descriptor → `READY` state.
2. **Polling**: Once ready, a recurring `pollRunnable` enqueues a fixed cycle of read/write commands (basic info, cell voltages, then extended device info on first cycle). Each command goes through a sequential queue (`ArrayDeque<CommandRequest>`), gated by a `writeInFlight` flag. Command timeout is 2200ms.
3. **Auto-reconnect**: On unexpected disconnect, schedules exponential backoff (5s base, 30s max) if auto-reconnect is enabled. Manual disconnect or cancel stops it.
4. **BLE UUIDs** are in `ble/BleConstants.kt` (service `0000ff00-...`, notify `0000ff01-...`, write `0000ff02-...`).

### JBD Protocol (`protocol/`)

The binary frame protocol has a fixed structure: `0xDD | CMD | STATUS | LEN | PAYLOAD | CHECKSUM(2) | 0x77`.

- **`JbdFrame.parse()`** validates boundaries, length, and checksum (`(~sum)+1` complement over `CMD+LEN+PAYLOAD`).
- **`JbdFrameAssembler`** buffers incoming BLE notification bytes and extracts complete frames, skipping malformed ones.
- **`JbdParser`** decodes frames into domain objects (`JbdBasicInfo`, `JbdCellVoltages`, `JbdDeviceInfo`). All multi-byte values are big-endian. Text uses GB2312 encoding.
- **`JbdCommands`** builds outgoing command frames. Read commands use mode `0xA5`; factory-mode open/close and extended-param reads use specific payloads. All domain value objects are immutable.

### UI Structure

| Component | Role |
|---|---|
| `MainActivity` | Host activity with Material 3 top bar + bottom nav. Manages connection lifecycle, auto-connect, and fragment switching with hide/show (not replace). |
| `OverviewFragment` | Real-time dashboard: SOC bar, voltage/current/power/capacity/cycles, MOS, protection, balance, temperatures as chips, cell voltage list with per-cell progress bars. |
| `ParametersFragment` | Read-only parameter list (26 rows): BLE name, address, BMS version, date, capacity, serial, barcode, ratings, etc. Rendered via `BaseAdapter`. |
| `SettingsFragment` | Theme, language, temperature unit, refresh interval (popup menus), auto-connect toggle (switch). Rows rendered via `BaseAdapter`. |
| `DeviceListActivity` | BLE scanner with service UUID filter, 8s scan window. Differentiates "display" devices from battery devices via scan record MAC pattern matching. |
| `DashboardActivity` | Landscape full-screen view with large SOC/voltage/current/power text. Subscribes to `BmsRepository.uiState`. |
| `AboutActivity` / `LicensesActivity` | Static info pages. |

Every activity overrides `attachBaseContext` to wrap the context with language and night-mode configuration from `AppSettings.preferredContext()`. Activities and fragments are `@AndroidEntryPoint` and inject `BmsRepository` / `BmsConnectionManager` via `@Inject lateinit`.

### Settings (`AppSettings`)

All preferences stored in `SharedPreferences` (`openjbd_settings`). Key settings: theme (system/light/dark), language (system/zh/en), temperature unit (C/F), refresh interval (1s/2s/5s/10s), auto-connect (boolean), last device address/name.

Legacy string values are auto-migrated to typed values (e.g., `refresh_interval_ms` string → long, `auto_connect` string → boolean).

### Key Patterns

- **Immutable data objects**: `JbdDeviceInfo` and `BmsUiState` use `with*` copy methods. Never mutate — always replace.
- **State collection**: collect `repository.uiState` in `onStart` via `lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { repository.uiState.collect { render(it) } } }` — collection stops automatically below STARTED, so no manual unregister is needed. Fragments still guard rendering with `::view.isInitialized` checks.
- **No internet permission**: The app explicitly avoids network access.
- **Factory mode**: Extended device info (serial, barcode, manufacturer, ratings, BMS model/address) requires entering factory mode (`CMD_FACTORY_MODE` with payload `0x56 0x78`), issuing reads, then closing factory mode (`CMD_CLOSE_FACTORY_MODE`). This happens only on the first poll cycle after connection.
