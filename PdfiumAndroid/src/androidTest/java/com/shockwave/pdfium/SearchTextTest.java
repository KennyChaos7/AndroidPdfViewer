package com.shockwave.pdfium;

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

    private void checkSearch(boolean singlePage) throws Exception {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        try (InputStream input = getInstrumentation().getContext().getAssets().open("search-text.pdf")) {
            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                data.write(buffer, 0, count);
            }
        }
        PdfiumCore core = new PdfiumCore(getInstrumentation().getTargetContext());
        PdfDocument document = core.newDocument(data.toByteArray());
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