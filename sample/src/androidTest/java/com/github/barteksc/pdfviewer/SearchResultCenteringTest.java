package com.github.barteksc.pdfviewer;

import android.graphics.RectF;
import android.graphics.Bitmap;
import android.view.MotionEvent;
import android.os.SystemClock;
import android.support.v7.widget.RecyclerView;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.view.Gravity;

import com.github.barteksc.sample.PDFViewActivity_;
import com.github.barteksc.sample.R;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.search.SearchResultPanel;
import com.github.barteksc.pdfviewer.util.FitPolicy;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.util.SizeF;

import java.util.ArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Real PDF search -> result row click -> viewport position, on a physical device. */
public class SearchResultCenteringTest extends ActivityInstrumentationTestCase2<PDFViewActivity_> {
    private PDFView view;

    public SearchResultCenteringTest() {
        super(PDFViewActivity_.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        view = (PDFView) getActivity().findViewById(R.id.pdfView);
        getInstrumentation().waitForIdleSync();
    }

    public void testDefaultCentersWithOldFlagFalse() throws Exception {
        checkContinuous("sample6.pdf", true, Gravity.RIGHT, false);
    }

    public void testVerticalCentersWithLeftList() throws Exception {
        checkContinuous("sample2.pdf", true, Gravity.LEFT, true);
    }

    public void testHorizontalCentersWithRightList() throws Exception {
        checkContinuous("sample4.pdf", false, Gravity.RIGHT, false);
    }

    public void testDefaultZoomKeepsDocumentBounds() throws Exception {
        load("sample4.pdf", true, false, Gravity.RIGHT, false);
        ArrayList<PdfDocument.Text> results = findMatches();
        click(0);
        assertEquals(1f, view.getZoom(), 0f);
        assertCentered("sample4.pdf default zoom", results.get(0), true);
    }

    public void testSearchThenDoubleTapAndClick() throws Exception {
        load("sample6.pdf", true, false, Gravity.RIGHT, false, FitPolicy.BOTH);
        ArrayList<PdfDocument.Text> results = findMatches();
        ui(new Runnable() {
            @Override public void run() { view.reloadPage(); }
        });
        doubleTapReadingArea();
        long deadline = SystemClock.uptimeMillis() + 3000;
        while (Math.abs(view.getZoom() - view.getMidZoom()) > 0.001f
                && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(20);
        }
        assertEquals("Double tap must actually zoom", view.getMidZoom(), view.getZoom(), 0.001f);
        click(0);
        assertCentered("sample6.pdf after double tap", results.get(0), true);
        // Allow the asynchronous PDF tiles to appear for visual diagnosis.
        SystemClock.sleep(600);
        Bitmap screenshot = getInstrumentation().getUiAutomation().takeScreenshot();
        File file = new File(getInstrumentation().getTargetContext().getExternalFilesDir(null),
                "search-double-tap.png");
        try (FileOutputStream output = new FileOutputStream(file)) {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, output);
        }
        screenshot.recycle();
    }

    public void testSearchThenMaximumZoomAndClickIsVisible() throws Exception {
        load("sample6.pdf", true, false, Gravity.RIGHT, false, FitPolicy.BOTH);
        final ArrayList<PdfDocument.Text> matches = new ArrayList<>();
        ui(new Runnable() {
            @Override public void run() {
                matches.addAll(view.searchText("1", 2));
                view.reloadPage();
            }
        });
        assertFalse(matches.isEmpty());
        for (float target : new float[]{view.getMidZoom(), view.getMaxZoom()}) {
            doubleTapReadingArea();
            long deadline = SystemClock.uptimeMillis() + 3000;
            while (Math.abs(view.getZoom() - target) > 0.001f
                    && SystemClock.uptimeMillis() < deadline) {
                SystemClock.sleep(20);
            }
            assertEquals("Double tap must reach its zoom level", target, view.getZoom(), 0.001f);
        }
        click(0);
        ui(new Runnable() {
            @Override public void run() {
                PdfDocument.Text match = matches.get(0);
                int page = match.getDestPageIdx();
                SizeF size = view.pdfFile.getScaledPageSize(page, view.getZoom());
                RectF bounds = view.pdfFile.mapRectToDeviceWithOpen(page,
                        (int) view.pdfFile.getSecondaryPageOffset(page, view.getZoom()),
                        (int) view.pdfFile.getPageOffset(page, view.getZoom()),
                        (int) size.getWidth(), (int) size.getHeight(), match.getBounds());
                bounds.offset(view.getCurrentXOffset(), view.getCurrentYOffset());
                assertTrue("Magnified match must be left of the sidebar: " + bounds
                                + ", sidebar starts at " + sidebar().getLeft(),
                        Math.max(bounds.left, bounds.right) <= sidebar().getLeft() + 1f);
            }
        });
    }

    private void doubleTapReadingArea() throws Exception {
        final int[] location = new int[2];
        ui(new Runnable() {
            @Override public void run() { view.getLocationOnScreen(location); }
        });
        float x = location[0] + view.getWidth() / 4f;
        float y = location[1] + view.getHeight() / 2f;
        for (int tap = 0; tap < 2; tap++) {
            long downTime = SystemClock.uptimeMillis();
            MotionEvent down = MotionEvent.obtain(downTime, downTime,
                    MotionEvent.ACTION_DOWN, x, y, 0);
            MotionEvent up = MotionEvent.obtain(downTime, downTime + 20,
                    MotionEvent.ACTION_UP, x, y, 0);
            getInstrumentation().sendPointerSync(down);
            getInstrumentation().sendPointerSync(up);
            down.recycle();
            up.recycle();
            SystemClock.sleep(60);
        }
    }

    public void testSinglePageKeepsPageJump() throws Exception {
        load("sample6.pdf", true, true, Gravity.RIGHT, false);
        final ArrayList<PdfDocument.Text> results = findMatches();
        final int page = results.get(0).getDestPageIdx();
        final float[] offsets = new float[2];
        ui(new Runnable() {
            @Override public void run() {
                view.jumpTo(page);
                offsets[0] = view.getCurrentXOffset();
                offsets[1] = view.getCurrentYOffset();
                view.jumpTo(0);
            }
        });
        click(0);
        assertEquals(page, view.getCurrentPage());
        assertEquals(offsets[0], view.getCurrentXOffset(), 1f);
        assertEquals(offsets[1], view.getCurrentYOffset(), 1f);
    }

    private void checkContinuous(String asset, final boolean vertical, int gravity,
                                 boolean oldFlag) throws Exception {
        load(asset, vertical, false, gravity, oldFlag);
        ui(new Runnable() {
            @Override public void run() { view.zoomTo(2f); }
        });
        ArrayList<PdfDocument.Text> results = findMatches();
        for (int index = 0; index < Math.min(2, results.size()); index++) {
            final PdfDocument.Text match = results.get(index);
            click(index);
            assertEquals("Click must preserve zoom", 2f, view.getZoom(), 0f);
            assertCentered(asset, match, vertical);
        }
        // Repeating a click after an ordinary page jump must still locate the match.
        ui(new Runnable() {
            @Override public void run() { view.jumpTo(0); }
        });
        click(0);
        assertCentered(asset, results.get(0), vertical);
    }

    private void load(final String asset, final boolean vertical, final boolean single,
                      final int gravity, final boolean oldFlag) throws Exception {
        load(asset, vertical, single, gravity, oldFlag, FitPolicy.WIDTH);
    }

    private void load(final String asset, final boolean vertical, final boolean single,
                      final int gravity, final boolean oldFlag, final FitPolicy fit) throws Exception {
        final CountDownLatch loaded = new CountDownLatch(1);
        final Throwable[] error = new Throwable[1];
        ui(new Runnable() {
            @Override public void run() {
                view.fromAsset(asset).swipeHorizontal(!vertical).singlePageMode(single)
                        .pageFitPolicy(fit).enableAnnotationRendering(true)
                        .spacing(10).showLoadingDialog(false)
                        .showSearchResultList(true, gravity, oldFlag, false)
                        .onLoad(new OnLoadCompleteListener() {
                            @Override public void loadComplete(int count) { loaded.countDown(); }
                        }).onError(new OnErrorListener() {
                            @Override public void onError(Throwable cause) {
                                error[0] = cause;
                                loaded.countDown();
                            }
                        }).load();
            }
        });
        assertTrue("Load timed out: " + asset, loaded.await(45, TimeUnit.SECONDS));
        if (error[0] != null) throw new AssertionError("Load failed: " + asset, error[0]);
        getInstrumentation().waitForIdleSync();
    }

    private ArrayList<PdfDocument.Text> findMatches() throws Exception {
        // Search a real interior page, using bounded common Chinese/English/numeric queries.
        for (int candidate = 2; candidate < Math.min(12, view.getPageCount()); candidate++) {
            final int page = candidate;
            for (final String query : new String[]{"的", "一", "the", "a", "1"}) {
                final ArrayList<PdfDocument.Text> found = new ArrayList<>();
                ui(new Runnable() {
                    @Override public void run() {
                        found.addAll(view.searchText(query, page));
                    }
                });
                if (!found.isEmpty()) {
                    getInstrumentation().waitForIdleSync();
                    return found;
                }
            }
        }
        fail("No searchable match found in pages 3-12 of the asset");
        return null;
    }

    private SearchResultPanel sidebar() {
        for (int i = 0; i < view.getChildCount(); i++) {
            if (view.getChildAt(i) instanceof SearchResultPanel) {
                return (SearchResultPanel) view.getChildAt(i);
            }
        }
        throw new AssertionError("Search result sidebar missing");
    }

    private RecyclerView list() {
        return sidebar().getRecyclerView();
    }

    private void click(final int index) throws Exception {
        ui(new Runnable() {
            @Override public void run() { list().scrollToPosition(index); }
        });
        getInstrumentation().waitForIdleSync();
        // Wait for RecyclerView's next layout, not a guessed rendering duration.
        final long deadline = SystemClock.uptimeMillis() + 5000;
        final boolean[] clicked = {false};
        while (!clicked[0] && SystemClock.uptimeMillis() < deadline) {
            ui(new Runnable() {
                @Override public void run() {
                    RecyclerView.ViewHolder holder = list().findViewHolderForAdapterPosition(index);
                    if (holder != null) clicked[0] = holder.itemView.performClick();
                }
            });
            if (!clicked[0]) SystemClock.sleep(20);
        }
        assertTrue("Search result row must be clicked", clicked[0]);
        getInstrumentation().waitForIdleSync();
    }

    private void assertCentered(final String asset, final PdfDocument.Text match,
                                final boolean vertical) throws Exception {
        ui(new Runnable() {
            @Override public void run() {
                int page = match.getDestPageIdx();
                SizeF size = view.pdfFile.getScaledPageSize(page, view.getZoom());
                float primary = view.pdfFile.getPageOffset(page, view.getZoom());
                float secondary = view.pdfFile.getSecondaryPageOffset(page, view.getZoom());
                RectF bounds = view.pdfFile.mapRectToDeviceWithOpen(page,
                        (int) (vertical ? secondary : primary),
                        (int) (vertical ? primary : secondary),
                        (int) size.getWidth(), (int) size.getHeight(), match.getBounds());
                SearchResultPanel sidebar = sidebar();
                float readingLeft = sidebar.getLeft() == 0 ? sidebar.getRight() : 0;
                float readingRight = sidebar.getLeft() == 0 ? view.getWidth() : sidebar.getLeft();
                float readingCenterX = (readingLeft + readingRight) / 2f;
                float contentWidth = vertical ? view.pdfFile.getMaxPageWidth() * view.getZoom()
                        : view.pdfFile.getDocLen(view.getZoom());
                float contentHeight = vertical ? view.pdfFile.getDocLen(view.getZoom())
                        : view.pdfFile.getMaxPageHeight() * view.getZoom();
                float expectedX = readingLeft + constrained(
                        readingCenterX - bounds.centerX() - readingLeft,
                        contentWidth, readingRight - readingLeft);
                float expectedY = constrained(view.getHeight() / 2f - bounds.centerY(), contentHeight, view.getHeight());
                Log.i("SearchCenteringTest", asset + " page=" + page + " bounds=" + bounds
                        + " actual=" + view.getCurrentXOffset() + "," + view.getCurrentYOffset()
                        + " expected=" + expectedX + "," + expectedY);
                assertEquals(asset + " horizontal result center", expectedX, view.getCurrentXOffset(), 1.5f);
                assertEquals(asset + " vertical result center", expectedY, view.getCurrentYOffset(), 1.5f);
            }
        });
    }

    private float constrained(float requested, float content, float viewport) {
        return content < viewport ? (viewport - content) / 2f
                : Math.max(viewport - content, Math.min(0, requested));
    }

    private void ui(final Runnable action) throws Exception {
        final Throwable[] failure = new Throwable[1];
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override public void run() {
                try { action.run(); }
                catch (Throwable cause) { failure[0] = cause; }
            }
        });
        if (failure[0] != null) throw new AssertionError("UI action failed", failure[0]);
    }
}
