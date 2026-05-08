# ant-js 脚本引擎 API 速查表

这份文档是给脚本开发时随手查用的，不展开讲原理，只保留最常用的信息：

1. 对象名
2. 方法签名
3. 返回值
4. 使用备注

详细执行链、完整示例和调试建议，见同目录下的《脚本引擎说明》。 

## 1. 运行时对象总览

| 对象 | 作用 | 常见用途 |
| --- | --- | --- |
| `global` | 通用控制、页面判断、触控、任务控制 | 睡眠、点击、切任务 |
| `app` | 应用启动与查询 | 启动目标 App、查包名 |
| `keys` | 系统按键 | 返回、Home、多任务 |
| `log` | 原生日志 | 记录业务步骤和异常 |
| `toast` | 原生提示 | 短提示、长提示 |
| `shell` | Shell 能力 | 文本命令、查询命令 |
| `display` | 屏幕信息 | 分辨率、dpi |
| `storages` | 本地持久化 | 标记完成状态、缓存数据 |
| `ocr` | 文字识别 | 识别截图内文字 |
| `image` | 截图与找图 | 截图、模板匹配、灰度匹配 |
| `require` | 远端公共脚本载入 | 复用脚本函数 |

## 2. `global`

### 2.1 控制与工具

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `global.log(message)` | `String` | `void` | 简单写日志到 Logcat |
| `global.sleep(millis)` | `Int` | `void` | 固定睡眠 |
| `global.sleep(min, max)` | `Int, Int` | `void` | 随机睡眠 |
| `global.random(min, max)` | `Int, Int` | `Int` | 生成随机整数 |

### 2.2 页面与前台状态

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `global.inPage(str)` | `String` | `Boolean` | 根据当前包名/Activity 相关信息做包含判断 |
| `global.getCurrentPackage()` | 无 | `String` | 获取前台包名 |
| `global.getCurrentActivity()` | 无 | `String` | 获取当前 Activity |
| `global.getCurrentActivityWithPackage()` | 无 | `String` | 返回带包名的完整 Activity |
| `global.getCurrentWindow(keyword)` | `String` | `String` | 执行窗口焦点查询，空字符串时走默认关键字 |

### 2.3 触控操作

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `global.click(x, y)` | `Int, Int` | `void` | 单击 |
| `global.longClick(x, y, duration)` | `Int, Int, Int` | `void` | 长按 |
| `global.press(x, y, duration)` | `Int, Int, Int` | `void` | 与 `longClick` 等价 |
| `global.swipe(x1, y1, x2, y2, duration)` | `Int, Int, Int, Int, Int` | `void` | 滑动 |

### 2.4 任务控制

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `global.start()` | 无 | `void` | 触发开始任务 |
| `global.stop()` | 无 | `void` | 触发停止任务 |
| `global.next()` | 无 | `void` | 切换到下一个任务 |
| `global.changeFloat(position)` | `String` | `void` | 调整悬浮窗位置 |

### 2.5 文件与应用管理

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `global.download(url, path)` | `String, String` | `String` | 下载文件到缓存目录，返回本地路径 |
| `global.installApk(path)` | `String` | `Boolean` | 安装指定 APK |
| `global.uninstallApk(packageName)` | `String` | `void` | 卸载指定包 |
| `global.restartApp()` | 无 | `Boolean` | 重启当前宿主 App |

### 2.6 截图辅助

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `global.cacheScreenshot()` | 无 | `void` | 预先缓存一张截图到内存 |
| `global.getScreenshotImage()` | 无 | `android.media.Image?` | 获取缓存截图对象，脚本里很少直接用 |

### 2.7 常用写法

```js
if (global.getCurrentPackage() !== 'com.example.demo') {
  app.launchPackage('com.example.demo');
  global.sleep(4000);
}

global.click(540, 1800);
global.sleep(800, 1200);
```

## 3. `app`

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `app.appName()` | 无 | `String` | 宿主 App 名称 |
| `app.versionCode()` | 无 | `Int` | 宿主版本号 code |
| `app.versionName()` | 无 | `String` | 宿主版本名 |
| `app.launchApp(appName)` | `String` | `Boolean` | 按应用名启动 |
| `app.launchPackage(packageName)` | `String` | `Boolean` | 按包名启动 |
| `app.launchPackage2(packageName)` | `String` | `Boolean` | 通过 `monkey` 启动 |
| `app.getPackageName(appName)` | `String` | `String` | 由应用名查包名 |
| `app.getAppName(packageName)` | `String` | `String` | 由包名查应用名 |
| `app.openAppSetting(packageName)` | `String` | `Boolean` | 打开应用详情设置页 |
| `app.getInstalledApps()` | 无 | `String` | 返回逗号分隔的包名列表 |

```js
const pkg = 'com.ss.android.ugc.aweme';
if (!app.launchPackage(pkg)) {
  log.addLog(`启动失败: ${pkg}`, true, 'error');
}
```

## 4. `keys`

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `keys.back()` | 无 | `void` | 返回键 |
| `keys.home()` | 无 | `void` | Home 键 |
| `keys.recents()` | 无 | `void` | 多任务键 |
| `keys.inputKey(keyCode)` | `Int` | `void` | 发送指定 keyCode |

```js
keys.back();
global.sleep(1000);
keys.home();
```

## 5. `log`

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `log.d(message)` | `Any` | `void` | 调试日志 |
| `log.e(message)` | `Any` | `void` | 错误日志 |
| `log.v(message)` | `Any` | `void` | 详细日志 |
| `log.addLog(message, showFloatToast, level)` | `String, Boolean, String?` | `void` | 写入项目日志体系 |

备注：

1. `level` 第三个参数可省略。
2. 业务脚本建议优先用 `log.addLog(...)`，便于前端页面和数据库一起看。

```js
log.addLog('准备进入任务主流程', true);
```

## 6. `toast`

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `toast.shortToast(text)` | `String` | `void` | 短提示 |
| `toast.langToast(text)` | `String` | `void` | 长提示 |

备注：

1. 当前代码里的方法名就是 `langToast`，不是 `longToast`。

## 7. `shell`

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `shell.cmd(args)` | `String` | `String` | Shell 命令，支持多行 |
| `shell.fastCmd(args)` | `String` | `String` | 更直接的执行方式 |
| `shell.sleepScreen()` | 无 | `Boolean` | 调用 `input keyevent 223` 熄屏 |

备注：

1. 适合文本型命令，比如 `dumpsys`、`pm`、`am`、`input`。
2. 不建议直接拿它跑 `screencap -p` 这种二进制输出命令。

```js
const focus = shell.fastCmd('dumpsys window | grep mCurrentFocus');
log.addLog(`当前焦点: ${focus}`, false);
```

## 8. `display`

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `display.getDisplayWidth()` | 无 | `Int` | 屏幕宽度 |
| `display.getDisplayHeight()` | 无 | `Int` | 屏幕高度 |
| `display.getDisplayDensityDpi()` | 无 | `Int` | 屏幕 dpi |

```js
log.addLog(
  `屏幕信息: ${display.getDisplayWidth()}x${display.getDisplayHeight()} dpi=${display.getDisplayDensityDpi()}`,
  false
);
```

## 9. `storages`

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `storages.put(key, value)` | `String, Any` | `void` | 自动按类型写入 |
| `storages.get(key)` | `String` | `Any?` | 读取通用值 |
| `storages.putString(key, value)` | `String, String` | `void` | 写字符串 |
| `storages.getString(key)` | `String` | `String?` | 读字符串 |
| `storages.getBoolean(key)` | `String` | `Boolean` | 读布尔 |
| `storages.getInt(key)` | `String` | `Int` | 读 Int |
| `storages.getFloat(key)` | `String` | `Float` | 读 Float |
| `storages.getLong(key)` | `String` | `Long` | 读 Long |
| `storages.containsKey(key)` | `String` | `Boolean` | 是否存在 |
| `storages.remove(key)` | `String` | `Boolean` | 删除指定 key |
| `storages.clearAll()` | 无 | `Boolean` | 清空所有存储 |

注意：

1. 当前 Kotlin 实现里，`getInt`、`getFloat`、`getLong`、`remove` 的条件判断看起来是反的。
2. 新脚本如果没有验证过，优先使用 `put/getString/getBoolean` 这一组更稳妥。

```js
if (!storages.getBoolean('daily_done')) {
  storages.put('daily_done', true);
}
```

## 10. `ocr`

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `ocr.recognizeText(path)` | `String` | `String` | 纯文本 OCR |
| `ocr.recognizeTextAsJson(path)` | `String` | `String` | 带 boundingBox 的 JSON 字符串 |

```js
const path = image.takeCapture('ocr_demo', '100,200,500,220');
const text = ocr.recognizeText(path);
log.addLog(`OCR: ${text}`, true);
```

## 11. `image`

### 11.1 截图模式与统计

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `image.getFindImageReturnMode()` | 无 | `String` | 返回当前主截图模式 |
| `image.getFindImageReturnModeLabel()` | 无 | `String` | 返回模式中文名 |
| `image.toggleFindImageReturnMode()` | 无 | `String` | 切换截图模式并返回新模式名 |
| `image.getFindImageCompareStatsText()` | 无 | `String` | 获取找图统计文本 |
| `image.resetFindImageCompareStats()` | 无 | `void` | 重置找图统计 |

当前常见模式值：

1. `shell`
2. `screenCapture`，界面显示为“底层截图”
3. `mediaProjection`，界面显示为“系统截图”

### 11.2 截图

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `image.takeCapture(id, area)` | `String, String` | `String` | 截图并保存，返回图片路径 |

参数说明：

1. `id` 是截图标识，通常落到 `cache/<id>.jpg`
2. `area` 格式为 `x,y,width,height`，空字符串表示不裁剪

```js
const shot = image.takeCapture('home_debug', '');
log.addLog(`截图路径: ${shot}`, false);
```

### 11.3 单点找图

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `image.findImage(id, template, threshold)` | `String, String, Double` | `Point?` | 彩色主找图入口 |
| `image.findImageG(id, template, threshold)` | `String, String, Double` | `Point?` | 灰度主找图入口 |

返回值说明：

1. 命中时返回类似 `{x, y}` 的点对象
2. 未命中返回 `null`

### 11.4 多点找图

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `image.findImages(id, template, threshold, maxMatches)` | `String, String, Double, Int` | `List<Point>?` | 彩色多点匹配 |
| `image.findImagesG(id, template, threshold, maxMatches)` | `String, String, Double, Int` | `List<Point>?` | 灰度多点匹配 |

### 11.5 颜色与角点

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `image.findColors(template, color, threshold)` | `String, String, Float` | `List<Point>?` | 查找颜色点 |
| `image.findImageCorners(id, template, threshold)` | `String, String, Double` | `String?` | 彩色四角，返回 JSON |
| `image.findImageCornersG(id, template, threshold)` | `String, String, Double` | `String?` | 灰度四角，返回 JSON |

### 11.6 `id` 参数的含义

`findImage*` 系列中的 `id` 不是单纯编号，它可能表示：

1. 直接图片路径
2. 缓存截图 id
3. 空字符串 `''`，表示走当前实时截图链路

常用记法：

1. 匹配当前屏幕时传 `''`
2. 匹配已保存局部图时传之前的 `id`

### 11.7 典型写法

```js
const point = image.findImageG('', 'task/login_btn.png', 0.86);
if (point) {
  global.click(parseInt(point.x), parseInt(point.y));
  global.sleep(1200);
} else {
  log.addLog('未找到登录按钮', true, 'warn');
}
```

```js
const points = image.findImagesG('', 'task/coin.png', 0.88, 5) || [];
for (let i = 0; i < points.length; i++) {
  global.click(parseInt(points[i].x), parseInt(points[i].y));
  global.sleep(500, 700);
}
```

```js
const cornersJson = image.findImageCornersG('', 'task/panel.png', 0.9);
if (cornersJson) {
  const corners = JSON.parse(cornersJson);
  log.addLog(`四角点: ${cornersJson}`, false);
}
```

## 12. `require`

| 方法 | 参数 | 返回值 | 说明 |
| --- | --- | --- | --- |
| `require(url)` | `String` | `Any?` | 拉取远端 JS 并立即执行 |

备注：

1. 这不是 Node.js 的模块系统。
2. 更接近远端公共脚本 include。

```js
require('http://example.com/common/base.js');
```

## 13. 常用组合模板

### 13.1 启动 App 并等待

```js
function ensureApp(pkg) {
  if (global.getCurrentPackage() !== pkg) {
    app.launchPackage(pkg);
    global.sleep(4000, 5000);
  }
}
```

### 13.2 找图点击

```js
function tapIfFound(template, threshold) {
  const point = image.findImageG('', template, threshold);
  if (!point) {
    return false;
  }
  global.click(parseInt(point.x), parseInt(point.y));
  global.sleep(1000, 1500);
  return true;
}
```

### 13.3 失败时保留截图

```js
function saveDebugShot(name) {
  const shot = image.takeCapture(name, '');
  log.addLog(`调试截图已保存: ${shot}`, true, 'warn');
}
```

## 14. 当前实现里的注意点

1. `toast` 的长提示方法名是 `langToast`。
2. `shell` 适合文本命令，不适合直接处理截图二进制流。
3. `image.findImage*` 的模板文件会做本地缓存。
4. `image.takeCapture`、`image.findImage*`、`image.findImages*` 都带同步控制，高频调用要注意节奏。
5. `storages.getInt/getFloat/getLong/remove` 当前实现疑似有逻辑反向问题，使用前先自测。

## 15. 推荐使用顺序

新人写脚本时，建议优先掌握这几组：

1. `global.sleep / click / next / getCurrentPackage`
2. `app.launchPackage`
3. `image.takeCapture / findImageG`
4. `log.addLog`
5. `storages.put / getBoolean / getString`

把这几组先用熟，基本就能覆盖大多数自动化场景。