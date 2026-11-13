package com.github.barteksc.pdfviewer.search;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.barteksc.pdfviewer.R;
import com.shockwave.pdfium.PdfDocument;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultSearchViewHolder extends BaseSearchResultViewHolder {
    private final TextView tvPage;
    private final TextView pageBadge;
    private final TextView positionLabel;
    private final TextView chevron;
    private final View selectionAccent;

    public DefaultSearchViewHolder(Context context) {
        this(LayoutInflater.from(context).inflate(R.layout.pdf_search_result_item, null));
    }

    DefaultSearchViewHolder(ViewGroup parent) {
        this(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.pdf_search_result_item, parent, false));
    }

    private DefaultSearchViewHolder(View view) {
        super(view);
        tvPage = view.findViewById(R.id.pdf_search_context);
        tvPage.setTag(defaultTextViewId);
        pageBadge = view.findViewById(R.id.pdf_search_page);
        positionLabel = view.findViewById(R.id.pdf_search_position);
        chevron = view.findViewById(R.id.pdf_search_chevron);
        selectionAccent = view.findViewById(R.id.pdf_search_selection);
    }

    public TextView getTvPage() {
        return tvPage;
    }

    void bind(PdfDocument.Text text, int position, boolean pageSummary,
              int pageMatchCount, boolean selected) {
        Context context = itemView.getContext();
        pageBadge.setText(context.getString(R.string.pdf_search_page_label,
                text.getDestPageIdx() + 1));
        positionLabel.setText(context.getString(R.string.pdf_search_position_label, position + 1));
        tvPage.setText(pageSummary
                ? context.getResources().getQuantityString(
                        R.plurals.pdf_search_page_match_count, pageMatchCount, pageMatchCount)
                : highlightedContext(text));
        itemView.setActivated(selected);
        pageBadge.setActivated(selected);
        positionLabel.setActivated(selected);
        chevron.setActivated(selected);
        selectionAccent.setVisibility(selected ? View.VISIBLE : View.GONE);
    }

    private CharSequence highlightedContext(PdfDocument.Text text) {
        String context = text.getContextText();
        String keyword = text.getValue();
        if (TextUtils.isEmpty(context)) {
            context = TextUtils.isEmpty(keyword) ? "" : keyword;
        }
        SpannableString styled = new SpannableString(context);
        if (!TextUtils.isEmpty(keyword)) {
            Matcher matches = Pattern.compile(Pattern.quote(keyword),
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(context);
            int color = ContextCompat.getColor(itemView.getContext(), R.color.pdf_search_highlight);
            while (matches.find()) {
                styled.setSpan(new BackgroundColorSpan(color), matches.start(), matches.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return styled;
    }
}
