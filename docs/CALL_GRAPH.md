# Call Graph

This graph summarizes the main runtime call flow in the app.

```mermaid
flowchart TD
  BootReceiver -->|ACTION_BOOT_COMPLETED| MonitorService
  MainActivity -->|toggle start| MonitorService
  MainActivity -->|toggle stop| MonitorService
  MainActivity -->|save limits| AppLimitDao
  MainActivity -->|refresh| MonitorService

  MonitorService --> AppUsageHelper
  MonitorService --> AppLimitDao
  MonitorService --> UsageHistoryDao
  MonitorService -->|UPDATE_OVERLAY/HIDE_OVERLAY| OverlayService
  MonitorService -->|startActivity timeout| TimeoutDialog

  OverlayService --> CircularProgressView
  TimeoutDialog -->|extend +5m| AppLimitDao
  TimeoutDialog -->|refresh/dismiss| MonitorService

  AppListAdapter -->|read usage| UsageHistoryDao
  AppListAdapter -->|onLimitChanged| MainActivity

  AppDatabase --> AppLimitDao
  AppDatabase --> UsageHistoryDao
```

## Key Paths

- Start monitoring: `MainActivity -> MonitorService.start() -> checkCurrentApp()`
- Foreground check loop: `MonitorService -> AppUsageHelper.getCurrentForegroundApp()`
- Limit hit path: `MonitorService -> TimeoutDialog`
- Overlay path: `MonitorService -> OverlayService -> CircularProgressView.startCountdown()`
- Persist usage: `MonitorService.saveCurrentSession() -> UsageHistoryDao.insertHistory()`
- Update limit after timeout: `TimeoutDialog.extendTime() -> AppLimitDao.updateLimit() -> MonitorService.refresh()`
