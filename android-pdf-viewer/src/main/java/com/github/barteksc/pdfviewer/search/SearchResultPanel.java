package com.github.barteksc.pdfviewer.search;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.barteksc.pdfviewer.R;
import com.github.barteksc.pdfviewer.util.Util;
import com.shockwave.pdfium.PdfDocument;

import java.util.ArrayList;

/** Presentation only; search results and navigation remain owned by PDFView. */
public class SearchResultPanel extends LinearLayout {
    private final RecyclerView recyclerView;
    private final TextView resultCount;
    private final TextView keyword;
    private final View emptyState;

    public SearchResultPanel(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(ContextCompat.getColor(context, R.color.pdf_search_surface));
        ViewCompat.setElevation(this, Util.getDP(context, 4));
        LayoutInflater.from(context).inflate(R.layout.pdf_search_result_panel, this, true);
        recyclerView = findViewById(R.id.pdf_search_results_list);
        resultCount = findViewById(R.id.pdf_search_count);
        keyword = findViewById(R.id.pdf_search_keyword);
        emptyState = findViewById(R.id.pdf_search_empty);
    }

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public void updateSummary(ArrayList<PdfDocument.Text> results) {
        int count = results.size();
        resultCount.setText(getResources().getQuantityString(
                R.plurals.pdf_search_result_count, count, count));
        String value = results.isEmpty() ? "" : results.get(0).getValue();
        keyword.setText(value);
        keyword.setVisibility(TextUtils.isEmpty(value) ? GONE : VISIBLE);
        recyclerView.setVisibility(results.isEmpty() ? GONE : VISIBLE);
        emptyState.setVisibility(results.isEmpty() ? VISIBLE : GONE);
    }
}
