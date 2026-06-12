package com.github.barteksc.pdfviewer;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.shockwave.pdfium.PdfDocument;

import java.util.ArrayList;

public class PdfSearchResultAdapter extends RecyclerView.Adapter<PdfSearchResultAdapter.ViewHolder> {
    private final int textViewId = 0x2192929;
    private ArrayList<Pair<Integer, Integer>> dataList = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(setupLayout(viewGroup.getContext()));
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, final int i) {
        if (dataList.size() > 0) {
            viewHolder.tvPageIndex.setText(String.valueOf("On page "+ dataList.get(i).first + 1 + ": had (" + dataList.get(i).second) + ") result.");
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onClick(i, dataList.get(i).first, viewHolder.itemView);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public void addData(ArrayList<PdfDocument.Text> newList) {
        Log.i("adapter", "list size " + newList.size());
        this.dataList.clear();
        for(int i = 0; i < newList.size(); i++) {
            PdfDocument.Text text = newList.get(i);
            if (this.dataList.size() > 0 && text != null) {
                boolean isAdded = false;
                for(int index = 0; index < this.dataList.size(); index ++) {
                    Pair<Integer, Integer> pair = this.dataList.get(index);
                    if (pair != null && pair.first == text.getDestPageIdx()) {
                        Pair<Integer, Integer> newPair = new Pair<>(pair.first, (pair.second + 1));
                        this.dataList.set(index, newPair);
                        isAdded = true;
                        break;
                    }
                    else {
                        isAdded = false;
                    }
                }
                if (!isAdded) {
                    Pair<Integer, Integer> pair = new Pair<Integer,Integer>(text.getDestPageIdx(), 1);
                    this.dataList.add(pair);
                }
            }
            else if (text != null) {
                Pair<Integer, Integer> pair = new Pair<Integer,Integer>(text.getDestPageIdx(), 1);
                this.dataList.add(pair);
            }
        }
        notifyDataSetChanged();
    }

    private View setupLayout(Context context) {
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
        textView.setTag(textViewId);
        linearLayout.addView(textView, layoutParams);
        return linearLayout;
    }

     public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
     }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPageIndex;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPageIndex = itemView.findViewWithTag(textViewId);
        }
    }
    
    public interface OnItemClickListener {
        void onClick(int position, Integer pageIndex, View itemView);
    }
}
