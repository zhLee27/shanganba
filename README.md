# 上岸吧 · 安徽省考备考 App

公考自用安卓应用，包名 `com.shanganba.examcountdown`。只服务一个场景：**纸质刷题 + 每日推进**——卷子上做题，App 负责倒计时、计时、记录和复盘。

## 功能

- **倒计时**：报名开始、报名截止、笔试、面试多节点可增删改；首页主倒计时支持自动取最近节点或手动钉选；秒级刷新 + 备考进度条
- **每日任务与打卡**：任务模板（可增删改停用）、当天临时任务、完成度；连续与累计打卡、近四周打卡日历
- **刷题计时**：自建模块（含行测五模块与申论题型的预设）、自定义题量、正计时/倒计时、计时页屏幕常亮、「做完一题 ＋1」记录每题耗时、标记疑难题、倒计时到点发系统通知
- **刷题结算**：录入做了几题/对了几题，自动算正确率与平均每题秒数，记录进入模块正确率与水平分析
- **错题本**：拍照 / 选文件 / 直接手打三种录入；复盘页可改答案对比、选错因标签、写解题技巧总结、标记已掌握
- **知识框架**：自建树状结构，节点掌握度按关联错题自动计算
- **水平分析**：行测五模块雷达图、按安徽省考分值加权估分（行测 100 + 申论 100）、正确率趋势、最该补的三块排序
- **激励**：勋章墙（连续打卡、累计刷题、复盘数量等）、每日一句话（硬核/温柔/轻松三种语气）
- **其它**：桌面小组件、每日计划提醒与考前 7/3/1 天提醒、应用内检查更新、数据导出/导入 JSON、三套主题配色、内置圆体字体

## 技术栈

Kotlin + Jetpack Compose + Material 3；单 Activity、MVVM；数据用 `kotlinx.serialization` 存到 `filesDir/data.json`（临时文件 + 原子替换，失败自动回退）；桌面小组件用 Glance；无后端，数据只留在手机上。

- minSdk 26（Android 8.0+）/ targetSdk 35
- 内置字体：站酷快乐体（SIL OFL 1.1）

## 本地构建

需要 JDK 17 与 Android SDK（platform 35、build-tools 35.0.0）。

```powershell
.\tools\build-apk.ps1 -VersionName "1.2.2" -VersionCode 6 -Test
```

`-Test` 会先跑单元测试；产物输出到 `dist\`（APK + version.json）。

脚本会在 C 盘建两个只占几字节的目录联接，用英文路径编译（Gradle 的 fork 进程在中文路径下会读不到 worker jar），代码与产物仍然都在 D 盘的工程目录里。

## 重要：签名密钥

`tools/keystore/` 已加入 `.gitignore`，**不会**也不应该提交到仓库。

请自行备份整个 `tools/keystore` 目录。Android 只允许「同包名 + 同签名」的 APK 覆盖安装，这个文件丢了以后就无法覆盖升级，只能卸载重装（打卡和错题数据全部丢失）。

## 发布新版与应用内更新

```powershell
python tools\upload_release.py --repo zhLee27/shanganba --tag v1.2.2
```

token 从 `tools/upload-token.txt` 读取（该文件同样不纳入版本管理），也可以用 `--token` 临时传入。

App 内置的更新地址是：

```
https://github.com/zhLee27/shanganba/releases/latest/download/version.json
```

它永远指向最新一个 Release，所以发布新 Release 后，应用启动时会自动检测并弹窗，可选择「立即更新」或「稍后更新」（稍后则下次启动再提醒）。

## 目录结构

```
app/src/main/java/com/shanganba/examcountdown/
  data/     数据模型与本地存储
  notify/   提醒闹钟与通知
  update/   检查更新、下载校验、拉起安装
  util/     日期计算、崩溃日志
  widget/   桌面小组件
  ui/       全部界面（Compose）
tools/      打包脚本、上传脚本、图标预览
dist/       构建产物（不纳入版本管理）
```
