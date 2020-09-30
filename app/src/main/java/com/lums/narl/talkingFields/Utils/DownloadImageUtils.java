package com.lums.narl.talkingFields.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public final class DownloadImageUtils {
    public DownloadImageUtils(){

    }
    public static void saveImage(Context context, Bitmap b, String polyID,long date) {
        FileOutputStream foStream;
        String imageName = polyID+"_"+date+".png";
        try {
            foStream = context.openFileOutput(imageName, Context.MODE_PRIVATE);
            b.compress(Bitmap.CompressFormat.PNG, 100, foStream);
            foStream.close();
        } catch (Exception e) {
            Log.d("saveImage", "Exception 2, Something went wrong!");
            e.printStackTrace();
        }
    }

    public static Bitmap loadImageBitmap(Context context, String polyID, long date) {
        Bitmap bitmap = null;
        FileInputStream fiStream;
        String imageName = polyID+"_"+date+".png";
        try {
            fiStream    = context.openFileInput(imageName);
            bitmap      = BitmapFactory.decodeStream(fiStream);
            fiStream.close();
        } catch (Exception e) {
            Log.d("saveImage", "Exception 3, Something went wrong!");
            e.printStackTrace();
        }
        return bitmap;
    }
}
