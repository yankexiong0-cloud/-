# DeepSeek 余额桌面小组件

Android 原生桌面小组件，直接在主屏显示 DeepSeek API 余额，每 15 分钟自动刷新。

## 怎么拿到 APK（不用装 Android Studio）

1. 新建一个 GitHub 仓库（Private 也可以）
2. 把这个目录里的所有文件传上去
3. 打开仓库的 **Actions** 页，等 `build-apk` 跑完（约 3–5 分钟）
4. 点进这次运行，下载底部 **Artifacts** 里的 `dswidget-debug-apk`
5. 解压得到 `app-debug.apk`，传到手机安装

## 装好之后

1. 打开「DeepSeek 余额」App，填入你的 API Key，点「保存并刷新」
2. 回桌面，长按空白处 → **小部件 / 添加小工具** → 找到「DeepSeek 余额」
3. 拖到桌面，大小可调

## 说明

- Key 只存在 App 私有目录（SharedPreferences），不上传
- 刷新间隔 15 分钟（Android 的闹钟会被系统合并，实际可能略晚）
- 点数字区域 → 打开 App；点左上角「DEEPSEEK · 余额」那一行 → 立即刷新
- 余额接口：`GET https://api.deepseek.com/user/balance`

## 目录结构

```
app/src/main/java/com/dsha/dswidget/
  BalanceWidget.kt     小组件渲染与刷新
  BalanceApi.kt        调 DeepSeek 余额接口
  RefreshScheduler.kt  AlarmManager 定时
  RefreshReceiver.kt   闹钟/开机广播
  Prefs.kt             Key 存储
  MainActivity.kt      填 Key 的界面
```
