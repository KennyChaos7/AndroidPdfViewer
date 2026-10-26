package com.shockwave.pdfium;

import android.graphics.RectF;
import android.os.ParcelFileDescriptor;
import android.support.v4.util.ArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PdfDocument {

    public static class Meta {
        String title;
        String author;
        String subject;
        String keywords;
        String creator;
        String producer;
        String creationDate;
        String modDate;

        public String getTitle() {
            return title;
        }

        public String getAuthor() {
            return author;
        }

        public String getSubject() {
            return subject;
        }

        public String getKeywords() {
            return keywords;
        }

        public String getCreator() {
            return creator;
        }

        public String getProducer() {
            return producer;
        }

        public String getCreationDate() {
            return creationDate;
        }

        public String getModDate() {
            return modDate;
        }
    }

    public static class Bookmark {
        private List<Bookmark> children = new ArrayList<>();
        String title;
        long pageIdx;
        long mNativePtr;

        public List<Bookmark> getChildren() {
            return children;
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }

        public String getTitle() {
            return title;
        }

        public long getPageIdx() {
            return pageIdx;
        }
    }

    public static class Link {
        private RectF bounds;
        private Integer destPageIdx;
        private String uri;

        public Link(RectF bounds, Integer destPageIdx, String uri) {
            this.bounds = bounds;
            this.destPageIdx = destPageIdx;
            this.uri = uri;
        }

        public Integer getDestPageIdx() {
            return destPageIdx;
        }

        public String getUri() {
            return uri;
        }

        public RectF getBounds() {
            return bounds;
        }
    }

    public static class Text {
        private RectF bounds;
        private int destPageIdx;
        private String value;
        private String contextText;

        public Text(RectF bounds, int destPageIdx, String value) {
            this(bounds, destPageIdx, value, value);
        }

        public Text(RectF bounds, int destPageIdx, String value, String contextText) {
            this.bounds = bounds;
            this.destPageIdx = destPageIdx;
            this.value = value;
            this.contextText = contextText;
        }

        public RectF getBounds() {
            return bounds;
        }

        public int getDestPageIdx() {
            return destPageIdx;
        }

        public String getValue() {
            return value;
        }

        public String getContextText() {
            return contextText;
        }

        @Override
        public String toString() {
            return "Text{" +
                    "bounds=" + bounds +
                    ", destPageIdx=" + destPageIdx +
                    ", value='" + value + '\'' +
                    ", contextText='" + contextText + '\'' +
                    '}';
        }
    }

    public static class StructElement {
        public final static int TYPE_UNKNOWN = -1; // 未知结构元素
        public final static int TYPE_DOCUMENT = 1; // 文档
        public final static int TYPE_PARAGRAPH = 2; // 段落
        public final static int TYPE_HEADING = 3; // 标题（H、H1-H6）
        public final static int TYPE_LIST = 4; // 列表
        public final static int TYPE_TEXT = 5; // 文本
        public final static int TYPE_PART = 6; // 文档部分
        public final static int TYPE_SECTION = 7; // 章节
        public final static int TYPE_DIV = 8; // 通用分区
        public final static int TYPE_LIST_ITEM = 9; // 列表项
        public final static int TYPE_LABEL = 10; // 列表标签
        public final static int TYPE_LIST_BODY = 11; // 列表正文
        public final static int TYPE_TABLE = 12; // 表格
        public final static int TYPE_TABLE_ROW = 13; // 表格行
        public final static int TYPE_TABLE_HEADER = 14; // 表头单元格
        public final static int TYPE_TABLE_DATA = 15; // 表格数据单元格
        public final static int TYPE_SPAN = 16; // 行内文本片段
        public final static int TYPE_FIGURE = 17; // 图形或图片
        public final static int TYPE_FORMULA = 18; // 公式
        public final static int TYPE_LINK = 19; // 链接
        public final static int TYPE_ANNOTATION = 20; // 注释或批注
        public final static int TYPE_CAPTION = 21; // 标题说明
        public final static int TYPE_BLOCK_QUOTE = 22; // 块引用

        private int type;
        private String id;
        private String parentId;
        private int destPageIdx;
        private String text;
        private RectF bounds;

        public StructElement(String type, String id, String parentId, int destPageIdx, String text, RectF bounds) {
            this.type = checkType(type);
            this.id = id;
            this.parentId = parentId;
            this.destPageIdx = destPageIdx;
            this.text = text;
            this.bounds = bounds;
        }

        public int getType() {
            return type;
        }

        public String getTypeName() {
            switch (type) {
                case TYPE_DOCUMENT:
                    return "Document";
                case TYPE_PARAGRAPH:
                    return "Paragraph";
                case TYPE_HEADING:
                    return "Heading";
                case TYPE_LIST:
                    return "List";
                case TYPE_TEXT:
                    return "Text";
                case TYPE_PART:
                    return "Part";
                case TYPE_SECTION:
                    return "Section";
                case TYPE_DIV:
                    return "Div";
                case TYPE_LIST_ITEM:
                    return "ListItem";
                case TYPE_LABEL:
                    return "Label";
                case TYPE_LIST_BODY:
                    return "ListBody";
                case TYPE_TABLE:
                    return "Table";
                case TYPE_TABLE_ROW:
                    return "TableRow";
                case TYPE_TABLE_HEADER:
                    return "TableHeader";
                case TYPE_TABLE_DATA:
                    return "TableData";
                case TYPE_SPAN:
                    return "Span";
                case TYPE_FIGURE:
                    return "Figure";
                case TYPE_FORMULA:
                    return "Formula";
                case TYPE_LINK:
                    return "Link";
                case TYPE_ANNOTATION:
                    return "Annotation";
                case TYPE_CAPTION:
                    return "Caption";
                case TYPE_BLOCK_QUOTE:
                    return "BlockQuote";
                default:
                    return "Unknown";
            }
        }

        public String getId() {
            return id;
        }

        public String getParentId() {
            return parentId;
        }

        public int getDestPageIdx() {
            return destPageIdx;
        }

        public String getText() {
            return text;
        }

        public RectF getBounds() {
            return bounds;
        }

        @Override
        public String toString() {
            return "StructElement{" +
                    "type=" + getTypeName() +
                    "(" + type + ")" +
                    ", id=" + id +
                    ", parentId=" + parentId +
                    ", destPageIdx=" + destPageIdx +
                    ", text='" + text + '\'' +
                    ", bounds=" + bounds +
                    '}';
        }

        private int checkType(String type) {
            if ("Document".equals(type)) {
                return TYPE_DOCUMENT;
            } else if ("P".equals(type)) {
                return TYPE_PARAGRAPH;
            } else if ("H".equals(type) || "H1".equals(type) || "H2".equals(type)
                    || "H3".equals(type) || "H4".equals(type) || "H5".equals(type)
                    || "H6".equals(type)) {
                return TYPE_HEADING;
            } else if ("L".equals(type)) {
                return TYPE_LIST;
            } else if ("T".equals(type)) {
                return TYPE_TEXT;
            } else if ("Part".equals(type)) {
                return TYPE_PART;
            } else if ("Sect".equals(type)) {
                return TYPE_SECTION;
            } else if ("Div".equals(type)) {
                return TYPE_DIV;
            } else if ("LI".equals(type)) {
                return TYPE_LIST_ITEM;
            } else if ("Lbl".equals(type)) {
                return TYPE_LABEL;
            } else if ("LBody".equals(type)) {
                return TYPE_LIST_BODY;
            } else if ("Table".equals(type)) {
                return TYPE_TABLE;
            } else if ("TR".equals(type)) {
                return TYPE_TABLE_ROW;
            } else if ("TH".equals(type)) {
                return TYPE_TABLE_HEADER;
            } else if ("TD".equals(type)) {
                return TYPE_TABLE_DATA;
            } else if ("Span".equals(type)) {
                return TYPE_SPAN;
            } else if ("Figure".equals(type)) {
                return TYPE_FIGURE;
            } else if ("Formula".equals(type)) {
                return TYPE_FORMULA;
            } else if ("Link".equals(type)) {
                return TYPE_LINK;
            } else if ("Annot".equals(type)) {
                return TYPE_ANNOTATION;
            } else if ("Caption".equals(type)) {
                return TYPE_CAPTION;
            } else if ("BlockQuote".equals(type)) {
                return TYPE_BLOCK_QUOTE;
            }
            return TYPE_UNKNOWN;
        }
    }

    public static class StructContent {
        private int pageIndex;
        private ArrayList<StructElement> childElementList = new ArrayList();
        private String text = "";
        private RectF bounds;

        public StructContent(int pageIndex, ArrayList<StructElement> chList, String text, RectF bounds) {
            this.pageIndex = pageIndex;
            this.childElementList = chList;
            this.text = text;
            this.bounds = bounds;
        }

        public int getPageIndex() {
            return this.pageIndex;
        }

        public ArrayList<StructElement> getChildElementList() {
            return this.childElementList;
        }

        public String getText() {
            return this.text;
        }

        public void setText(String text) {
            this.text = text;
        }

         public RectF getBounds() {
            return this.bounds;
        }

        public void setBounds(RectF bounds) {
            this.bounds = bounds;
        }


        @Override
        public String toString() {
            return "StructContent{" +
                    "pageIndex=" + pageIndex +
                    ", childElementList.size=" + childElementList.size() +
                    ", text='" + text + '\'' +
                    ", bounds='" + bounds + '\'' +
                    '}';
        }

         public String toStringWithArray() {
            return "StructContent{" +
                    "pageIndex=" + pageIndex +
                    ", childElementList=" + childElementList.toString() +
                    ", text='" + text + '\'' +
                    ", bounds='" + bounds + '\'' +
                    '}';
        }
    }

    /* package */ PdfDocument() {
    }

    /* package */ long mNativeDocPtr;
    /* package */ ParcelFileDescriptor parcelFileDescriptor;

    /* package */ final Map<Integer, Long> mNativePagesPtr = new ArrayMap<>();

    public boolean hasPage(int index) {
        return mNativePagesPtr.containsKey(index);
    }
}
