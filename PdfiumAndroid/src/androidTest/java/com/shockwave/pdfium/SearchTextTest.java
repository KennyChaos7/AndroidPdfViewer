package com.shockwave.pdfium;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.test.InstrumentationTestCase;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class SearchTextTest extends InstrumentationTestCase {
    public void testDocumentSearchReturnsRectF() throws Exception {
        checkSearch(false);
    }

    public void testPageSearchReturnsRectF() throws Exception {
        checkSearch(true);
    }

    public void testDocumentSearchClearsOnlyPreviousSearchHighlights() throws Exception {
        checkPreviousSearchHighlights(false);
    }

    public void testPageSearchClearsOnlyPreviousSearchHighlights() throws Exception {
        checkPreviousSearchHighlights(true);
    }

    private void checkPreviousSearchHighlights(boolean singlePage) throws Exception {
        PdfiumCore core = new PdfiumCore(getInstrumentation().getTargetContext());
        PdfDocument document = core.newDocument(readAsset("search-text.pdf"));
        try {
            core.openPage(document, 0);
            Bitmap original = render(core, document);
            ArrayList<PdfDocument.Text> first = singlePage
                    ? core.searchText(document, "needle", 0)
                    : core.searchText(document, "needle");
            assertEquals(1, first.size());
            Bitmap highlighted = render(core, document);
            assertFalse("First search must visibly highlight its match", original.sameAs(highlighted));
            ArrayList<PdfDocument.Text> second = singlePage
                    ? core.searchText(document, "中文", 0)
                    : core.searchText(document, "中文");
            assertTrue("Chinese query has no match in the fixture", second.isEmpty());
            Bitmap cleared = render(core, document);
            assertTrue("Previous search highlight must clear while original annotations remain",
                    original.sameAs(cleared));
            original.recycle();
            highlighted.recycle();
            cleared.recycle();
        } finally {
            core.closeDocument(document);
        }
    }

    private Bitmap render(PdfiumCore core, PdfDocument document) {
        Bitmap bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        core.renderPageBitmap(document, bitmap, 0, 0, 0, 400, 400, true, null);
        return bitmap;
    }

    private byte[] readAsset(String name) throws Exception {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        try (InputStream input = getInstrumentation().getContext().getAssets().open(name)) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                data.write(buffer, 0, count);
            }
        }
        return data.toByteArray();
    }

    private void checkSearch(boolean singlePage) throws Exception {
        PdfiumCore core = new PdfiumCore(getInstrumentation().getTargetContext());
        PdfDocument document = core.newDocument(readAsset("search-text.pdf"));
        try {
            ArrayList<PdfDocument.Text> results;
            if (singlePage) {
                core.openPage(document, 0);
                results = core.searchText(document, "needle", 0);
            } else {
                results = core.searchText(document, "needle");
            }
            assertEquals(1, results.size());
            PdfDocument.Text match = results.get(0);
            assertEquals(0, match.getDestPageIdx());
            assertEquals("needle", match.getValue());
            RectF bounds = match.getBounds();
            assertNotNull(bounds);
            assertTrue(bounds.right > bounds.left);
            assertTrue(bounds.top != bounds.bottom);
        } finally {
            core.closeDocument(document);
        }
    }
}