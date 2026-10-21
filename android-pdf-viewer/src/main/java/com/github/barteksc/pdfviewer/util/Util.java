/*
 * Copyright (C) 2016 Bartosz Schiller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.barteksc.pdfviewer.util;

import android.content.Context;
import android.util.TypedValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import android.graphics.RectF;
import com.shockwave.pdfium.PdfDocument.StructElement;
import com.shockwave.pdfium.PdfDocument.StructContent;

public class Util {
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static int getDP(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    public static byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int n;
        while (-1 != (n = inputStream.read(buffer))) {
            os.write(buffer, 0, n);
        }
        return os.toByteArray();
    }

    public static ArrayList<StructContent> toStructContentList(int pageIndex, ArrayList<StructElement> elements) {

        ArrayList<StructContent> result = new ArrayList<>();
        if (elements == null || elements.isEmpty()) {
            return result;
        }

        // 同一父节点的孩子，按 JNI 返回顺序保存。
        Map<String, ArrayList<StructElement>> childrenByParentId = new HashMap<>();

        for (StructElement element : elements) {
            // 旧 JNI 的 /ID 可能为空，不能用来可靠地恢复节点关系。
            if (element.getId() == null) {
                throw new IllegalArgumentException(
                        "StructElement.id 为空，请先由 JNI 生成节点编号");
            }

            String parentId = element.getParentId();
            if (parentId == null) {
                continue;
            }

            ArrayList<StructElement> children = childrenByParentId.get(parentId);

            if (children == null) {
                children = new ArrayList<>();
                childrenByParentId.put(parentId, children);
            }
            children.add(element);
        }

        // 按段落在原列表中的顺序输出。
        for (StructElement element : elements) {
            if (element.getType() != StructElement.TYPE_PARAGRAPH) {
                continue;
            }

            ArrayList<StructElement> paragraphElements = new ArrayList<>();
            StringBuilder text = new StringBuilder();

            collectParagraphElements(
                    element,
                    childrenByParentId,
                    paragraphElements,
                    text,
                    true);

            RectF bounds = null;

            for (StructElement fragment : paragraphElements) {
                RectF fragmentBounds = fragment.getBounds();
                if (fragmentBounds == null) {
                    continue;
                }

                if (bounds == null) {
                    bounds = new RectF(fragmentBounds);
                } else {
                    bounds.left = Math.min(bounds.left, fragmentBounds.left);
                    bounds.right = Math.max(bounds.right, fragmentBounds.right);

                    // 保留当前 JNI 的 PDF 坐标：原点在左下角。
                    bounds.top = Math.max(bounds.top, fragmentBounds.top);
                    bounds.bottom = Math.min(bounds.bottom, fragmentBounds.bottom);
                }
            }

            result.add(new StructContent(
                    pageIndex,
                    paragraphElements,
                    text.toString(),
                    bounds));
        }

        return result;
    }

    private static void collectParagraphElements(
            StructElement node,
            Map<String, ArrayList<StructElement>> childrenByParentId,
            ArrayList<StructElement> paragraphElements,
            StringBuilder text,
            boolean collectText) {

        // 保存段落节点本身及其后代，供调用方查看结构和范围。
        paragraphElements.add(node);

        String nodeText = node.getText();
        if (collectText && nodeText != null) {
            text.append(nodeText);
        }

        ArrayList<StructElement> children = childrenByParentId.get(node.getId());

        if (children == null) {
            return;
        }

        // 非 null 的文字包括空字符串，都表示该节点提供了替代文字。
        boolean collectChildText = collectText && nodeText == null;

        for (StructElement child : children) {
            // 后代段落由外层单独生成 StructContent。
            if (child.getType() == StructElement.TYPE_PARAGRAPH) {
                continue;
            }

            collectParagraphElements(
                    child,
                    childrenByParentId,
                    paragraphElements,
                    text,
                    collectChildText);
        }
    }
}
