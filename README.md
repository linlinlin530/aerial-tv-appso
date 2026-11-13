# Aerial APPSO Edition

基于 [pengchujin/aerial-tv](https://github.com/pengchujin/aerial-tv) 的非官方定制分支。保留 Aerial 的电视桌面体验，增加点阵字体、配置备份和 ifanr 文章屏保。

[下载安装包](https://github.com/linlinlin530/aerial-tv-appso/releases/latest) · [安装与操作指南](docs/APPSO-GUIDE.txt) · [构建说明](docs/APPSO-BUILD.md) · [许可证与来源](THIRD_PARTY_NOTICES.md)

## 和原版有什么不同？

| 功能 | 本分支的改动 |
| --- | --- |
| 点阵字体 | 桌面中文、英文和数字采用 Nothing 风格的圆点字形；非 Nothing 官方字体。 |
| 配置导入导出 | 手机/电脑管理页“更多 → 配置备份”导出 JSON，恢复应用排序、图标库选择、布局等；导入前自动保存回滚副本。 |
| ifanr 屏保 | RSS 获取最新文章封面和标题，约 15 秒切换，左右键切篇，支持已有缓存。 |
| 后台更新 | 选中 ifanr 屏保时注册每 2 小时的后台任务，切换其他屏保时取消；执行时刻由系统调度。 |
| 原文浏览 | 3 秒内两次确认打开对应原文，使用系统 WebView 和桌面 UA，无需第三方浏览器；上下滚动、返回退出。 |
| 版本标记 | 控制中心显示 `Aerial 1.96(APPSO)`。 |

以上以本分支基于的 Aerial 1.96 为比较对象。配置备份不包含上传的媒体文件、已安装应用或系统权限。

## 安装

适用于 Android 5.0+，本次提供 **standard Release** 包。需要 guard 变体的固件不在本次发布范围内。

1. 更新前导出配置；下载 Release 中的 APK 并核对 SHA-256。
2. 开启电视 ADB，连接并完成电视上的授权。
3. Android 5–8 可运行 `adb install -r Aerial-1.96-APPSO.apk`，必须看到 `Success`。
4. Android 9+ 的 dex metadata 安装方式及首次接管桌面步骤见操作指南与构建说明。

**不要先卸载或清除数据。** 本分支使用独立签名，不能直接覆盖官方版或其他签名版本；同签名版本可以覆盖更新。下载官方安装脚本会获取官方 APK，不会安装本定制版。

## 验证范围

维护者反馈已在小米 MiTV4-ANSM0 / Android 6.0.1 上完成现有功能实测。发布的 142 包在此基础上精简资源并重新构建，通过 29 项自动测试、Release 构建及 APK 签名检查。此次精简包没有另行完成一轮独立真机回归。

APK 约 3.49 MiB；删除无引用字体及冗余元数据，保留功能资源和许可证。系统 WebView 的版本、网站改版及不同电视固件可能影响网页交互；未承诺全部机型兼容。

## 来源与反馈

- 原项目：[pengchujin/aerial-tv](https://github.com/pengchujin/aerial-tv)，应用原始代码采用 MIT 许可，保留原作者版权声明。
- 字体：[wixette/dotted-chinese-fonts](https://github.com/wixette/dotted-chinese-fonts)，单独采用 GPL-2.0；原始字体发布包、转换源码和许可收录于 `third_party/dotted-chinese-fonts/`。
- 文章与图片来自 ifanr.com；内容版权归各权利人，不适用本项目的软件许可证。

本分支由独立维护者维护，不代表 Aerial 原作者或 Nothing 的官方版本。定制版问题请提交到[本仓库 Issues](https://github.com/linlinlin530/aerial-tv-appso/issues)，不要将定制功能的问题直接提交给上游。

原项目文档保留在 [README.upstream.md](README.upstream.md)。各组件的许可证分别适用，不将整个 APK 笼统声明为 MIT。
