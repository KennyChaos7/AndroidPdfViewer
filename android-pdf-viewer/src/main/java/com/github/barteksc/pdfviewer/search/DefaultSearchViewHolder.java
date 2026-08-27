package com.github.barteksc.pdfviewer.search;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DefaultSearchViewHolder extends BaseSearchResultViewHolder{
    private TextView tvPage;

    public DefaultSearchViewHolder(Context context) {
        super(createItemView(context));
        tvPage = itemView.findViewWithTag(defaultTextViewId);
    }

    public TextView getTvPage() {
        return tvPage;
    }

    private static View createItemView(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(Color.WHITE);
        RecyclerView.LayoutParams parentLayoutParams = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        parentLayoutParams.setMargins(0, 10, 0, 10);
        linearLayout.setLayoutParams(parentLayoutParams);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.START;
        layoutParams.setMargins(10, 50, 10, 50);
        TextView textView = new TextView(linearLayout.getContext());
        textView.setTextColor(Color.BLACK);
        textView.setTextSize(14);
        textView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.START);
        textView.setTag(defaultTextViewId);
        linearLayout.addView(textView, layoutParams);
        return linearLayout;
    }
}
