# findImageGray 执行链说明

这份文档只描述当前分支里灰度找图相关的 Kotlin 执行链。

排查口径以实际进入 Kotlin API 的参数和运行时日志为准，不以用户手工贴出的原始参数为准。因为上层 JS / 中间层可能会先做裁剪、偏移、路径转换或缓存 id 处理。

## 范围

本说明覆盖以下入口：

1. `ImageApi.takeCapture(id, area)`
2. `ImageApi.findImageG(id, template, threshold)`
3. `ImageApi.findImagesG(id, template, threshold, maxMatches)`
4. `ImageApi.findImageCornersG(id, template, threshold)`

对应实现文件：

1. [app/src/main/java/com/android/ant/rhino/api/ImageApi.kt](app/src/main/java/com/android/ant/rhino/api/ImageApi.kt)
2. [app/src/main/java/com/android/ant/opencv/ImageMatcher.kt](app/src/main/java/com/android/ant/opencv/ImageMatcher.kt)
3. [app/src/main/java/com/android/ant/rhino/helper/Screencap.kt](app/src/main/java/com/android/ant/rhino/helper/Screencap.kt)
4. [app/src/main/java/com/android/ant/rhino/helper/ScreenCaptureProcess.kt](app/src/main/java/com/android/ant/rhino/helper/ScreenCaptureProcess.kt)

## 核心结论

1. 当前分支的灰度匹配算法本身没有切换为别的方案，核心仍然是 `BGR -> Gray -> TM_CCOEFF_NORMED`。
2. 真正容易和 `feature-v1.0.2` 拉开差异的点，主要在上游输入准备：截图来源、局部图路径解析、以及中间层如何把参数传进 Kotlin。
3. 当前实现已经做了兼容收敛：`takeCapture` 优先尝试旧链路 `Screencap/ImageReader`，拿不到时回退到当前 `ScreenCaptureProcess`；G 系列接口统一支持“直接文件路径”和“缓存 id”两种首参语义。

## 执行链

### 1. takeCapture

入口：`ImageApi.takeCapture(id, area)`

执行顺序：

1. 将 `id` 为空时归一化为 `"1"`。
2. 调用 `captureForTakeCapture()` 取图。
3. `captureForTakeCapture()` 先尝试 `captureViaLegacyScreencap()`。
4. `captureViaLegacyScreencap()` 只有在 `Screencap.hasImageReader()` 为真时才会继续。
5. 如果旧链路可用，就通过 `Screencap.takeImage()` 取到 `Image` 并转换成 `Bitmap`。
6. 如果旧链路不可用或失败，就回退到 `ScreenCaptureProcess.capture(...)`。
7. 取到 `Bitmap` 后，进入 `safeCrop(bitmap, area)` 裁剪局部图。
8. 裁剪结果通过 `Bitmap.save(path, quality)` 以 JPEG 质量 50 写入 `cache/<id>.jpg`。
9. 返回实际保存路径。

当前行为与 `feature-v1.0.2` 的关系：

1. `feature-v1.0.2` 的 `takeCapture` 直接依赖 `getImageReader().acquireLatestImage()`。
2. 当前分支为了兼容现有架构，优先尝试这条旧链路，再回退到新的 `screen` 链路。
3. 落盘格式仍保持为 JPEG，质量仍保持为 50，与 `feature-v1.0.2` 一致。

当前分支的一个重要现状：

1. 工程里目前没有活动中的 `Screencap.setImageReader(...)` 调用点。
2. 这意味着如果不重新启用旧的 `ImageReader` 初始化链，`takeCapture` 大多数情况下会直接落到 `screen` 回退链。
3. 所以日志里如果看到 `stage=takeCapture source=screen`，这通常不是匹配算法漂移，而是旧截图源当前没有被真正激活。

### 2. findImageG

入口：`ImageApi.findImageG(id, template, threshold)`

执行顺序：

1. 将 `id` 为空时归一化为 `"1"`。
2. 通过 `resolveMatchSourcePath(id)` 解析大图来源。
3. `resolveMatchSourcePath(id)` 先走 `resolveCaptureSourcePath(id)`。
4. `resolveCaptureSourcePath(id)` 优先把首参当作直接文件路径检查。
5. 如果直接路径不存在，再尝试 `cache/<id>.jpg`。
6. 若两者都找不到，则回退为原始 `id` 字符串，保持旧行为兼容。
7. 模板图通过 `getTemplateCacheFile(template)` 命中本地缓存，或通过 `downloadTemplateFile(...)` 下载到本地。
8. 最终调用 `ImageMatcher.templateMatchByGray(sourcePath, templatePath, threshold)`。
9. 匹配成功返回中心点，失败返回 `null`。

### 3. findImagesG

入口：`ImageApi.findImagesG(id, template, threshold, maxMatches)`

执行顺序与 `findImageG` 相同，差异只有最后一步：

1. 底层调用 `ImageMatcher.getImagePointsG(...)`。
2. 返回多点列表，而不是单个中心点。

### 4. findImageCornersG

入口：`ImageApi.findImageCornersG(id, template, threshold)`

执行顺序与 `findImageG` 相同，差异只有最后一步：

1. 底层调用 `ImageMatcher.templateMatchCornersG(...)`。
2. 如果匹配成功，返回四个角点组成的 JSON 字符串。

## 底层灰度匹配逻辑

### templateMatchByGray

逻辑：

1. 用 `Imgcodecs.imread(...)` 读取大图和模板图。
2. 两张图都转换为灰度图。
3. 使用 `Imgproc.matchTemplate(..., TM_CCOEFF_NORMED)` 做模板匹配。
4. 读取最大匹配分数和位置。
5. 若 `score > threshold`，返回模板中心点。
6. 否则返回 `null`。

### getImagePointsG

逻辑：

1. 读取大图和模板图。
2. 两张图都转换为灰度图。
3. 使用 `TM_CCOEFF_NORMED` 做匹配。
4. 循环取当前最高分位置。
5. 用 `distanceThreshold` 做去重。
6. 直到达到 `maxMatches` 或连续多次低于阈值。
7. 返回点列表。

### templateMatchCornersG

逻辑：

1. 读取大图和模板图。
2. 两张图都转换为灰度图。
3. 使用 `TM_CCOEFF_NORMED` 做匹配。
4. 若最高分大于阈值，则以最佳匹配左上角和模板宽高推导四个角点。
5. 返回角点列表。

## 运行时日志

当前灰度链统一使用日志前缀：

`jsapi-detect type=findImageGray`

建议重点看以下阶段：

1. `stage=takeCapture`
2. `stage=takeCapture-save`
3. `stage=findImageG-entry`
4. `stage=templateMatchByGray`
5. `stage=findImageG-exit`
6. `stage=findImagesG-entry`
7. `stage=getImagePointsG`
8. `stage=findImageCornersG-entry`
9. `stage=templateMatchCornersG`

字段含义：

1. `source=`：实际进入匹配的大图路径
2. `template=` 或 `templatePath=`：模板路径
3. `full=`：原始截图尺寸
4. `crop=`：裁剪后局部图尺寸
5. `format=`：落盘格式
6. `quality=`：落盘质量
7. `sourceSize=`：OpenCV 读到的大图尺寸
8. `templateSize=`：OpenCV 读到的模板尺寸
9. `threshold=`：匹配阈值
10. `score=`：最高匹配分数
11. `result=`：最终点位、角点或空结果

## feature-v1.0.2 对照点

当前与 `feature-v1.0.2` 最重要的对照点如下：

1. `takeCapture` 的截图来源是否是 `screencap`
2. `findImageG-entry` 里的 `source=` 是否指向预期的局部图路径
3. `templateMatchByGray` 里的 `sourceSize` 和 `templateSize` 是否正常
4. `score` 与 `result` 是否和旧分支现场一致

如果这几个字段一致而结果仍不同，再怀疑模板资源或上层调用链；如果前面字段都已经不一致，优先排查中间层传参或截图来源。

## 仍需注意的现状

1. 当前文档描述的是“代码实际行为”，不是“旧链路一定已被运行时激活”。
2. 如果目标是严格复现 `feature-v1.0.2` 的截图来源，仅仅保留优先尝试旧链路还不够，还需要把旧的 `ImageReader` 初始化链重新接回运行时。
3. 在没有接回这套初始化之前，灰度链已经尽量兼容旧语义，但取图来源仍可能是 `screen` 回退链。

## 本次顺手修正

本次在补日志的同时，顺手修了一个灰度链的局部风险：

1. `templateMatchByGray`
2. `getImagePointsG`
3. `templateMatchCornersG`

这三个方法现在都会在执行结束后释放内部 OpenCV `Mat`，避免高频灰度找图时持续积累本地内存占用。