# 我用 Kotlin + Compose 写了个舒尔特方格 APP，把它做成了一款专注力训练工具

你有没有过这种感觉：看书没两页就开始走神，开会听着听着脑子就飘了，明明想专注却总也静不下来。

其实专注力是可以「练」的。而训练专注力最经典、也最被验证有效的工具之一，就是**舒尔特方格（Schulte Grid）**。

这篇文章记录我如何从零做一个舒尔特方格的 Android APP，以及在这个过程中逐步加上的难度调节、震动反馈、训练统计、动画打磨等能力。代码已经开源，文末有仓库地址。

---

## 一、舒尔特方格到底是什么

舒尔特方格是一张 N×N 的数字矩阵，里面打乱填入 1 到 N² 的数字。训练时，你要尽可能快地按顺序点击 1、2、3……直到最后一个数字。

它的核心训练价值在于：

- **视觉广度**：方格越大，需要的周边视野越广；
- **抗干扰**：大脑要在一片数字里持续锁定下一个目标，天然锻炼专注；
- **可量化**：用了多少秒、点错了几次，都是客观指标，方便追踪进步。

所以一个好用的舒尔特方格工具，不只要「能点」，还要能**调节难度、给即时反馈、记录成绩**。围绕这三点，我设计了整个 APP。

---

## 二、技术选型：为什么是原生 Android + Jetpack Compose

一开始我考虑过 Web 方案，但「点击震动反馈」是核心需求。Web 的 Vibration API 在 iOS 上基本不可用，而安卓原生 `Vibrator` 则稳定且细腻。加上用户明确要的是安卓 APP，于是选定：

- **Kotlin + Jetpack Compose（Material 3）**：声明式 UI，状态驱动，代码量小；
- **单 Activity + ViewModel + StateFlow**：状态集中管理，UI 纯展示；
- **minSdk 24 / targetSdk 34**，AGP 8.5.2，Gradle 8.9。

整体架构非常薄：

```
MainActivity  ──持有──>  SchulteViewModel（状态与逻辑）
                         │
                         └──>  StateFlow<TrainingUiState>  ──订阅──>  SchulteApp（纯 Compose UI）
```

所有游戏状态都收拢在一个不可变数据类 `TrainingUiState` 里，UI 只是它的「投影」。这种单向数据流，让暂停、倒计时、错误抖动这些状态都变得好测试、好推理。

---

## 三、核心功能拆解

### 1. 五档难度 + 两种模式

难度做成了 3×3 到 7×7 五档：入门、简单、标准、困难、大师。再叠加**正序（1→N）**和**倒序（N→1）**两种模式——倒序模式其实更难，因为大脑更习惯从小到大找数。

```kotlin
enum class Difficulty(val size: Int, val label: String, val desc: String) {
    EASY3(3, "3×3", "入门"),  NORMAL5(5, "5×5", "标准"),  MASTER7(7, "7×7", "大师")
    // ...
}
enum class Mode(val label: String) { ASC("正序"), DESC("倒序") }
```

### 2. 震动反馈

在 `AndroidManifest` 里声明 `VIBRATE` 权限后，用 `VibratorManager` 封装三层反馈：

- 点对：一次短震；
- 点错：双脉冲提示；
- 完成：胜利节奏。

并且把震动做成了**可调强度**（轻/中/强），通过缩放每段时长实现，同时尊重用户的开关设置。

### 3. 计时与暂停

计时用协程循环刷新，关键点是**支持暂停**：把已流逝时间累加到 `accumMs`，暂停时冻结，继续时再续上，这样成绩才精确。

```kotlin
private fun pauseTimer() {
    accumMs += System.currentTimeMillis() - runStartMs
    timerJob?.cancel()
    _uiState.update { it.copy(paused = true, elapsedMs = accumMs) }
}
```

### 4. 成绩统计与历史

每个「难度 + 模式」组合独立持久化：最佳时间、平均用时、最近 10 次成绩、错误点击次数。完成弹窗里会展示这些数据，并给出 1~3 星评级——评级按难度目标时长（比如 5×5 标准约 35 秒）和错误次数计算。

---

## 四、体验打磨：让「训练」有节奏感

功能齐全之后，我重点优化了体验，几个改动对「手感」提升最大：

- **开局倒计时**：点开始后先 3-2-1 预备，每次滴答带一次轻震，帮助进入专注状态；
- **格子动画**：点对时数字弹跳缩放，点错时左右抖动，反馈干脆；
- **进度条**：网格上方一条进度条实时显示完成度；
- **自适应字号**：两位数自动缩字号，7×7 也不会拥挤；
- **顶部渐变背景 + 卡片化圆角**，整体更克制耐看。

下面是格子动画的核心思路——用 `Animatable` 在状态切到「完成」时先放大再回弹：

```kotlin
val popScale = remember { Animatable(1f) }
LaunchedEffect(cell.status) {
    if (cell.status == CellStatus.DONE) {
        popScale.snapTo(1.18f)
        popScale.animateTo(1f, spring(stiffness = 600f, dampingRatio = 0.5f))
    }
}
```

---

## 五、开发小记

整个过程也踩了几个坑，顺手记一下：

- **Gradle Wrapper**：远程仓库已有一个初始提交，rebase 时 `.gitignore` 用了 `--ours` 实际保留了远端版本，导致误把 `.DS_Store` 提交了，后来补进忽略项并清理；
- **构建验证**：本地有 Android SDK（`android-34` + build-tools 34.0.0），`./gradlew assembleDebug` 顺利产出 APK，包名 `com.adrain.schultegrid`；
- **版本与命名**：APK 输出名从默认 `app-debug.apk` 改成了 `SchulteGrid-v1.1-debug.apk`，版本号升到 versionCode 2 / versionName 1.1。

最终代码、构建脚本、README 全部推送到 `master` 分支，仓库地址见文末。

---

## 六、还能怎么演进

如果要继续做，我会优先这几项：

- **Release 签名配置**，让 release 包能直接安装 / 上架；
- **更多变体**：奇数优先、随机跳跃等模式；
- **数据可视化**：把历史成绩画成折线，直观看到专注力曲线；
- **浅色主题**与系统动态取色（`dynamicColor`）。

---

如果你也在练专注力，或者想用 Compose 写一个自己的小工具，欢迎 clone 这个仓库玩一玩。

**仓库地址**：https://github.com/adra2n/Schulte-Grid

欢迎在评论区聊聊：你平时用什么方式训练专注力？
