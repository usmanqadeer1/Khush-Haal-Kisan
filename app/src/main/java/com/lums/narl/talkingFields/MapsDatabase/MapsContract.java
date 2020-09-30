package com.lums.narl.talkingFields.MapsDatabase;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;


public class MapsContract {
    public static final String CONTENT_AUTHORITY = "com.lums.android.maps";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_MAPS = "maps";

    private MapsContract() {}

    public static abstract class MapsEntry implements BaseColumns {

        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_MAPS);
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" +
                CONTENT_AUTHORITY + "/" + PATH_MAPS;
        public static final String CONTENT_LIST_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/"
                + CONTENT_AUTHORITY + "/" + PATH_MAPS;
        public final static String TABLE_NAME = "maps";
        public final static String _ID = BaseColumns._ID;
        public final static String COLUMN_MAP_NAME = "name";
        public final static String COLUMN_MAP_COORDINATES = "coordinates";
        public final static String COLUMN_MAP_AREA = "area";
        public final static String COLUMN_POLYGON_ID ="polygon_id";
        public final static String COLUMN_CROP_TYPE = "crop";
        public final static String COLUMN_MAP_DATE = "date";

    }

    }
