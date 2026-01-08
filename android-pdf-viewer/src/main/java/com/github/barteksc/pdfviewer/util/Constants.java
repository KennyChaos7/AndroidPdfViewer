/**
 * Copyright 2016 Bartosz Schiller
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.barteksc.pdfviewer.util;

import com.github.barteksc.pdfviewer.PDFView;

public class Constants {

    public static boolean DEBUG_MODE = false;

    /** Between 0 and 1, the thumbnails quality (default 0.3). Increasing this value may cause performance decrease */
    public static float THUMBNAIL_RATIO = 0.3f;

    // 缩略图拆分小快进行分块加载，默认为4快
    public final static int THUMBNAIL_SPLIT = 4;

    /**
     * The size of the rendered parts (default 256)
     * Tinier : a little bit slower to have the whole page rendered but more reactive.
     * Bigger : user will have to wait longer to have the first visual results
     */
    public static float PART_SIZE = 256;

    /** Part of document above and below screen that should be preloaded, in dp */
    public static int PRELOAD_OFFSET = 20;

    public static class Cache {

        /** The size of the cache (number of bitmaps kept) */
        public static int CACHE_SIZE = 120;
        //默认的缩略图最大缓存数量
        public static int DEFAULT_THUMBNAILS_CACHE_SIZE = 8;
        /**
         * 缩略图的最大缓存数量跟是否进行缩略图分块#{@link PDFView#isThumbnailSplit()}处理相关
         * 会在设置完缩略图是否分块{@link PDFView#setThumbnailSplit(boolean)}后进行重新设值
         */
        public static int THUMBNAILS_CACHE_SIZE = DEFAULT_THUMBNAILS_CACHE_SIZE;
    }

    public static class Pinch {

        public static float MAXIMUM_ZOOM = 10;

        public static float MINIMUM_ZOOM = .5f;

    }

}
