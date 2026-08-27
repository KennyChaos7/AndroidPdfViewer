package com.github.barteksc.pdfviewer.search;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.github.barteksc.pdfviewer.R;
import com.shockwave.pdfium.PdfDocument;

import java.util.ArrayList;
import java.util.HashMap;

public class DefaultSearchResultAdapter extends BaseSearchAdapter<DefaultSearchViewHolder> {
    private final static int TYPE_PAGE = 0;
    private final static int TYPE_DETAILS = 1;
    private final static int TYPE_EMPTY = -1;
    private int itemType = TYPE_EMPTY;
    private ArrayList<PdfDocument.Text> dataList = new ArrayList<>();
    private HashMap<Integer, Integer> numInPage = new HashMap<>();
    private OnItemClickListener onItemClickListener;
    private String formatHeadText = "";
    private String formatContentText = "";

    public DefaultSearchResultAdapter(Context context, boolean isJustShowPage) {
        this.formatHeadText = context.getString(R.string.pdf_search_result_on_one_page_head);
        this.formatContentText = context.getString(R.string.pdf_search_result_on_one_page_content);
        if (isJustShowPage) {
            itemType = TYPE_PAGE;
        }
        else {
            itemType = TYPE_DETAILS;
        }
    }

    @NonNull
    @Override
    public DefaultSearchViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new DefaultSearchViewHolder(viewGroup.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull DefaultSearchViewHolder defaultSearchViewHolder, int i) {
        final DefaultSearchViewHolder finalDefaultSearchViewHolder = defaultSearchViewHolder;
        final int position = i;
        final PdfDocument.Text text = (PdfDocument.Text) dataList.get(position);
        if (itemType == TYPE_PAGE) {
            finalDefaultSearchViewHolder.getTvPage().setText(String.format(formatHeadText, text.getDestPageIdx() + 1, numInPage.get(text.getDestPageIdx())));
        }
        else if (itemType == TYPE_DETAILS) {
            finalDefaultSearchViewHolder.getTvPage().setText(String.format(formatContentText, text.getDestPageIdx() + 1, text.getBounds().left, text.getBounds().top));
        }
        finalDefaultSearchViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onClick(position, text.getDestPageIdx(), text, finalDefaultSearchViewHolder.itemView);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    @Override
    public void addDataList(ArrayList<PdfDocument.Text> newList) {
        this.numInPage.clear();
        this.dataList.clear();
        if (itemType == TYPE_PAGE) {
            for (int i = 0; i < newList.size(); i++) {
                PdfDocument.Text text = newList.get(i);
                if (i == 0) {
                    this.numInPage.put(text.getDestPageIdx(), 1);
                    this.dataList.add(text);
                }
                else {
                    if (this.numInPage.get(text.getDestPageIdx()) != null) {
                        int lastNum = this.numInPage.get(text.getDestPageIdx());
                        this.numInPage.put(text.getDestPageIdx(), lastNum + 1);
                    }
                    else {
                        this.numInPage.put(text.getDestPageIdx(), 1);
                        this.dataList.add(text);
                    }
                }
            }
        }
        else if (itemType == TYPE_DETAILS) {
            this.dataList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @Override
    public ArrayList<PdfDocument.Text> getDataList() {
        return this.dataList;
    }

    @Override
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }


}
