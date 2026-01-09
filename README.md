# README
> 本仓库只用于存储对PdfViewer的个性化修改

- [x] 逐帧数据绘制完成回调
- [x] 增加loading页面
  -  - [ ] 可以考虑是否增加到每个bitmap上
- [x] 增加是否进行pdfView缩略图分块渲染设置
  - 创建PdfView时，**PdfView.isThumbnailSplit(true)** 可以设置为缩略图分块渲染模式
- [ ] 单页模式
  - - [x] 单页模式1.0 
    - 拥有基本单页效果
    - 创建PdfView时，**PdfView.singlePageMode(true)** 可以设置为单页模式
    - 可以直接依靠**PdfView.loadNextPage()**, **PdfView.loadPreviousPage()** 进行上下页切换
  - - [x] 优化滑动和缩放的效果
  - - [ ] 处理单页模式下的页码不正确问题
  - - [ ] 先加载缩略图后，再加载原图
  - 原本pdfview中也有单页模式，需要在PdfView.pages()中填入相应的页码，限制要读取的页码数据。但在实际的情况下的单页模式，更多是取消连续滑动，靠下一页按钮进行切换，但仍需要读取全部页码数据。
- [ ] 双页模式
  - 在同个屏幕渲染两页pdf
- [ ] 更新最新的FPDF库
- [ ] 兼容原仓库的16K问题修复

