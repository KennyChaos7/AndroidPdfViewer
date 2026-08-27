# README
> 本仓库只用于存储对PdfViewer的个性化修改

- [x] 编译构建pdfium并打包生成so，将其应用在本仓库中
  - [详细编译办法流程和脚本参考本人的另一个仓库**how_to_build_pdfium_so_on_2026**](https://github.com/KennyChaos7/how_to_build_pdfium_so_on_2026)
- [x] 逐帧数据绘制完成回调
- [x] 增加loading页面
  - [ ] 可以考虑是否增加到每个bitmap上
- [x] 增加是否进行pdfView缩略图分块渲染设置
  - 创建PdfView时，**PdfView.isThumbnailSplit(true)** 可以设置为缩略图分块渲染模式
  - 支持在**Constants**中直接修改具体块数，横向**THUMBNAIL_SPLIT_VERTICAL**纵向**THUMBNAIL_SPLIT_LEVEL**
  - **分的块数越多会导致实际显示效果不佳**
- [ ] 单页模式
  - - [x] 单页模式1.0 
    - 原本pdfview中也有单页模式，需要在PdfView.pages()中填入相应的页码，限制要读取的页码数据。但在实际的情况下的单页模式，更多是取消连续滑动，靠下一页按钮进行切换，但仍需要读取全部页码数据。
    - 拥有基本单页效果
      - 创建PdfView时，**PdfView.singlePageMode(true)** 可以设置为单页模式
      - 可以直接依靠**PdfView.loadNextPage()**, **PdfView.loadPreviousPage()** 进行上下页切换
      - 兼容jumpTo等方法
  - - [x] 优化滑动和缩放的效果
  - - [x] 处理单页模式下的页码不正确问题
  - - [ ] 先加载缩略图后，再加载原图（原pdf库中的加载顺序，是先**所有缩略图**，然后是**所有原图**，这样会导致单页模式下加载时会出现"**先A页缩略图，覆盖上B页缩略图，接着又被A页原图覆盖。最后被B页原图覆盖**"这种情况，所以可以考虑使用**PriorityQueue**来处理，仅以入队顺序作为加载的先后，待考虑功能）
    - 需要进行优先度区分，先加载当前显示的页面，然后再按照加入的顺序区分
    - ~~可以考虑优化加载区块，如果区块A中已经加载了原图，则不渲染这块的缩略图，如果未加载原图，再去加载缩略图（需要通过判断原图的RectF是否在此块缩略图的RectF中）~~ 并未能加快加载速度，本身canvas的draw方法并不会过于耗时，主耗时的区域还是在fpdf中的读取文件时候
- [ ] 双页模式
  - 在同个屏幕渲染两页pdf
- [ ] 是否渲染到opengles中
- [x] 更新最新的FPDF库
- [ ] 兼容原仓库的16K问题修复
- [x] Pdf文件内文字搜索 (**本搜索中使用到了FPDF库中较新版本的一些Experimental API.**  ~~**所以需要重新编译较新的FPDF库**~~ **已经建编译后的较新FPDF库so更新至项目中** [详细编译办法流程和脚本参考本人的另一个仓库**how_to_build_pdfium_so_on_2026**](https://github.com/KennyChaos7/how_to_build_pdfium_so_on_2026))
  - [x] 支持当前页搜索
  - [x] 支持跨页搜索
  - [x] 支持搜索后染色
    - [x] 支持原本滑动模式当前页
    - [x] 支持单页模式
    - [ ] 支持修改高亮颜色
  - [x] 搜索结果的列表展示 
    > 具体通过**showSearchResultList**设置
    - [x] 支持搜索结果的列表自定义居左还是右
    - [x] 搜索结果要按页排序或者全部列出
      - 例如搜索结构中，存在单页里有多个结果时候的，则通过设置**showSearchResultList**列出全部结果，并且在点击跳转时自动将结果居中显示
    - [x] 支持跳到具体页面，或者具体位置
      > 通过设置**showSearchResultList**的**isAllowToJumpToSpecificLocation**来选择是跳到具体页面还是具体位置
  - [ ] 支持多种搜索的匹配模式(例如完全匹配或者模糊匹配等)
- [ ] Pdf文件后插入新的页面(功能细则思考中.......)