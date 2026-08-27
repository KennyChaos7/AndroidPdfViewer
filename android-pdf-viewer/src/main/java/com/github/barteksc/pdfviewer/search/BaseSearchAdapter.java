package com.github.barteksc.pdfviewer.search;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.shockwave.pdfium.PdfDocument;

import java.util.ArrayList;

public abstract class BaseSearchAdapter<VH extends BaseSearchResultViewHolder> extends RecyclerView.Adapter<VH>  {
    public abstract void addDataList(ArrayList<PdfDocument.Text> dataList);
    public abstract ArrayList<PdfDocument.Text> getDataList();
    public abstract void setOnItemClickListener(OnItemClickListener listener);
    public interface OnItemClickListener {
        void onClick(int position, Integer pageIndex, PdfDocument.Text text,View itemView);
    }
}
