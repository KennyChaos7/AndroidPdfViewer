package com.github.barteksc.pdfviewer.search;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

public abstract class BaseSearchResultViewHolder extends RecyclerView.ViewHolder {
    public final static int defaultTextViewId = 0x2192929;
    public BaseSearchResultViewHolder(@NonNull View itemView) {
        super(itemView);
    }

}
