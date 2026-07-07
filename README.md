# 舒尔特方格 · 专注力训练 APP

原生 Android 应用（Kotlin + Jetpack Compose），用于舒尔特方格专注力训练。

## 功能特性

- **五档难度可调**：3×3 入门、4×4 简单、5×5 标准、6×6 困难、7×7 大师
- **震动反馈**：点击正确短震、点击错误双脉冲、训练完成胜利节奏
- **实时计时**：精确到 0.01 秒
- **最佳成绩**：每个难度独立记录最佳时间（本地持久化）
- **视觉反馈**：已点击格变绿、错误格红闪、完成弹窗
- **沉浸式深色 UI**，移动端竖屏优化

## 玩法

按 1 → 2 → 3 → … → N 的顺序依次点击方格中的数字，越快越好。视线尽量注视方格中心，用余光寻找数字，可有效锻炼专注力与 peripheral vision（周边视野）。

## 技术栈

- Kotlin 1.9.24
- Jetpack Compose（Material 3，BOM 2024.06.00）
- Android Gradle Plugin 8.5.2 / Gradle 8.9
- minSdk 24 / targetSdk 34
- 单 Activity + StateFlow 驱动的 ViewModel 架构

## 项目结构

```
app/src/main/java/com/adrain/schultegrid/
├── MainActivity.kt          # 入口 Activity
├── SchulteApp.kt            # Compose UI 全部界面
├── SchulteViewModel.kt      # 游戏逻辑、计时、最佳成绩
├── VibrationHelper.kt       # 震动反馈封装
└── model.kt                 # 数据模型（Difficulty / GameState / CellState）
```

## 构建运行

1. 用 Android Studio（Hedgehog 或更高版本）打开本项目根目录。
2. 等待 Gradle Sync 完成（首次会下载依赖）。
3. 连接 Android 设备或模拟器，点击 Run。

命令行构建：

```bash
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 震动权限

已在 `AndroidManifest.xml` 声明 `VIBRATE` 权限，无需运行时申请。
