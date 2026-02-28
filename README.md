# Daily Segmented App Timer

这是一个 Android 应用限时工具：按固定时间段（整点对齐）统计并限制目标 App 的可用时长，超时后触发拦截提醒。

## 功能简介
- 按 N 小时为周期进行限时（从整点开始，到整点结束）
- 为每个目标 App 配置时长上限（支持小数分钟输入）
- 悬浮倒计时 + 超时弹窗提醒
- 支持前台切换场景下的会话统计与恢复

## 项目说明
- 技术栈：Kotlin + Android + Room + Foreground Service
- 本项目用于工程实践与调试练习
- 代码由 **Claude Code** 与 **Codex** 协作完成，作为 AI 编程工作流练习样例

## 运行
```bash
./gradlew assembleDebug
```

## 备注
当前仓库默认仅跟踪核心源码与配置，不包含本地 SDK/JDK 等环境目录。
