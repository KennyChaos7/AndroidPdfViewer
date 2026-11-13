package com.github.barteksc.pdfviewer.search;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

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
    private int selectedPosition = RecyclerView.NO_POSITION;

    public DefaultSearchResultAdapter(Context context, boolean isJustShowPage) {
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
        return new DefaultSearchViewHolder(viewGroup);
    }

    @Override
    public void onBindViewHolder(@NonNull DefaultSearchViewHolder defaultSearchViewHolder, int i) {
        final DefaultSearchViewHolder finalDefaultSearchViewHolder = defaultSearchViewHolder;
        final int position = i;
        final PdfDocument.Text text = dataList.get(position);
        boolean pageSummary = itemType == TYPE_PAGE;
        defaultSearchViewHolder.bind(text, position, pageSummary,
                pageSummary ? numInPage.get(text.getDestPageIdx()) : 0,
                position == selectedPosition);
        finalDefaultSearchViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedPosition != position) {
                    setSelectedPosition(position);
                }
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

    private void setSelectedPosition(int position) {
        if (selectedPosition == position) return;
        int previous = selectedPosition;
        selectedPosition = position;
        if (previous != RecyclerView.NO_POSITION) notifyItemChanged(previous);
        if (position != RecyclerView.NO_POSITION) notifyItemChanged(position);
    }

    @Override
    public void addDataList(ArrayList<PdfDocument.Text> newList) {
        selectedPosition = RecyclerView.NO_POSITION;
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
