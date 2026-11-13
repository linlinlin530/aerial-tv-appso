# 许可证与来源

## Aerial 应用代码

原项目：https://github.com/pengchujin/aerial-tv  
基线提交：`030a77fbc99e8179bac571fc86179a8b45557838`（Aerial 1.96）。

原版权声明 `Copyright (c) 2026 pengchujin` 及 MIT 条款完整保留于根目录 `LICENSE`。本分支新编写的应用代码也按 MIT 提供；该说明不重新许可第三方组件。

## 点阵字体

- 项目：https://github.com/wixette/dotted-chinese-fonts
- 固定提交：`ae60462b56d670a84f96852ab6aaef4dad7c43d5`
- 使用文件：上游 0.1 发布包中的 `DottedSongtiCircleRegular.otf`，内容未修改，只在 APK 中重命名为 `aerial_dots.otf`。
- 字体项目声明 GPL-2.0；转换工具的 package.json 另声明 GPL-2.0-or-later，以各自原始声明为准。
- 字体来源于文泉驿点阵宋体，不是 Nothing 字体。保留上游来源说明。

`third_party/dotted-chinese-fonts/` 提供未经修改的上游字体发布包（含可编辑 OTF）、转换源码、锁文件、README 和 GPL 正文。其 `UPSTREAM.txt` 记录来源；本项目不宣称可以仅靠转换脚本逐字节重建经过上游 FontForge 清理的字体。APK 内同时保留字体许可证和来源说明。

这些独立字体资源保留自己的许可；根目录 MIT 不覆盖它们。不对整个分发包作单一 MIT 授权声明。

## AndroidX 与其他依赖

依赖及固定版本见 `app/build.gradle.kts` 和 Gradle 解析配置。打包时将内容相同的四份 AndroidX Apache 2.0 许可合并为 `assets/licenses/AndroidX-APACHE-2.0.txt`；原始路径记录在同目录 NOTICE.txt。其他随包许可继续保留。

上游 Inter 字体已不再打入本 APK；仓库保留上游的 FONT-LICENSE-OFL.txt 及历史记录。

## 文章、图片与品牌

ifanr 内容版权归对应权利人，不由本项目软件许可证授予使用权。应用通过网站 RSS 获取内容并链接原文，不随发布包分发文章缓存。

Aerial、APPSO、Nothing 等名称不因软件许可而获得商标授权。本项目不暗示上游作者或 Nothing 的认可；APPSO 为本定制分支展示名称。
