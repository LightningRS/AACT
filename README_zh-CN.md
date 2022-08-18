# AACT: Android Application Component Tester

安卓应用组件自动测试工具

## 运行环境

+ Java：Java 17 及以上版本

+ Android：5.0 及以上版本

### 关于 Android 系统签名

目前仅支持在 AOSP 原始 test-key 签名的 Android 系统上运行。

[参考链接 1](https://android.googlesource.com/platform/build/+/master/target/product/security)

[参考链接 2](https://github.com/wfairclough/android_aosp_keys)

工具开发时主要使用的是 [雷电模拟器](https://www.423down.com/7476.html) 64位版本。

## 运行准备

首先，请准备好待测的 APK 文件。建议放置在 `data/APKs` 目录中。

使用 ICCBot 工具分析所有 APK 文件，得到的结果建议放置在 `data/ICCResult` 目录中。

ICCBot 工具执行命令参考：

```bat
cd ICCBot/
python ./scripts/runICCBot-spec.py ../data/APKs ../data/ICCResult
```

按照 [运行参数说明](#运行参数说明) 中的相关说明，设置参数，执行本工具。

## 运行参数说明

工具默认的启动命令行为 `java -jar AACT-1.0.jar`。各参数说明如下：

| 参数简称 | 参数全称 | 是否必需 | 参数描述 | 示例值 |
| :------ | :------- | :-----: | :------ | :----- |
| `-k`  | `--apk-path` | 是 | 待测 APK 的所在目录 | /path/to/apk-path | 
| `-i`  | `--icc-result-path` | 是 | 待测 APK 的 ICCBot 工具分析结果所在目录 | /path/to/icc-result-path |
| `-c`  | `--testcase-path` | 是 | 待测 APK 的测试用例存放目录 | /path/to/testcase-path |
| `-st` | `--strategy` | 是 | 测试所采用的的测试用例生成策略。参见 [测试用例生成策略](#测试用例生成策略) | preset; random+iccBot |
| `-a`  | `--adb-path` | 否 | ADB 可执行文件路径 | /path/to/adb |
| `-ln` | `--android-launcher-package-name` | 否 | Android 系统当前默认启动器包名 | com.android.launcher3 |
| `-o` | `--scope-config` | 否 | 启用的 ICCBot 工具分析域。参见 [ICCBot 结果分析域设置](#ICCBot-结果分析域设置) | /path/to/scope-config-csv
| `-d` | `--device` | 否 | ADB 连接到的 Android 设备序列号。当同时有多台 Android 设备连接到主控机时，必须指定该参数（可通过 `adb devices` 命令获得）| emulator-5554 |
| `-s`  | `--seed` | 否 | 测试所使用的随机种子数值 | 12345678 |
| `-r` | `--rand-val-num` | 否 | 生成随机数据时使用的随机种子数值。默认为当前毫秒级时间戳 | 12345678 |
| `-ce` | `--continue-if-error` | 否 | 是否在发现崩溃时继续执行测试。默认为否（发现崩溃时立即终止测试） | false
| `-smin` | `--str-min-length` | 否 | 生成随机字符串的最小长度。默认为 1 | 1 |
| `-smax` | `--str-max-length` | 否 | 生成随机字符串的最大长度。默认为 20 | 20 |
| `-ia` | `--start-apk-index` | 否 | 待测 APK 的起始序号。对应日志输出的 `apkIndex`。默认为 0 (*用于意外中断时继续测试*) | 0 |
| `-ic` | `--start-component-index` | 否 | 待测组件的起始序号。对应日志输出的 `compIndex`。默认为 0 (*用于意外中断时继续测试*) | 0 |
| `-it` | `--start-testcase-index` | 否 | 待测用例的起始序号。对应日志输出的 `caseIndex`。默认为 0 (*用于意外中断时继续测试*) | 0 |
| `-is` | `--start-strategy-index` | 否 | 待测的起始策略。对应日志输出的 `strategy`。默认为空 (*用于意外中断时继续测试*) | preset+iccBot |
| `-ag` | `--auto-generate` | * | 自动生成测试用例。当测试用例不存在时，会自动生成。 | 无 |
| `-og` | `--only-generate` | * | 仅生成测试用例。只生成测试用例，不执行测试。 | 无 |
| `-ng` | `--not-generate` | * | 不生成测试用例。忽略不存在的测试用例。 | 无 |

*注：`-ag`、`-og` 和 `-ng` 参数能且仅能三者择一。

### 参数示例

以下参数适用于 **Windows 系统的命令提示符环境**，将仅生成测试用例，不执行。

```bat
java -jar AACT-1.0.jar ^
 -k "data/APKs" ^
 -i "data/ICCResult" ^
 -c "data/Testcases" ^
 -s 12345678 ^
 -r 3 ^
 -og ^
 -st "iccBot; random; randomWithStruct; iccBot+preset; iccBot+preset+randomWithStruct"
```

以下参数适用于 **Windows 系统的命令提示符环境**，将自动生成测试用例，并执行测试。

```bat
java -jar AACT-1.0.jar ^
 -a "adb.exe" ^
 -ln "com.android.launcher3" ^
 -k "data/APKs" ^
 -i "data/ICCResult" ^
 -c "data/Testcases" ^
 -s 12345678 ^
 -r 3 ^
 -d "emulator-5556" ^
 -ag ^
 -ce ^
 -st "iccBot; random; randomWithStruct; iccBot+preset; iccBot+preset+randomWithStruct"
```

### 测试用例生成策略

目前支持的测试用例生成策略如下：

| 策略名 | 策略描述 |
| :----- | :------ |
| `preset` | 该策略包含一些 *Intent* 参数常见的预设值 |
| `iccBot` | 该策略包含 ICCBot 分析得到的 *Intent* 各参数及字段取值 |
| `random` | 该策略随机生成 *Intent* 各参数的值（不包含 *extra* 参数）|
| `randomWithStruct` | 该策略在 `random` 策略的基础上，借助 ICCBot 的分析结果构造 `extra` 参数结构，并为各字段填充随机值 |

启动工具时指定的 `-st` (`--strategy`) 参数可使用 `策略名` 中的任意值。

一次可以指定多组策略，每组策略间用分号 `;` 分隔（空格会被自动忽略）。
每组策略独立生成测试用例，独立运行，互不影响。

策略之间可以相互组合。每种策略间用加号 `+` 分隔（空格会被自动忽略）。
策略组合后生成测试用例时，同一字段的相同值将被忽略，同一字段的不同值将被合并。

注：`random` 策略生成的值是 `randomWithStruct` 策略生成值的子集，因此二者择一使用即可，无需组合使用。

以下参数设置是合法的：

```shell
-st "preset"
-st "presetrandom"
-st "preset+randomWithStruct; iccBot"
-st "preset; iccBot+random; randomWithStruct"
```

### ICCBot 结果分析域设置

ICCBot 工具提供的结果可划分为以下四种分析域；

| 分析域名 | 分析域描述 |
| :------ | :-------- |
| `sendIntent` | 该分析域包含 Intent 发送过程的分析结果 |
| `recvIntent` | 该分析域包含 Intent 接收过程的分析结果 |
| `manifest` | 该分析域包含 AndroidManifest.xml 中的分析结果 |
| `specIntent` | 该分析域包含其它分析方法下的分析结果 |

当 [测试用例生成策略](#测试用例生成策略) 中使用了 `iccBot` 策略时，可配置具体使用哪些分析域。

分析域的配置通过以下形式的 CSV 文件实现：

```csv
,manifest,sendIntent,recvIntent,specIntent
action,1,1,1,1
category,1,1,1,1
data,1,1,1,1
extra,0,1,1,0
flag,0,1,1,1
type,1,1,1,1
```

其中行名为分析域名，列名为 *Intent* 的各参数字段名。
值为 1 表示包含该字段在该分析域下的结果，值为 0 表示不包含。

工具启动参数中的 `-o` (`--scope-config`) 参数值即为该 CSV 文件的路径。
当未指定该参数时，采用如上示例所示的默认配置。

## 运行结果

目前尚未完成结果的可视化，但可通过查阅 `logs/test-controller.log` 日志文件分析测试结果。

当工具执行测试用例发现错误时，会打印出包含关键字 `Case FAILED!` 的日志。例如：

```log
[2022-06-10 16:33:31.961] main WARN c.i.testcontroller.TestController [TestController.java:316] - Stacktrace caught: java.lang.NullPointerException: Attempt to invoke virtual method 'boolean java.lang.String.startsWith(java.lang.String)' on a null object reference
java.lang.NullPointerException: Attempt to invoke virtual method 'boolean java.lang.String.startsWith(java.lang.String)' on a null object reference
	at android.net.Uri.checkFileUriExposed(Uri.java:2398)
	at android.content.Intent.prepareToLeaveProcess(Intent.java:8980)
	at android.content.Intent.prepareToLeaveProcess(Intent.java:8941)
	at android.app.Instrumentation.execStartActivity(Instrumentation.java:1517)
	at android.app.Activity.startActivityForResult(Activity.java:4244)
	at android.app.Activity.startActivityForResult(Activity.java:4202)
	at com.test.apptestclient.controller.TestController.startActivity(TestController.java:32)
	at com.test.apptestclient.controller.handlers.RunCaseHandler.handle(RunCaseHandler.java:41)
	at com.test.apptestclient.controller.rpc.RPCClientHandler.run(RPCClientHandler.java:86)
[2022-06-10 16:33:31.961] main ERROR c.i.testcontroller.TestController [TestController.java:326] - Case FAILED! state=INTENT_ERROR, mFocusedActivity=at.bitfire.icsdroid/.ui.AddCalendarActivity, comp=at.bitfire.icsdroid.ui.AddCalendarActivity, apkIndex=0, caseIndex=7, compIndex=2, strategy=iccBot
```

该行日志会给出该用例的执行结果，其中包含组件状态名 `state` 以及当前用例的相关信息。
该行日志附近将会打印出收集到的调用栈追踪信息（如果有）。

测试用例的具体执行结果通过状态名 `state` 判断。组件的状态分为以下类别：

| 状态名 | 状态值 | 状态描述 |
| :---- | :----- | :------- |
| CLIENT_ERROR | 3 | AACT 测试客户端出现问题。该状态下，测试客户端将被重启，当前用例将被重新执行。|
| INTENT_ERROR | 4 | AACT 测试客户端生成的 Intent 存在问题，导致无法通过系统 API 启动组件。这种情况是测试用例的问题，而非目标组件的问题。|
| SYS_ERROR | 5 | Android 系统未成功启动目标组件。这种情况可能是由于没有启动目标应用的权限，或 Android 系统校验 Intent 不通过导致的，非目标组件问题。|
| APP_CRASHED | 6 | Intent 已发送给目标组件，且组件发生崩溃。将被记入 `独立崩溃` 统计 |
| TIMEOUT | 7 | Intent 已发送给目标组件，但目标组件在规定时间内未启动。这可能是由于目标组件假死导致的，将被记入 `独立崩溃` 统计 |
| DISPLAYED_TIMEOUT | 8 | 目标组件已启动，但未能在规定时间内维持运行。这可能是由于目标组件启动后发生崩溃已导致的，将被记入 `独立崩溃` 统计 |
| JUMPED | 9 | (仅针对 Activity) 当前显示的 Activity 并非启动的目标组件，且非 AACT 测试客户端。这可能是由于目标组件启动后跳转到其它 Activity 导致的* |
| SUCCESS | 10 | 目标组件启动成功 |

*注：若目标 Activity 启动后跳转到了启动器【通过执行参数 `-ln` (`--android-launcher-package-name`) 指定】，则认为是目标应用发生了崩溃，状态将被设为 `APP_CRASHED`。