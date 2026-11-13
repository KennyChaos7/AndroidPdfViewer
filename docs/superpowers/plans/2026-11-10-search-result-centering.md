# Search Result Centering Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan. Steps use checkbox syntax.

**Goal:** 非单页模式默认将点击的搜索结果居中，单页行为不变。

**Architecture:** 复用 PDF 坐标映射、页面偏移和 `moveTo`。在结果点击入口计算扣除搜索列表后的中心，不修改全局边界。

**Tech Stack:** Java, Android support RecyclerView, Gradle, Android instrumentation.

**Spec:** `docs/superpowers/specs/2026-11-10-search-result-centering-design.md`

## Global Constraints

- 在 `personal_custom` 当前工作区实施；保留公开方法签名和当前缩放。
- 仅使用真机；无设备时通知用户连接。
- 提交和推送单独授权，用户已于 2026-11-13 授权。

## Review Focus

旧开关 false、单页模式、横向布局、左右列表占位、文档边界均由下列测试覆盖。

### Task 1: Default result centering

**Files:** `PDFView.java`（点击和旧开关说明），`sample/build.gradle`（内置 instrumentation runner），`sample/src/androidTest/java/com/github/barteksc/pdfviewer/SearchResultCenteringTest.java`（真实点击），`README.md`（默认行为说明）。

**Interfaces:** 保留 `showSearchResultList(boolean,int,boolean,boolean)` 和 `setIsAllowToJumpToSpecificLocation(boolean)`；单页继续调用 `jumpTo`。

- [x] 添加真机测试，通过多个 assets PDF 的实际搜索及列表点击校验坐标，另验证单页与通用跳页的偏移一致。
- [x] 构建测试 APK；在真机运行，观察当前实现因开关或贴边定位而失败。
- [x] 在原搜索跳转方法中取消非单页开关依赖，以命中中心计算两轴偏移，复用现有边界和页面加载。
- [x] 更新旧参数说明及 README，运行同一真机测试，应全部通过。
- [x] 完成一次聚焦代码自查和真实显示核对；保留测试报告，汇报结果。

## 验证结果

- 设备：Lenovo TB-J706F，Android 12，物理设备。
- `sample2.pdf`、`sample4.pdf`、`sample6.pdf`：5 项点击测试全部通过，涵盖旧参数 false、左右列表、横纵向、默认和 2 倍缩放、同页不同命中、跨页和单页行为。
- PdfiumAndroid 既有真机测试：5 项通过。各模块单元测试任务无测试源，未计入上述数量。
- `sample3.pdf` 第 3–12 页未命中测试所用常见查询，改用 `sample2.pdf` 提供实际搜索结果，未修改该文件或搜索算法。
- 原实现失败记录：`build/search-centering-red-results.xml`；最终运行日志：`build/search-centering-final.log`。
- 实际整篇搜索并点击第 3 页结果的截图：`build/search-centering-verified.png`，命中可见且在纵向中心。
- 当前改动由主代理自查；独立审查的路由状态脚本未安装，未声称已完成独立审查。
- 后续整合搜索卡片侧栏，定位测试按新面板边界核对阅读区域。
- 后续复现最高倍率时右侧命中被侧栏遮挡，搜索跳转改为使用阅读区域的移动边界。
- 最终点击定位测试 7/7 通过，增加搜索后真实双击放大及最高倍率下命中可见性回归。构建日志为 `build/search-zoom-fix-build.log`，真机结果为 `build/search-zoom-fix-test.log`。
