# JS 引擎通过 shell 截图卡死分析

## 结论

当前仓库里，JS 引擎如果走的是 `shell.cmd(...)` / `shell.fastCmd(...)` 去执行 `screencap -p` 这一类 shell 截图命令，出现“卡死”是符合代码现状的。

但结合你补充的现场信息，这个问题现在更适合拆成两层来看：

1. 如果脚本直接通过 shell 执行 `screencap -p`，这条链路本身就存在二进制输出导致的阻塞风险。
2. 如果截图拿来和服务器图片做比对，而且问题主要出现在“清理缓存之后”，那么还有一条非常像“卡死”的冷启动阻塞链路：模板图重新下载、写入缓存、再做 OpenCV 匹配，这些动作都是同步执行的。

所以更准确的结论是：

- shell 截图链路本身有结构性风险；
- 而“清缓存后更容易出现”的现象，说明除了 shell 问题之外，模板缓存失效后的下载和匹配链路也很可能是主要诱因之一。

## 先说一个关键事实

仓库内和截图相关的注释有不少地方写着“现在使用 shell 命令截图”，但从实际调用点看，当前主链路并不是这样：

1. `TaskService` 在 Rhino 线程里注册了两个能力：`shell` 和 `image`。
2. `image.takeCapture(...)` 的实现位于 `app/src/main/java/com/android/ant/rhino/api/ImageApi.kt`。
3. `ImageApi.takeCapture(...)` 现在实际调用的是 `AppProcess.getAshmemData("")`。
4. 悬浮窗手动截图 `FloatService.takeTempCapture()` 也调用的是 `AppProcess.getAshmemData("")`。
5. `LaunchActivity` 启动时还会先调用一次 `AppProcess.getAshmemData(packageCodePath)`，明显是在做截图模块预热。
6. `captureViaShellCommand()` 虽然还保留在 `app/src/main/java/com/android/ant/utils/Utils.kt`，但当前仓库中没有直接调用点。

也就是说：

- 当前项目内置的截图 API 主路径已经不是 shell。
- 如果你现在看到“JS 引擎里用 shell 截图会卡死”，大概率是脚本直接调用了 `shell.cmd("screencap -p ...")` 或 `shell.fastCmd("screencap -p ...")` 这类能力，而不是调用 `image.takeCapture(...)`。

## 卡死的真正原因

### 1. `execCmd(...)` 先 `waitFor()`，再读输出，遇到大输出很容易死锁

`app/src/main/java/com/android/ant/utils/Utils.kt` 里的 `execCmd(...)` 逻辑大致是：

1. `Runtime.getRuntime().exec("su")`
2. 往 shell 写入命令
3. 写入 `exit`
4. 调用 `process.waitFor()` 等子进程结束
5. 子进程退出后，才开始读 `process.inputStream`

这套顺序对 `input tap`、`dumpsys window` 这种文本、小输出命令通常没问题；但对 `screencap -p` 这种会向标准输出持续写 PNG 二进制数据的命令就很危险。

典型阻塞过程如下：

1. JS 线程调用 `shell.fastCmd("screencap -p")`。
2. `execCmd(...)` 启动 `su` 子进程，并让它执行 `screencap -p`。
3. `screencap -p` 开始往 stdout 写大量 PNG 数据。
4. 父进程这时还没有读取 stdout，而是在 `waitFor()`。
5. 当 stdout 管道缓冲区写满后，子进程会阻塞在写操作上。
6. 子进程阻塞后就无法正常结束，父进程又在等它结束。
7. 结果就是双方互等，表现出来就是“卡死”。

这就是最核心的根因。

### 2. `execCmd(...)` 把输出按 UTF-8 文本逐行读取，不适合 PNG 二进制

`execCmd(...)` 后续使用的是：

- `BufferedReader(InputStreamReader(process.inputStream, "UTF-8"))`
- `readLine()`

但 `screencap -p` 输出的是 PNG 二进制流，不是 UTF-8 文本，也不是按行组织的数据。即使没有死锁，这种读取方式也会有两个问题：

1. 二进制数据会被当成文本错误解码。
2. `readLine()` 的语义和 PNG 数据格式不匹配，拿到的内容不可用于还原图片。

所以这条 API 从设计上就不适合拿来承载截图原始数据。

### 3. JS 引擎线程是同步执行的，一旦 shell 阻塞，脚本就整体卡住

`TaskService` 中的 Rhino 执行是在单独的 `js-thread` 里串行跑的。`shell.fastCmd(...)` 最终同步调用 `execCmd(...)`；`shell.cmd(...)` 虽然包了一层协程，但本质仍然是 `runBlocking + await()`，不会真正把这类阻塞 I/O 从调用方解耦掉。

因此一旦 `screencap -p` 卡在 shell 进程，整个 JS 任务线程都会一起挂住，表现就是：

- 当前脚本不再继续执行
- 停止任务响应变慢或看起来无效
- 上层看起来像“JS 引擎卡死”

### 4. 这不是“adb”在卡，而是应用内 shell 执行模型在卡

从仓库代码看，应用内部并没有真的调用 PC 端 `adb shell`，而是调用：

- `su`
- 然后在设备本地 shell 里执行命令

所以你感受到的是“adb shell 风格的截图命令卡死”，但从代码层面，它实际是应用内 `su/sh + screencap -p` 这条链路发生了阻塞。

## 为什么“底层截图”没问题

当前项目里所谓“底层截图”，实际对应的是 `AppProcess.getAshmemData(...)` 这一条路径。结合现有代码，可以推断它至少具备下面这些特点：

1. 直接返回 `Bitmap`，不经过 shell stdout。
2. 不依赖把 PNG 二进制当文本去读。
3. `LaunchActivity` 启动时会先调用一次，说明项目有意对截图模块做预热。
4. 项目里还专门保留了 `IsolatedProcessService`，并且配置了 `android:isolatedProcess="true"`，说明截图模块/初始化过程可能和进程隔离、共享内存或模块加载有关。

无论 AAR 内部具体实现是什么，这条路径至少避开了“shell 管道阻塞 + 文本解码二进制”这两个问题，所以表现稳定是合理的。

## 清理缓存后为什么更容易出现

你补充的这个信息和仓库代码是能对上的，而且它会明显改变问题重心。

### 1. 截图文件和模板文件都落在 `cacheDir`

当前实现里：

1. `ImageApi.takeCapture(...)` 会把截图保存到 `mApp.cacheDir` 下。
2. `ImageApi.findImage(...)` / `findImages(...)` / `findImageCorners(...)` 也会把服务器模板图缓存到同一个 `mApp.cacheDir` 下。

这意味着一旦系统清理了应用缓存，丢掉的不只是旧截图，也包括所有已缓存的模板图。

### 2. 缓存一旦被清空，首次比图会走“同步下载 + 写盘 + 解码 + 匹配”冷启动路径

模板文件不存在时，`ImageApi.findImage*` 不会直接失败，而是会：

1. 现场拼出服务器图片 URL。
2. 调用 `ImageHelper.download(...)` 同步下载模板图。
3. 把下载结果写回 `cacheDir`。
4. 然后再调用 OpenCV 的 `Imgcodecs.imread(...)` / `matchTemplate(...)` 去做匹配。

其中 `ImageHelper.download(...)` 本身就是阻塞式实现：

1. OkHttp 分支设置了 `connectTimeout(60s)`、`readTimeout(60s)`、`writeTimeout(60s)`。
2. 另一条分支甚至会直接 `curl` 下载。
3. `body.bytes()` 会先把整个响应体一次性读进内存，再写入文件。

所以缓存被清掉以后，第一次比图本身就可能变成一个很长的同步操作。

### 3. `ImageApi` 的截图和比图入口都加了 `@Synchronized`

这是一个很关键的放大器。

`ImageApi` 里的这些方法都使用了同步锁：

1. `takeCapture(...)`
2. `findImage(...)`
3. `findImages(...)`
4. `findImageCorners(...)`
5. 其它灰度/多图版本也是同样模式

这意味着：

1. 只要其中一个方法正在执行下载、解码或匹配，其他图像相关调用都要排队等待。
2. 缓存清空后的首次模板下载如果很慢，后续截图和找图动作就会一起表现成“卡住了”。
3. 在 JS 任务线程只有一条的前提下，这种阻塞会被直接感知成“JS 引擎截图卡死”。

### 4. 这更像“冷缓存阻塞”，不一定是真死锁

如果问题主要发生在清缓存之后，那么现场现象未必都是前面说的 shell 管道死锁。

还有一种很可能的情况是：

1. 截图已经成功。
2. 但下一步要比对的模板图因为缓存丢失，被迫重新下载。
3. 下载、写盘、OpenCV 解码和模板匹配都在同一条同步链路里执行。
4. 上层脚本看起来就像“截图这一步卡死了”。

也就是说，用户侧感受到的是“截图卡住”，但代码层面的真实阻塞点，可能已经进入了模板下载或模板匹配阶段。

### 5. 清缓存后还会放大内存压力

你提到“服务器上的图片加载到内存做比对”，这和代码现状也一致：

1. 截图先由 `AppProcess.getAshmemData(...)` 产出 `Bitmap`。
2. 模板下载时，`body.bytes()` 会把整张图先放进 JVM 堆内存。
3. 随后 OpenCV 又会把截图和模板分别读成 `Mat`。

因此在冷缓存阶段，同一时刻可能同时存在：

1. 截图 `Bitmap`
2. 模板字节数组
3. 模板文件
4. OpenCV 的源图/模板 `Mat`

如果模板数量多、图片大或者连续调用频繁，这会放大卡顿概率。它不一定直接导致死锁，但会让“清缓存后第一次运行明显更卡”这件事变得更容易出现。

## 仓库里还存在的混淆点

### 1. 注释和真实实现不一致

以下位置的注释都在强调“现在使用 shell 命令截图”：

- `MainActivity.kt`
- `FloatService.kt`
- `AndroidManifest.xml`

但真实运行代码里：

- `ImageApi.takeCapture(...)` 走的是 `AppProcess.getAshmemData("")`
- `FloatService.takeTempCapture()` 走的也是 `AppProcess.getAshmemData("")`
- `captureViaShellCommand()` 没有调用点

这会让后续排查非常容易误判。

### 2. 仓库中还保留了一个“看起来能用”的 shell 截图函数，但它不是当前主路径

`captureViaShellCommand()` 比 `execCmd(...)` 更接近正确方向，因为它至少是按字节流读取 `process.inputStream`，不是按文本行读取。

但它当前也不是 JS 引擎的标准截图入口，而且它仍然有几个风险：

1. 没有并发处理 `errorStream`。
2. 没有超时控制。
3. 仍是同步阻塞调用。
4. 发生权限或 shell 交互异常时，诊断信息有限。

所以它只能说明“方向更对”，不能说明“当前 JS shell 截图链路是安全的”。

## 建议

### 建议 1

JS 引擎不要再通过通用 `shell.cmd(...)` / `shell.fastCmd(...)` 执行 `screencap -p` 获取截图。

这是最重要的一条。`shell` API 应该继续只用于文本型命令，例如：

- `input tap`
- `input swipe`
- `dumpsys`
- `pm` / `am`

而截图应走单独 API。

### 建议 2

如果项目当前 `AppProcess.getAshmemData(...)` 已经稳定，就统一让 JS 侧走 `image.takeCapture(...)`，不要让业务脚本自行拼 shell 截图命令。

这也是当前仓库已经表现出来的主方向。

### 建议 3

如果必须保留 shell 截图能力，就单独做一个“二进制截图 API”，不要复用 `execCmd(...)`。

这个专用 API 至少要满足：

1. 按字节流读取 stdout。
2. 同时消费 stderr，避免另一条管道堵塞。
3. 不使用 `BufferedReader.readLine()` 处理图片数据。
4. 不要先 `waitFor()` 再开始读大块输出。
5. 增加超时和失败日志。

### 建议 4

把仓库里与截图有关的误导性注释清理掉，明确区分两条链路：

- 主链路：`AppProcess.getAshmemData(...)`
- 旧链路/实验链路：shell `screencap -p`

否则以后继续排查时，看到注释很容易以为线上还在走 shell 截图。

## 最终判断

基于当前仓库代码，可以给出比较明确的判断：

1. JS 引擎里如果直接通过 shell 执行 `screencap -p`，仍然是高风险路径，因为 `execCmd(...)` 不适合处理截图这种二进制大输出。
2. 如果问题主要出现在清理缓存之后，那么更需要怀疑的是“模板缓存失效后的同步下载和比图链路”，而不是只盯着截图动作本身。
3. 当前项目里“底层截图”之所以没问题，是因为它已经绕开了 shell 管道，改走 `AppProcess.getAshmemData(...)`。
4. 当前最需要避免的有两类用法：
	- 把截图当普通 shell 文本命令去执行。
	- 在单线程同步链路里把“截图、模板下载、OpenCV 比图”串成一次冷启动重操作。

## 本次分析的边界

这份结论完全基于仓库内可见代码。

如果你本地线上脚本仓库里还有额外的 JS 代码直接调用了：

- `shell.cmd("screencap -p ...")`
- `shell.fastCmd("screencap -p ...")`

那么这些脚本本身就是触发卡死的直接入口，但这些脚本不在当前仓库里，所以这里只能从 Android 侧执行链确认根因。