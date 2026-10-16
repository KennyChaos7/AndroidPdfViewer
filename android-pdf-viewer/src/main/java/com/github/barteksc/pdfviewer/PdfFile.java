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

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.github.barteksc.pdfviewer.exception.PageRenderingException;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.github.barteksc.pdfviewer.util.PageSizeCalculator;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfFrameListener;
import com.shockwave.pdfium.PdfiumCore;
import com.shockwave.pdfium.util.Size;
import com.shockwave.pdfium.util.SizeF;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

class PdfFile {

    private static final Object lock = new Object();
    private PdfDocument pdfDocument;
    private PdfiumCore pdfiumCore;
    private int pagesCount = 0;
    /** Original page sizes */
    private List<Size> originalPageSizes = new ArrayList<>();
    /** Scaled page sizes */
    private List<SizeF> pageSizes = new ArrayList<>();
    private static final int MAX_OPEN_PAGES = 4;
    /** 最近访问页及其打开状态（含失败记录）；显示位图无需持有原生页面。 */
    private final LinkedHashMap<Integer, Boolean> openedPages = new LinkedHashMap<>(4, 0.75f, true);
    /** Page with maximum width */
    private Size originalMaxWidthPageSize = new Size(0, 0);
    /** Page with maximum height */
    private Size originalMaxHeightPageSize = new Size(0, 0);
    /** Scaled page with maximum height */
    private SizeF maxHeightPageSize = new SizeF(0, 0);
    /** Scaled page with maximum width */
    private SizeF maxWidthPageSize = new SizeF(0, 0);
    /** True if scrolling is vertical, else it's horizontal */
    private boolean isVertical;
    // 单页模式
    private boolean isSinglePageMode;
    /** Fixed spacing between pages in pixels */
    private int spacingPx;
    /** Calculate spacing automatically so each page fits on it's own in the center of the view */
    private boolean autoSpacing;
    /** Calculated offsets for pages */
    private List<Float> pageOffsets = new ArrayList<>();
    /** Calculated auto spacing for pages */
    private List<Float> pageSpacing = new ArrayList<>();
    /** Calculated document length (width or height, depending on swipe mode) */
    private float documentLength = 0;
    private final FitPolicy pageFitPolicy;
    /**
     * True if every page should fit separately according to the FitPolicy,
     * else the largest page fits and other pages scale relatively
     */
    private final boolean fitEachPage;
    /**
     * The pages the user want to display in order
     * (ex: 0, 2, 2, 8, 8, 1, 1, 1)
     */
    private int[] originalUserPages;

    PdfFile(PdfiumCore pdfiumCore, PdfDocument pdfDocument, FitPolicy pageFitPolicy, Size viewSize, int[] originalUserPages,
            boolean isVertical, boolean isSinglePageMode, int spacing, boolean autoSpacing, boolean fitEachPage) {
        this.pdfiumCore = pdfiumCore;
        this.pdfDocument = pdfDocument;
        this.pageFitPolicy = pageFitPolicy;
        this.originalUserPages = originalUserPages;
        this.isVertical = isVertical;
        this.isSinglePageMode = isSinglePageMode;
        this.spacingPx = spacing;
        this.autoSpacing = autoSpacing;
        this.fitEachPage = fitEachPage;
        setup(viewSize);
    }

    private void setup(Size viewSize) {
        if (originalUserPages != null) {
            pagesCount = originalUserPages.length;
        } else {
            pagesCount = pdfiumCore.getPageCount(pdfDocument);
        }

        for (int i = 0; i < pagesCount; i++) {
            Size pageSize = pdfiumCore.getPageSize(pdfDocument, documentPage(i));
            if (pageSize.getWidth() > originalMaxWidthPageSize.getWidth()) {
                originalMaxWidthPageSize = pageSize;
            }
            if (pageSize.getHeight() > originalMaxHeightPageSize.getHeight()) {
                originalMaxHeightPageSize = pageSize;
            }
            originalPageSizes.add(pageSize);
        }

        recalculatePageSizes(viewSize);
    }

    /**
     * Call after view size change to recalculate page sizes, offsets and document length
     *
     * @param viewSize new size of changed view
     */
    public void recalculatePageSizes(Size viewSize) {
        pageSizes.clear();
        PageSizeCalculator calculator = new PageSizeCalculator(pageFitPolicy, originalMaxWidthPageSize,
                originalMaxHeightPageSize, viewSize, fitEachPage);
        maxWidthPageSize = calculator.getOptimalMaxWidthPageSize();
        maxHeightPageSize = calculator.getOptimalMaxHeightPageSize();

        for (Size size : originalPageSizes) {
            pageSizes.add(calculator.calculate(size));
        }
        if (autoSpacing) {
            prepareAutoSpacing(viewSize);
        }
        prepareDocLen();
        preparePagesOffset();
    }

    public int getPagesCount() {
        return pagesCount;
    }

    public SizeF getPageSize(int pageIndex) {
        int docPage = documentPage(pageIndex);
        if (docPage < 0) {
            return new SizeF(0, 0);
        }
        return pageSizes.get(pageIndex);
    }

    public SizeF getScaledPageSize(int pageIndex, float zoom) {
        SizeF size = getPageSize(pageIndex);
        return new SizeF(size.getWidth() * zoom, size.getHeight() * zoom);
    }

    /**
     * get page size with biggest dimension (width in vertical mode and height in horizontal mode)
     *
     * @return size of page
     */
    public SizeF getMaxPageSize() {
        return isVertical ? maxWidthPageSize : maxHeightPageSize;
    }

    public float getMaxPageWidth() {
        return getMaxPageSize().getWidth();
    }

    public float getMaxPageHeight() {
        return getMaxPageSize().getHeight();
    }

    private void prepareAutoSpacing(Size viewSize) {
        pageSpacing.clear();
        for (int i = 0; i < getPagesCount(); i++) {
            SizeF pageSize = pageSizes.get(i);
            float spacing = Math.max(0, isVertical ? viewSize.getHeight() - pageSize.getHeight() :
                    viewSize.getWidth() - pageSize.getWidth());
            if (i < getPagesCount() - 1) {
                spacing += spacingPx;
            }
            pageSpacing.add(spacing);
        }
    }

    private void prepareDocLen() {
        float length = 0;
        if (isSinglePageMode) {
            SizeF pageSize = pageSizes.get(0);
            length += isVertical ? pageSize.getHeight() : pageSize.getWidth();
            if (autoSpacing) {
                length += pageSpacing.get(0);
            } else if (0 < getPagesCount() - 1) {
                length += spacingPx;
            }
        }
        else {
            for (int i = 0; i < getPagesCount(); i++) {
                SizeF pageSize = pageSizes.get(i);
                length += isVertical ? pageSize.getHeight() : pageSize.getWidth();
                if (autoSpacing) {
                    length += pageSpacing.get(i);
                } else if (i < getPagesCount() - 1) {
                    length += spacingPx;
                }
            }
        }
        documentLength = length;
    }

    private void preparePagesOffset() {
        pageOffsets.clear();
        float offset = 0;
        if (isSinglePageMode) {
            if (autoSpacing) {
                offset += pageSpacing.get(0) / 2f;
                offset -= spacingPx / 2f;

            }
            for (int i = 0; i < getPagesCount(); i++) {
                pageOffsets.add(offset);
            }
        }
        else {
            for (int i = 0; i < getPagesCount(); i++) {
                SizeF pageSize = pageSizes.get(i);
                float size = isVertical ? pageSize.getHeight() : pageSize.getWidth();
                if (autoSpacing) {
                    offset += pageSpacing.get(i) / 2f;
                    if (i == 0) {
                        offset -= spacingPx / 2f;
                    } else if (i == getPagesCount() - 1) {
                        offset += spacingPx / 2f;
                    }
                    pageOffsets.add(offset);
                    offset += size + pageSpacing.get(i) / 2f;
                } else {
                    pageOffsets.add(offset);
                    offset += size + spacingPx;
                }
            }
        }
    }

    public float getDocLen(float zoom) {
        return documentLength * zoom;
    }

    /**
     * Get the page's height if swiping vertical, or width if swiping horizontal.
     */
    public float getPageLength(int pageIndex, float zoom) {
        SizeF size = getPageSize(pageIndex);
        return (isVertical ? size.getHeight() : size.getWidth()) * zoom;
    }

    public float getPageSpacing(int pageIndex, float zoom) {
        float spacing = autoSpacing ? pageSpacing.get(pageIndex) : spacingPx;
        return spacing * zoom;
    }

    /** Get primary page offset, that is Y for vertical scroll and X for horizontal scroll */
    public float getPageOffset(int pageIndex, float zoom) {
        int docPage = documentPage(pageIndex);
        if (docPage < 0) {
            return 0;
        }
        return pageOffsets.get(pageIndex) * zoom;
    }

    /** Get secondary page offset, that is X for vertical scroll and Y for horizontal scroll */
    public float getSecondaryPageOffset(int pageIndex, float zoom) {
        SizeF pageSize = getPageSize(pageIndex);
        if (isVertical) {
            float maxWidth = getMaxPageWidth();
            return zoom * (maxWidth - pageSize.getWidth()) / 2; //x
        } else {
            float maxHeight = getMaxPageHeight();
            return zoom * (maxHeight - pageSize.getHeight()) / 2; //y
        }
    }

    public int getPageAtOffset(float offset, float zoom) {
        int currentPage = 0;
        for (int i = 0; i < getPagesCount(); i++) {
            float off = pageOffsets.get(i) * zoom - getPageSpacing(i, zoom) / 2f;
            if (off >= offset) {
                break;
            }
            currentPage++;
//            if (isSinglePageMode) break;
        }
        return --currentPage >= 0 ? currentPage : 0;
    }

    public boolean openPage(int pageIndex) throws PageRenderingException {
        synchronized (lock) {
            int docPage = documentPage(pageIndex);
            if (pdfDocument == null || docPage < 0) {
                return false;
            }
            if (openedPages.get(docPage) == null) {
                if (openedPages.size() == MAX_OPEN_PAGES) {
                    Integer oldest = openedPages.keySet().iterator().next();
                    pdfiumCore.closePage(pdfDocument, oldest);
                    openedPages.remove(oldest);
                }
                try {
                    pdfiumCore.openPage(pdfDocument, docPage);
                    openedPages.put(docPage, true);
                    return true;
                } catch (Exception e) {
                    openedPages.put(docPage, false);
                    throw new PageRenderingException(pageIndex, e);
                }
            }
            return false;
        }
    }

    public boolean pageHasError(int pageIndex) {
        synchronized (lock) {
            return Boolean.FALSE.equals(openedPages.get(documentPage(pageIndex)));
        }
    }

    private boolean preparePage(int pageIndex) {
        if (pdfDocument == null || documentPage(pageIndex) < 0) {
            return false;
        }
        try {
            openPage(pageIndex);
            return !pageHasError(pageIndex);
        } catch (PageRenderingException e) {
            Log.e("PdfFile", "Cannot open page " + pageIndex, e);
            return false;
        }
    }

    public void renderPageBitmap(Bitmap bitmap, int pageIndex, Rect bounds, boolean annotationRendering, PdfFrameListener frameListener) {
        synchronized (lock) {
            // 页面重开、原生调用和淘汰须在同一锁内完成，分块渲染期间也遵守此约束。
            if (preparePage(pageIndex)) {
                pdfiumCore.renderPageBitmap(pdfDocument, bitmap, documentPage(pageIndex),
                        bounds.left, bounds.top, bounds.width(), bounds.height(), annotationRendering, frameListener);
            }
        }
    }

    public PdfDocument.Meta getMetaData() {
        synchronized (lock) {
            return pdfDocument == null ? null : pdfiumCore.getDocumentMeta(pdfDocument);
        }
    }

    public List<PdfDocument.Bookmark> getBookmarks() {
        synchronized (lock) {
            return pdfDocument == null ? new ArrayList<PdfDocument.Bookmark>()
                    : pdfiumCore.getTableOfContents(pdfDocument);
        }
    }

    public List<PdfDocument.Link> getPageLinks(int pageIndex) {
        synchronized (lock) {
            return preparePage(pageIndex) ? pdfiumCore.getPageLinks(pdfDocument, documentPage(pageIndex))
                    : new ArrayList<PdfDocument.Link>();
        }
    }

    public RectF mapRectToDevice(int pageIndex, int startX, int startY, int sizeX, int sizeY,
                                 RectF rect) {
        synchronized (lock) {
            return preparePage(pageIndex) ? pdfiumCore.mapRectToDevice(pdfDocument,
                    documentPage(pageIndex), startX, startY, sizeX, sizeY, 0, rect) : new RectF();
        }
    }

    public RectF mapRectToDeviceWithOpen(int pageIndex, int startX, int startY, int sizeX, int sizeY, RectF rect) {
        return mapRectToDevice(pageIndex, startX, startY, sizeX, sizeY, rect);
    }

    public void dispose() {
        synchronized (lock) {
            if (pdfiumCore != null && pdfDocument != null) {
                pdfiumCore.closeDocument(pdfDocument);
            }
            openedPages.clear();
            pdfDocument = null;
            originalUserPages = null;
        }
    }

    /**
     * Given the UserPage number, this method restrict it
     * to be sure it's an existing page. It takes care of
     * using the user defined pages if any.
     *
     * @param userPage A page number.
     * @return A restricted valid page number (example : -2 => 0)
     */
    public int determineValidPageNumberFrom(int userPage) {
        if (userPage <= 0) {
            return 0;
        }
        if (originalUserPages != null) {
            if (userPage >= originalUserPages.length) {
                return originalUserPages.length - 1;
            }
        } else {
            if (userPage >= getPagesCount()) {
                return getPagesCount() - 1;
            }
        }
        return userPage;
    }

    public int documentPage(int userPage) {
        int documentPage = userPage;
        if (originalUserPages != null) {
            if (userPage < 0 || userPage >= originalUserPages.length) {
                return -1;
            } else {
                documentPage = originalUserPages[userPage];
            }
        }

        if (documentPage < 0 || userPage >= getPagesCount()) {
            return -1;
        }

        return documentPage;
    }

    public ArrayList<PdfDocument.Text> searchText(String txt, int pageIndex) {
        synchronized (lock) {
            return preparePage(pageIndex) ? pdfiumCore.searchText(pdfDocument, txt, documentPage(pageIndex))
                    : new ArrayList<PdfDocument.Text>();
        }
    }

    public ArrayList<PdfDocument.Text> searchText(String txt) {
        synchronized (lock) {
            return pdfDocument == null ? new ArrayList<PdfDocument.Text>() : pdfiumCore.searchText(pdfDocument, txt);
        }
    }

    public void insertImageToFile(Bitmap bitmap, int fd) {
        synchronized (lock) {
            if (pdfDocument != null) pdfiumCore.insertImageToFile(pdfDocument, bitmap, fd);
        }
    }
}
