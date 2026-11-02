# 余额小组件（DeepSeek / 千问 / 智谱）

Android 原生桌面小组件，一个 App 里三个可拖到主屏的小组件：

| 小组件 | 数据来源 | 显示 |
|---|---|---|
| `DEEPSEEK · 余额` | `api.deepseek.com/user/balance`（API Key） | 真实余额 + 计费时段 |
| `QWEN · 余额` | 阿里云费用中心 `QueryAccountBalance`（AccessKey） | 真实可用余额 / 未配置时是入口卡片 |
| `智谱 · 余额` | 官方没有公开余额接口 | 纯入口卡片，点开财务页 |

刷新间隔 15 分钟（Android 的闹钟会被系统合并，实际可能略晚）。

## 怎么拿到 APK（不用装 Android Studio）

1. 把这个目录推到 GitHub 仓库
2. 打开仓库的 **Actions** 页，等 `build-apk` 跑完（约 3–5 分钟）
3. 点进这次运行，下载底部 **Artifacts** 里的 `dswidget-debug-apk`
4. 解压得到 `app-debug.apk`，传到手机安装

## 装好之后

1. 打开「余额小组件」App，填凭据，点保存
2. 回桌面，长按空白处 → **小部件 / 添加小工具** → 找到对应的小组件
3. 拖到桌面，大小可调

## 千问（阿里云百炼）余额说明

千问的 `sk-` API Key **只能调模型，不能查余额**（我试过 10 个候选路径，全部 404）。
百炼的费用是从阿里云账户里扣的，所以走费用中心的开放接口：

```
GET https://business.aliyuncs.com/
  Action=QueryAccountBalance & Version=2017-12-14 & Format=JSON
  + AccessKeyId / SignatureNonce / Timestamp / Signature(HMAC-SHA1)
```

需要一对**阿里云 AccessKey**：

1. 打开 <https://ram.console.aliyun.com/manage/ak> 建一个 AccessKey
2. 建议新建 RAM 子账号，只授 `bss:DescribeAccount`（费用中心只读）权限
3. 把 `AccessKey ID` 和 `AccessKey Secret` 填进 App 的「千问」一栏

不填也能用：小组件会变成「点开查看」的入口卡片，点一下直达百炼控制台。
配了之后：显示 `¥可用余额`，明细行是「现金 x · 信控 y」，点数字跳费用中心。

## 说明

- 凭据只存在 App 私有目录（SharedPreferences），不上传
- 点左上角标题行 → 立即刷新；点数字区域 → 打开 App 或对应控制台
- 签名算法按阿里云 RPC 规范实现，已用官方文档示例向量校验通过

## 目录结构

```
app/src/main/java/com/dsha/dswidget/
  BalanceWidget.kt      DeepSeek 小组件
  BalanceApi.kt         DeepSeek 余额接口
  Billing.kt            DeepSeek 谷价时段计算
  QwenWidget.kt         千问小组件 + 入口卡片
  QwenApi.kt            阿里云 QueryAccountBalance
  AliyunSign.kt         阿里云 RPC 签名（HMAC-SHA1）
  ZhipuWidget.kt        智谱入口卡片
  RefreshScheduler.kt   AlarmManager 定时
  RefreshReceiver.kt    闹钟/开机广播
  Prefs.kt              凭据存储
  MainActivity.kt       填凭据的界面
```
