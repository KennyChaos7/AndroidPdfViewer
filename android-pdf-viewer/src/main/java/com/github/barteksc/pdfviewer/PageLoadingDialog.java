package com.github.barteksc.pdfviewer;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * 页面加载条
 * 需要在 {@link PDFView#isShowLoadingDialog()} 中设置为 true后才会显示，默认显示
 */
public class PageLoadingDialog extends ProgressDialog {

    public PageLoadingDialog(Context context) {
        this(context, false);
    }

    public PageLoadingDialog(Context context, boolean outsideTouch) {
        super(context);
        setCanceledOnTouchOutside(outsideTouch);
    }

}
