package com.lums.narl.talkingFields.MapsDatabase;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.lums.narl.talkingFields.MapsDatabase.MapsContract.MapsEntry;

public class MapsProvider extends ContentProvider {

    public static final String LOG_TAG = MapsProvider.class.getSimpleName();
    private MapsDbHelper mDbHelper;
    private static final int MAP = 100;
    private static final int MAP_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(MapsContract.CONTENT_AUTHORITY,MapsContract.PATH_MAPS,MAP);
        sUriMatcher.addURI(MapsContract.CONTENT_AUTHORITY,MapsContract.PATH_MAPS+"#",MAP_ID);
    }


    @Override
    public boolean onCreate() {
        mDbHelper = new MapsDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();
        Cursor cursor;
        int match = sUriMatcher.match(uri);
        switch (match) {
            case MAP:
                cursor= database.query(MapsEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null,null,sortOrder);
                break;

            case MAP_ID:
                selection = MapsEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                cursor = database.query(MapsEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;

            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case MAP:
                return insertMap(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    private Uri insertMap(Uri uri, ContentValues values){
        String name = values.getAsString(MapsEntry.COLUMN_MAP_NAME);
        String polygonID = values.getAsString(MapsEntry.COLUMN_POLYGON_ID);
        String coordinates = values.getAsString(MapsEntry.COLUMN_MAP_COORDINATES);
        double area = values.getAsDouble(MapsEntry.COLUMN_MAP_AREA);
        String corn = values.getAsString(MapsEntry.COLUMN_CROP_TYPE);
        String date = values.getAsString(MapsEntry.COLUMN_MAP_DATE);

        if (name == null || coordinates == null || area < 0 || polygonID == null || corn ==null || date == null) {
            throw new IllegalArgumentException("Something is missing");// add date == null condition after it is edited on firebase
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        long id = database.insert(MapsEntry.TABLE_NAME, null, values);
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case MAP:
                return updateMap(uri, contentValues, selection, selectionArgs);
            case MAP_ID:
                selection = MapsEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateMap(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updateMap(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.containsKey(MapsEntry.COLUMN_MAP_NAME)) {
            String name = values.getAsString(MapsEntry.COLUMN_MAP_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Map requires a name");
            }
        }

        if (values.containsKey(MapsEntry.COLUMN_POLYGON_ID)) {
            String name = values.getAsString(MapsEntry.COLUMN_POLYGON_ID);
            if (name == null) {
                throw new IllegalArgumentException("Map didn't get polygon ID from server");
            }
        }

        if (values.containsKey(MapsEntry.COLUMN_MAP_COORDINATES)) {
            String coordinates = values.getAsString(MapsEntry.COLUMN_MAP_COORDINATES);
            if (coordinates.length() < 16) {
                throw new IllegalArgumentException("Map has no cooridnates");
            }
        }
        if (values.containsKey(MapsEntry.COLUMN_MAP_AREA)) {
            Double area = values.getAsDouble(MapsEntry.COLUMN_MAP_AREA);
            if (area <= 0 ) {
                throw new IllegalArgumentException("Map has invalid area");
            }
        }
        if (values.containsKey(MapsEntry.COLUMN_MAP_DATE)) {
            String date = values.getAsString(MapsEntry.COLUMN_MAP_DATE);
            if (date== null)  {
                throw new IllegalArgumentException("Map date has not been mentioned");
            }
        }

        if (values.containsKey(MapsEntry.COLUMN_CROP_TYPE)) {
            String crop = values.getAsString(MapsEntry.COLUMN_CROP_TYPE);
            if (crop== null)  {
                throw new IllegalArgumentException("Crop type has not been mentioned");
            }
        }

        if (values.size() == 0) {
            return 0;
        }

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        int rowsUpdated = database.update(MapsEntry.TABLE_NAME, values, selection, selectionArgs);
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        // Returns the number of database rows affected by the update statement
        return rowsUpdated;
    }


        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            SQLiteDatabase database = mDbHelper.getWritableDatabase();
            int rowsDeleted;
            final int match = sUriMatcher.match(uri);
            switch (match) {
                case MAP:
                    // Delete all rows that match the selection and selection args
                    rowsDeleted = database.delete(MapsEntry.TABLE_NAME, selection, selectionArgs);
                    break;
                case MAP_ID:
                    // Delete a single row given by the ID in the URI
                    selection = MapsEntry._ID + "=?";
                    selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                    rowsDeleted = database.delete(MapsEntry.TABLE_NAME, selection, selectionArgs);
                    break;
                default:
                    throw new IllegalArgumentException("Deletion is not supported for " + uri);
            }

            if (rowsDeleted != 0) {
                getContext().getContentResolver().notifyChange(uri, null);
            }

            return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case MAP:
                return MapsEntry.CONTENT_LIST_TYPE;
            case MAP_ID:
                return MapsEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    public int getProfilesCount() {
        String countQuery = "SELECT  * FROM " + MapsEntry.TABLE_NAME;
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }
}
