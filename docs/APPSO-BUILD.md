# 构建 APPSO 定制版

环境：JDK 17、Android SDK Platform 36，以及仓库自带的 Gradle wrapper。安装 SDK 后设置 `ANDROID_HOME`，或在不提交的 `local.properties` 中设置 `sdk.dir`。

```bash
./gradlew :app:assembleStandardRelease
```

产物：`app/build/outputs/apk/standard/release/app-standard-release.apk`。

没有 `local/signing.properties` 时构建使用本机 debug 签名，仍是经过 R8 压缩的 Release 构建，但不能覆盖发布签名的 APK。发布者私钥不在仓库中；自行构建者应使用自己的密钥，不要将密钥或密码提交。

需要自己的签名时，在被 gitignore 排除的 `local/` 中放置 keystore，创建 `local/signing.properties`：

```properties
storeFile=your-release-key.jks
storePassword=YOUR_LOCAL_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_LOCAL_PASSWORD
```

以上仅为占位模板。发布构建的证书 SHA-256 指纹：
`4950dc702d0f4b25726a6a7445ff7c1c3ecb080eafe292df2c945e0de0f2fb2f`。

## 本次变更的自动测试

```bash
./gradlew :app:testStandardReleaseUnitTest \
  --tests app.orchard.tv.ui.home.ArticleConfirmTest \
  --tests app.orchard.tv.wallpaper.IfanrArticlesTest \
  --tests app.orchard.tv.wallpaper.IfanrRefreshTest \
  --tests app.orchard.tv.wallpaper.ArticleReaderTest \
  --tests app.orchard.tv.wallpaper.LegacyArticleCssTest \
  --tests app.orchard.tv.settings.ConfigBackupTest \
  --tests app.orchard.tv.update.LegacyUpdateTlsTest
```

## Android 9+ 安装时的 dex metadata

APK 中包含两种 profile 格式，Android 9–11 选 28.dm，Android 12+ 选 31.dm。先查询 API 级别，再提取一种；Android 5–8 跳过。

```bash
adb shell getprop ro.build.version.sdk
# 下例为 API 31+；API 28–30 将 31.dm 改成 28.dm。
unzip -p Aerial-1.96-APPSO.apk assets/aerial-dm/31.dm > Aerial-1.96-APPSO.dm
adb install-multiple -r Aerial-1.96-APPSO.apk Aerial-1.96-APPSO.dm
```

APK 和 .dm 必须来自同一次构建并使用同一个文件名前缀。
