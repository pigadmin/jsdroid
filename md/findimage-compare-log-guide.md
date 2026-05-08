# findImage 差异日志与统计说明

## 目的

这份文档说明 `findImage` 对比模式下，以下内容分别保存在哪里、什么情况下会写入、怎么看：

1. 差异日志
2. 对比截图缓存
3. 悬浮窗统计数据
4. 统计清零后的实际行为

补充说明：当前构建默认关闭多路对比采集，`findImage` 只抓当前主返回模式对应的一路截图；只有后续重新开启对比采集时，下面这些差异日志和对比截图才会继续增长。

## 差异日志保存目录

`findImage` 的差异日志保存在应用私有目录下的：

`files/findimage-compare-logs`

代码位置：

- `app/src/main/java/com/android/ant/rhino/api/ImageApi.kt`

实际设备路径通常是：

`/data/user/0/<当前包名>/files/findimage-compare-logs/`

例如 `ant` 包一般是：

`/data/user/0/com.android.ant/files/findimage-compare-logs/`

## 差异日志文件名

当前会按三组配对差异拆成 9 个文件：

1. `shell_hit_screen_miss.log`
2. `screen_hit_shell_miss.log`
3. `point_diff.log`
4. `shell_hit_projection_miss.log`
5. `projection_hit_shell_miss.log`
6. `shell_projection_point_diff.log`
7. `screen_hit_projection_miss.log`
8. `projection_hit_screen_miss.log`
9. `screen_projection_point_diff.log`

分别表示：

1. `shell` 能找到，`screen` 找不到
2. `screen` 能找到，`shell` 找不到
3. `shell` 和 `screen` 都找到了，但点位不同
4. `shell` 能找到，`projection` 找不到
5. `projection` 能找到，`shell` 找不到
6. `shell` 和 `projection` 都找到了，但点位不同
7. `screen` 能找到，`projection` 找不到
8. `projection` 能找到，`screen` 找不到
9. `screen` 和 `projection` 都找到了，但点位不同

## 什么情况下会写日志

只有发生“差异”时才会追加写入日志：

1. `shell` 命中、`screen` 未命中
2. `screen` 命中、`shell` 未命中
3. `shell` 和 `screen` 都命中，但返回点位不同
4. `shell` 命中、`projection` 未命中
5. `projection` 命中、`shell` 未命中
6. `shell` 和 `projection` 都命中，但返回点位不同
7. `screen` 命中、`projection` 未命中
8. `projection` 命中、`screen` 未命中
9. `screen` 和 `projection` 都命中，但返回点位不同

以下情况不会写入这些差异日志文件：

1. 两边识别结果一致
2. 其中一路没有拿到截图，属于“没法比较”

所以如果你去目录里看不到某个文件，通常说明对应类型的差异还没发生。

## 单行日志格式

当前每条差异日志大致长这样：

```text
时间戳|模板路径|mode=返回模式|shell=点位,分数,c取图耗时,m识别耗时|screen=点位,分数,c取图耗时,m识别耗时|projection=点位,分数,c取图耗时,m识别耗时
```

示例：

```text
1775663805148|ant/a.png|mode=shell|shell=(540,960),0.9987,c85,m22|screen=null,0.4123,c126,m20|projection=(540,960),0.9979,c148,m24
```

字段说明：

1. `mode` 是当前 `findImage` 最终返回结果采用的模式
2. `shell` 后面的 `c` 是取图耗时，单位毫秒
3. `shell` 后面的 `m` 是识别耗时，单位毫秒
4. `screen` 表示底层截图，它后面的 `c`、`m` 含义相同
5. `projection` 表示系统截图，也就是 MediaProjection 方案

## 对比截图缓存目录

日志文件和对比截图不是放在同一个目录。

如果当前版本仍保持默认配置，那么通常只有“当前主返回模式”对应的那一张最新截图会被刷新，其他链路不会在每次找图时都参与抓图。

对比截图缓存保存在：

`cache/findimage-compare`

代码位置：

- `app/src/main/java/com/android/ant/rhino/api/ImageApi.kt`

实际设备路径通常是：

`/data/user/0/<当前包名>/cache/findimage-compare/`

当前主要会看到：

1. `findimage_shell_latest.png`
2. `findimage_screen_latest.png`
3. `findimage_projection_latest.png`

注意：这是“最新一次”覆盖式缓存，不是历史累积。

## 统计悬浮窗显示什么

统计悬浮窗当前会显示这些信息：

1. 返回模式
2. 识别画面次数
3. `shell` 取图延迟，本次值和均值
4. `shell` 识别延迟，本次值和均值
5. `底层截图` 取图延迟，本次值和均值
6. `底层截图` 识别延迟，本次值和均值
7. `系统截图` 取图延迟，本次值和均值
8. `系统截图` 识别延迟，本次值和均值
9. 系统 CPU 占用，本次值和均值
10. App CPU 占用，本次值和均值
11. 系统内存占用
12. App 堆占用
13. `shell/底层` 可比较次数、一致次数、差异次数
14. `shell/系统` 可比较次数、一致次数、差异次数
15. `底层/系统` 可比较次数、一致次数、差异次数
16. 各配对的分类差异次数

悬浮窗行为：

1. 默认隐藏
2. 点小蚂蚁后和截图菜单一起显示
3. 再点一次小蚂蚁会一起隐藏

## 清零统计会做什么

悬浮窗上的“清零统计”当前只会清掉内存中的累计统计，并同步更新 MMKV：

1. 画面次数
2. 各类差异次数
3. shell、底层、系统 取图与识别耗时累计值
4. CPU 统计累计值
5. 内存快照显示

但它不会删除这些差异日志文件：

1. `shell_hit_screen_miss.log`
2. `screen_hit_shell_miss.log`
3. `point_diff.log`
4. `shell_hit_projection_miss.log`
5. `projection_hit_shell_miss.log`
6. `shell_projection_point_diff.log`
7. `screen_hit_projection_miss.log`
8. `projection_hit_screen_miss.log`
9. `screen_projection_point_diff.log`

如果需要连日志文件一起清空，需要额外实现“清日志”动作。

## 返回模式说明

统计悬浮窗顶部的“返回结果”按钮可以切换：

1. `shell截图`
2. `底层截图`
3. `系统截图`

切换后，`findImage` 最终返回点位会严格跟随当前模式，不再自动拿另一条截图路径兜底。

差异日志中的 `mode=` 字段也会记录当时的返回模式，便于排查。

## 常用排查方式

### 直接看私有目录

如果设备有 root，可以直接看：

```sh
su -c ls /data/user/0/<当前包名>/files/findimage-compare-logs
su -c cat /data/user/0/<当前包名>/files/findimage-compare-logs/point_diff.log
```

### 拷到可见目录

如果想导出到 `Download` 再看，可以手动拷贝：

```sh
su -c cp /data/user/0/<当前包名>/files/findimage-compare-logs/*.log /sdcard/Download/
```

### 同时看缓存截图

如果要对照日志里的时间和识别结果，建议同时看：

1. `files/findimage-compare-logs/*.log`
2. `cache/findimage-compare/findimage_shell_latest.png`
3. `cache/findimage-compare/findimage_screen_latest.png`
4. `cache/findimage-compare/findimage_projection_latest.png`

## 现状限制

当前实现有两个限制：

1. 差异日志只记“发生差异”的历史，不记“完全一致”的历史
2. 对比截图缓存只保留最新一张 shell 图、screen 图、授权图，不保留每次历史截图

如果后续需要，也可以继续扩成：

1. 导出差异日志到 `Download`
2. 一键清空差异日志
3. 记录每次对比截图历史
4. 记录一致样本日志