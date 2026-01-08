/**
 * Copyright 2017 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.barteksc.pdfviewer;

import android.graphics.RectF;
import android.util.Log;

import com.github.barteksc.pdfviewer.util.Constants;
import com.github.barteksc.pdfviewer.util.MathUtils;
import com.github.barteksc.pdfviewer.util.Util;
import com.shockwave.pdfium.util.SizeF;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.github.barteksc.pdfviewer.util.Constants.Cache.CACHE_SIZE;
import static com.github.barteksc.pdfviewer.util.Constants.PRELOAD_OFFSET;

class PagesLoader {

    private PDFView pdfView;
    private int cacheOrder;
    private float xOffset;
    private float yOffset;
    private float pageRelativePartWidth;
    private float pageRelativePartHeight;
    private float partRenderWidth;
    private float partRenderHeight;
    private final RectF thumbnailRect = new RectF(0, 0, 1, 1);
    private List<RectF> splitRectfList = new ArrayList<>(Constants.THUMBNAIL_SPLIT);
    private final int preloadOffset;

    private class Holder {
        int row;
        int col;

        @Override
        public String toString() {
            return "Holder{" +
                    "row=" + row +
                    ", col=" + col +
                    '}';
        }
    }

    private class RenderRange {
        int page;
        GridSize gridSize;
        Holder leftTop;
        Holder rightBottom;

        RenderRange() {
            this.page = 0;
            this.gridSize = new GridSize();
            this.leftTop = new Holder();
            this.rightBottom = new Holder();
        }

        @Override
        public String toString() {
            return "RenderRange{" +
                    "page=" + page +
                    ", gridSize=" + gridSize +
                    ", leftTop=" + leftTop +
                    ", rightBottom=" + rightBottom +
                    '}';
        }
    }

    private class GridSize {
        int rows;
        int cols;

        @Override
        public String toString() {
            return "GridSize{" +
                    "rows=" + rows +
                    ", cols=" + cols +
                    '}';
        }
    }

    PagesLoader(PDFView pdfView) {
        this.pdfView = pdfView;
        this.preloadOffset = Util.getDP(pdfView.getContext(), PRELOAD_OFFSET);

        // TODO 每行的块数，利用开次方计算 ，目前先用固定分成4块处理
//            int splitInVertical = (int) Math.sqrt(Constants.THUMBNAIL_SPLIT);
        int splitInVertical = Constants.THUMBNAIL_SPLIT / 2;
        divideRectangle(thumbnailRect, 0, splitInVertical, splitRectfList);
//            Log.e("loadThumbnail" , splitRectfList.size() + " - " + splitInVertical);
    }

    private void getPageColsRows(GridSize grid, int pageIndex) {
        SizeF size = pdfView.pdfFile.getPageSize(pageIndex);
        float ratioX = 1f / size.getWidth();
        float ratioY = 1f / size.getHeight();
        final float partHeight = (Constants.PART_SIZE * ratioY) / pdfView.getZoom();
        final float partWidth = (Constants.PART_SIZE * ratioX) / pdfView.getZoom();
        grid.rows = MathUtils.ceil(1f / partHeight);
        grid.cols = MathUtils.ceil(1f / partWidth);
    }

    private void calculatePartSize(GridSize grid) {
        pageRelativePartWidth = 1f / (float) grid.cols;
        pageRelativePartHeight = 1f / (float) grid.rows;
        partRenderWidth = Constants.PART_SIZE / pageRelativePartWidth;
        partRenderHeight = Constants.PART_SIZE / pageRelativePartHeight;
    }


    /**
     * calculate the render range of each page
     */
    private List<RenderRange> getRenderRangeList(float firstXOffset, float firstYOffset, float lastXOffset, float lastYOffset) {

        float fixedFirstXOffset = -MathUtils.max(firstXOffset, 0);
        float fixedFirstYOffset = -MathUtils.max(firstYOffset, 0);

        float fixedLastXOffset = -MathUtils.max(lastXOffset, 0);
        float fixedLastYOffset = -MathUtils.max(lastYOffset, 0);

        float offsetFirst = pdfView.isSwipeVertical() ? fixedFirstYOffset : fixedFirstXOffset;
        float offsetLast = pdfView.isSwipeVertical() ? fixedLastYOffset : fixedLastXOffset;

        int firstPage = pdfView.pdfFile.getPageAtOffset(offsetFirst, pdfView.getZoom());
        int lastPage = pdfView.pdfFile.getPageAtOffset(offsetLast, pdfView.getZoom());
        int pageCount = lastPage - firstPage + 1;

        List<RenderRange> renderRanges = new LinkedList<>();

        for (int page = firstPage; page <= lastPage; page++) {
            RenderRange range = new RenderRange();
            range.page = page;

            float pageFirstXOffset, pageFirstYOffset, pageLastXOffset, pageLastYOffset;
            if (page == firstPage) {
                pageFirstXOffset = fixedFirstXOffset;
                pageFirstYOffset = fixedFirstYOffset;
                if (pageCount == 1) {
                    pageLastXOffset = fixedLastXOffset;
                    pageLastYOffset = fixedLastYOffset;
                } else {
                    float pageOffset = pdfView.pdfFile.getPageOffset(page, pdfView.getZoom());
                    SizeF pageSize = pdfView.pdfFile.getScaledPageSize(page, pdfView.getZoom());
                    if (pdfView.isSwipeVertical()) {
                        pageLastXOffset = fixedLastXOffset;
                        pageLastYOffset = pageOffset + pageSize.getHeight();
                    } else {
                        pageLastYOffset = fixedLastYOffset;
                        pageLastXOffset = pageOffset + pageSize.getWidth();
                    }
                }
            } else if (page == lastPage) {
                float pageOffset = pdfView.pdfFile.getPageOffset(page, pdfView.getZoom());

                if (pdfView.isSwipeVertical()) {
                    pageFirstXOffset = fixedFirstXOffset;
                    pageFirstYOffset = pageOffset;
                } else {
                    pageFirstYOffset = fixedFirstYOffset;
                    pageFirstXOffset = pageOffset;
                }

                pageLastXOffset = fixedLastXOffset;
                pageLastYOffset = fixedLastYOffset;

            } else {
                float pageOffset = pdfView.pdfFile.getPageOffset(page, pdfView.getZoom());
                SizeF pageSize = pdfView.pdfFile.getScaledPageSize(page, pdfView.getZoom());
                if (pdfView.isSwipeVertical()) {
                    pageFirstXOffset = fixedFirstXOffset;
                    pageFirstYOffset = pageOffset;

                    pageLastXOffset = fixedLastXOffset;
                    pageLastYOffset = pageOffset + pageSize.getHeight();
                } else {
                    pageFirstXOffset = pageOffset;
                    pageFirstYOffset = fixedFirstYOffset;

                    pageLastXOffset = pageOffset + pageSize.getWidth();
                    pageLastYOffset = fixedLastYOffset;
                }
            }

            getPageColsRows(range.gridSize, range.page); // get the page's grid size that rows and cols
            SizeF scaledPageSize = pdfView.pdfFile.getScaledPageSize(range.page, pdfView.getZoom());
            float rowHeight = scaledPageSize.getHeight() / range.gridSize.rows;
            float colWidth = scaledPageSize.getWidth() / range.gridSize.cols;


            // get the page offset int the whole file
            // ---------------------------------------
            // |            |           |            |
            // |<--offset-->|   (page)  |<--offset-->|
            // |            |           |            |
            // |            |           |            |
            // ---------------------------------------
            float secondaryOffset = pdfView.pdfFile.getSecondaryPageOffset(page, pdfView.getZoom());

            // calculate the row,col of the point in the leftTop and rightBottom
            if (pdfView.isSwipeVertical()) {
                range.leftTop.row = MathUtils.floor(Math.abs(pageFirstYOffset - pdfView.pdfFile.getPageOffset(range.page, pdfView.getZoom())) / rowHeight);
                range.leftTop.col = MathUtils.floor(MathUtils.min(pageFirstXOffset - secondaryOffset, 0) / colWidth);

                range.rightBottom.row = MathUtils.ceil(Math.abs(pageLastYOffset - pdfView.pdfFile.getPageOffset(range.page, pdfView.getZoom())) / rowHeight);
                range.rightBottom.col = MathUtils.floor(MathUtils.min(pageLastXOffset - secondaryOffset, 0) / colWidth);
            } else {
                range.leftTop.col = MathUtils.floor(Math.abs(pageFirstXOffset - pdfView.pdfFile.getPageOffset(range.page, pdfView.getZoom())) / colWidth);
                range.leftTop.row = MathUtils.floor(MathUtils.min(pageFirstYOffset - secondaryOffset, 0) / rowHeight);

                range.rightBottom.col = MathUtils.floor(Math.abs(pageLastXOffset - pdfView.pdfFile.getPageOffset(range.page, pdfView.getZoom())) / colWidth);
                range.rightBottom.row = MathUtils.floor(MathUtils.min(pageLastYOffset - secondaryOffset, 0) / rowHeight);
            }

            renderRanges.add(range);
        }

        return renderRanges;
    }

    private RenderRange getRenderRange(int page, float firstXOffset, float firstYOffset, float lastXOffset, float lastYOffset) {

        float fixedFirstXOffset = -MathUtils.max(firstXOffset, 0);
        float fixedFirstYOffset = -MathUtils.max(firstYOffset, 0);

        float fixedLastXOffset = -MathUtils.max(lastXOffset, 0);
        float fixedLastYOffset = -MathUtils.max(lastYOffset, 0);

        float offsetFirst = pdfView.isSwipeVertical() ? fixedFirstYOffset : fixedFirstXOffset;
        float offsetLast = pdfView.isSwipeVertical() ? fixedLastYOffset : fixedLastXOffset;

        int pageCount = 1;
        RenderRange range = new RenderRange();
        range.page = page;

        float pageFirstXOffset, pageFirstYOffset, pageLastXOffset, pageLastYOffset;
        pageFirstXOffset = fixedFirstXOffset;
        pageFirstYOffset = fixedFirstYOffset;
        pageLastXOffset = fixedLastXOffset;
        pageLastYOffset = fixedLastYOffset;
        getPageColsRows(range.gridSize, range.page); // get the page's grid size that rows and cols
        SizeF scaledPageSize = pdfView.pdfFile.getScaledPageSize(range.page, pdfView.getZoom());
        float rowHeight = scaledPageSize.getHeight() / range.gridSize.rows;
        float colWidth = scaledPageSize.getWidth() / range.gridSize.cols;


        // get the page offset int the whole file
        // ---------------------------------------
        // |            |           |            |
        // |<--offset-->|   (page)  |<--offset-->|
        // |            |           |            |
        // |            |           |            |
        // ---------------------------------------
        float secondaryOffset = pdfView.pdfFile.getSecondaryPageOffset(page, pdfView.getZoom());

        // calculate the row,col of the point in the leftTop and rightBottom
        if (pdfView.isSwipeVertical()) {
            range.leftTop.row = MathUtils.floor(Math.abs(pageFirstYOffset - pdfView.pdfFile.getPageOffset(range.page, pdfView.getZoom())) / rowHeight);
            range.leftTop.col = MathUtils.floor(MathUtils.min(pageFirstXOffset - secondaryOffset, 0) / colWidth);

            range.rightBottom.row = MathUtils.ceil(Math.abs(pageLastYOffset - pdfView.pdfFile.getPageOffset(range.page, pdfView.getZoom())) / rowHeight);
            range.rightBottom.col = MathUtils.floor(MathUtils.min(pageLastXOffset - secondaryOffset, 0) / colWidth);
        } else {
            range.leftTop.col = MathUtils.floor(Math.abs(pageFirstXOffset - pdfView.pdfFile.getPageOffset(range.page, pdfView.getZoom())) / colWidth);
            range.leftTop.row = MathUtils.floor(MathUtils.min(pageFirstYOffset - secondaryOffset, 0) / rowHeight);

            range.rightBottom.col = MathUtils.floor(Math.abs(pageLastXOffset - pdfView.pdfFile.getPageOffset(range.page, pdfView.getZoom())) / colWidth);
            range.rightBottom.row = MathUtils.floor(MathUtils.min(pageLastYOffset - secondaryOffset, 0) / rowHeight);
        }
        return range;
    }

    private void loadVisible() {
        int parts = 0;
        float scaledPreloadOffset = preloadOffset;
        float firstXOffset = -xOffset + scaledPreloadOffset;
        float lastXOffset = -xOffset - pdfView.getWidth() - scaledPreloadOffset;
        float firstYOffset = -yOffset + scaledPreloadOffset;
        float lastYOffset = -yOffset - pdfView.getHeight() - scaledPreloadOffset;

        List<RenderRange> rangeList = getRenderRangeList(firstXOffset, firstYOffset, lastXOffset, lastYOffset);

        for (RenderRange range : rangeList) {
            loadThumbnail(range.page);
        }

        for (RenderRange range : rangeList) {
            calculatePartSize(range.gridSize);
            parts += loadPage(range.page, range.leftTop.row, range.rightBottom.row, range.leftTop.col, range.rightBottom.col, CACHE_SIZE - parts);
            if (parts >= CACHE_SIZE) {
                break;
            }
        }

    }

    private void loadSingleVisible(int page) {
        float scaledPreloadOffset = preloadOffset;
        float firstXOffset = -xOffset + scaledPreloadOffset;
        float lastXOffset = -xOffset - pdfView.getWidth() - scaledPreloadOffset;
        float firstYOffset = -yOffset + scaledPreloadOffset;
        float lastYOffset = -yOffset - pdfView.getHeight() - scaledPreloadOffset;

        RenderRange range = getRenderRange(page, firstXOffset, firstYOffset, lastXOffset, lastYOffset);
        Log.e("loadSingleVisible", pdfView.getCurrentPage() + " - " + range.page);
        /*
         单页模式下，每页的数据实际上绘制在同一个位置的
         由于绘制顺序的是先缩略后全图，导致如果切换页面的时候会先绘制第一页缩略，第二页缩略，然后是第一页的全图，实际上第二页的缩略图就被覆盖
         所以单页模式下需要先清除缓存
         */
        pdfView.cacheManager.clearCache();
        loadThumbnail(range.page);
        calculatePartSize(range.gridSize);
        loadPage(range.page, range.leftTop.row, range.rightBottom.row, range.leftTop.col, range.rightBottom.col, CACHE_SIZE);
    }

    private int loadPage(int page, int firstRow, int lastRow, int firstCol, int lastCol,
                         int nbOfPartsLoadable) {
        int loaded = 0;
        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = firstCol; col <= lastCol; col++) {
                if (loadCell(page, row, col, pageRelativePartWidth, pageRelativePartHeight)) {
                    loaded++;
                }
                if (loaded >= nbOfPartsLoadable) {
                    return loaded;
                }
            }
        }
        return loaded;
    }

    private boolean loadCell(int page, int row, int col, float pageRelativePartWidth, float pageRelativePartHeight) {

        float relX = pageRelativePartWidth * col;
        float relY = pageRelativePartHeight * row;
        float relWidth = pageRelativePartWidth;
        float relHeight = pageRelativePartHeight;

        float renderWidth = partRenderWidth;
        float renderHeight = partRenderHeight;
        if (relX + relWidth > 1) {
            relWidth = 1 - relX;
        }
        if (relY + relHeight > 1) {
            relHeight = 1 - relY;
        }
        renderWidth *= relWidth;
        renderHeight *= relHeight;
        RectF pageRelativeBounds = new RectF(relX, relY, relX + relWidth, relY + relHeight);

        if (renderWidth > 0 && renderHeight > 0) {
            if (!pdfView.cacheManager.upPartIfContained(page, pageRelativeBounds, cacheOrder)) {
                pdfView.renderingHandler.addRenderingTask(page, renderWidth, renderHeight,
                        pageRelativeBounds, false, cacheOrder, pdfView.isBestQuality(),
                        pdfView.isAnnotationRendering());
            }

            cacheOrder++;
            return true;
        }
        return false;
    }

    private void loadThumbnail(int page) {
        SizeF pageSize = pdfView.pdfFile.getPageSize(page);
        float thumbnailWidth = pageSize.getWidth() * Constants.THUMBNAIL_RATIO;
        float thumbnailHeight = pageSize.getHeight() * Constants.THUMBNAIL_RATIO;
        if (pdfView.isThumbnailSplit()) {
            for (RectF rectF: splitRectfList) {
                thumbnailWidth = pageSize.getWidth() * Constants.THUMBNAIL_RATIO / splitRectfList.size();
                thumbnailHeight = pageSize.getHeight() * Constants.THUMBNAIL_RATIO / splitRectfList.size();
                Log.e("loadThumbnail", page + " - " + rectF);
                Log.e("loadThumbnail", !pdfView.cacheManager.containsThumbnail(page, rectF) + "");
                if (!pdfView.cacheManager.containsThumbnail(page, rectF)) {
                    pdfView.renderingHandler.addRenderingTask(page,
                            thumbnailWidth, thumbnailHeight, rectF,
                            true, page, false, pdfView.isAnnotationRendering());
                }
            }
        }
        else {
            if (!pdfView.cacheManager.containsThumbnail(page, thumbnailRect)) {
                pdfView.renderingHandler.addRenderingTask(page,
                        thumbnailWidth, thumbnailHeight, thumbnailRect,
                        true, 0, pdfView.isBestQuality(), pdfView.isAnnotationRendering());
            }
        }
    }

    // 递归划分方法
    public void divideRectangle(RectF rectF, int currentDepth, int maxDepth,  List<RectF> splitRectfList) {
        if (currentDepth >= maxDepth) {
            splitRectfList.add(rectF);
            return;
        }
        float x1 = rectF.left;
        float y1 = rectF.top;
        float x2 = rectF.right;
        float y2 = rectF.bottom;
        // 水平划分
        if (currentDepth % 2 == 0) {
            float midX = (x1 + x2) / 2;
            divideRectangle(new RectF(x1, y1, midX, y2), currentDepth + 1, maxDepth, splitRectfList);
            divideRectangle(new RectF(midX, y1, x2, y2), currentDepth + 1, maxDepth, splitRectfList);
        }
        // 垂直划分
        else {
            float midY = (y1 + y2) / 2;
            divideRectangle(new RectF(x1, y1, x2, midY), currentDepth + 1, maxDepth, splitRectfList);
            divideRectangle(new RectF(x1, midY, x2, y2), currentDepth + 1, maxDepth, splitRectfList);
        }
    }

    void loadPages() {
        cacheOrder = 1;
        xOffset = -MathUtils.max(pdfView.getCurrentXOffset(), 0);
        yOffset = -MathUtils.max(pdfView.getCurrentYOffset(), 0);

        loadVisible();
    }

    void loadSinglePage(int page) {
        cacheOrder = 1;
        xOffset = -MathUtils.max(pdfView.getCurrentXOffset(), 0);
        yOffset = -MathUtils.max(pdfView.getCurrentYOffset(), 0);

        loadSingleVisible(page);
    }
}
