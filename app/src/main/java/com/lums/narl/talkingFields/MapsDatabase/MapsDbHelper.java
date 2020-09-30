package com.lums.narl.talkingFields.MapsDatabase;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.lums.narl.talkingFields.MapsDatabase.MapsContract.MapsEntry;

import java.util.ArrayList;

public class MapsDbHelper extends SQLiteOpenHelper {
    public static final String LOG_TAG = MapsDbHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "talkerMaps.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "maps";

    public MapsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String SQL_CREATE_MAPS_TABLE = "CREATE TABLE "
                + MapsEntry.TABLE_NAME + " ("
                + MapsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + MapsEntry.COLUMN_MAP_NAME + " TEXT NOT NULL,"
                + MapsEntry.COLUMN_POLYGON_ID + " TEXT NOT NULL,"
                + MapsEntry.COLUMN_MAP_COORDINATES + " TEXT NOT NULL,"
                + MapsEntry.COLUMN_MAP_AREA + " DOUBLE NOT NULL,"
                + MapsEntry.COLUMN_MAP_DATE + " TEXT NOT NULL,"
                + MapsEntry.COLUMN_CROP_TYPE + " TEXT NOT NULL);";
        Log.v(LOG_TAG,SQL_CREATE_MAPS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_MAPS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public ArrayList<String> getAllNames(){
        ArrayList<String> labels = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT name FROM " + TABLE_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if(cursor!=null && cursor.getCount() > 0){
            if (cursor.moveToFirst()) {
                do {
                    labels.add(cursor.getString(cursor.getColumnIndex("name")));
                } while (cursor.moveToNext());
            }
        }

        // closing connection
        cursor.close();
        db.close();
        // returning lables
        return labels;
    }

    public ArrayList<String> getAllPolygonIds(){
        ArrayList<String> labels = new ArrayList<>();
        // Select All Query
        String selectQuery = "SELECT polygon_id FROM " + TABLE_NAME;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list

        if(cursor!=null && cursor.getCount() > 0) {
            if (cursor.moveToFirst()) {
                do {
                    labels.add(cursor.getString(cursor.getColumnIndex("polygon_id")));
                } while (cursor.moveToNext());
            }
        }
        // closing connection
        cursor.close();
        db.close();
        // returning lables
        return labels;
    }
}

