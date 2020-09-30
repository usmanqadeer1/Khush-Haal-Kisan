package com.lums.narl.talkingFields;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.lums.narl.talkingFields.MapsDatabase.MapsContract;
import com.lums.narl.talkingFields.MapsDatabase.MapsContract.MapsEntry;
import com.lums.narl.talkingFields.Utils.DownloadImageUtils;
import com.lums.narl.talkingFields.Utils.ForcastUtils;
import com.lums.narl.talkingFields.Utils.LocaleUtils;
import com.lums.narl.talkingFields.Utils.WeatherUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MapsAdapter extends CursorAdapter {

    private AlertDialog.Builder askToDelete;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DatabaseReference usersDatabase;
    private String userID;
    private String mapNameOriginal, cropType, polygonID, coordinates;
    private static String  weatherJsonResponse, forcastJsonResponse;
    TextView temperature, humidity;
    double mapArea;
    int MAP_ID;
    TextView imageDay1, imageDay2, imageDay3, imageDay4, imageDay5;
    TextView dateDay1, dateDay2, dateDay3, dateDay4, dateDay5;
    TextView tempDay1, tempDay2, tempDay3, tempDay4, tempDay5;
    Typeface weatherFont;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    Handler mHandler;
    private long forecastTime, currentTime;

    public MapsAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {

        // initialize sharedpref
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);//this.getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        //get icons for weather
        weatherFont = Typeface.createFromAsset(context.getAssets(), "fonts/weathericons-regular-webfont.ttf");

        // initialize UI componnents of list item
        TextView nameTextView =  view.findViewById(R.id.name);
        TextView cropTextView = view.findViewById(R.id.crop_type);
        TextView areaTextView = view.findViewById(R.id.area);
        temperature = view.findViewById(R.id.temp);
        humidity = view.findViewById(R.id.humidity);
        ImageView imageView = view.findViewById(R.id.ndvi_image);
        ImageView deleteButton = view.findViewById(R.id.delete_item);
        ImageView editButton = view.findViewById(R.id.edit_item);
        initializeForcastBlock(view);
        mHandler = new Handler();


        // getting values for each item to fill in UI components
        int id = cursor.getColumnIndex(MapsContract.MapsEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(MapsEntry.COLUMN_MAP_NAME);
        int areaColumnIndex = cursor.getColumnIndex(MapsEntry.COLUMN_MAP_AREA);
        int cropColumnIndex = cursor .getColumnIndex(MapsEntry.COLUMN_CROP_TYPE);
        int coordinatesColumnIndex = cursor.getColumnIndex(MapsEntry.COLUMN_MAP_COORDINATES);
        int polyIDColumnIndex = cursor.getColumnIndex(MapsEntry.COLUMN_POLYGON_ID);
        MAP_ID = cursor.getInt(id);

        String mapName = cursor.getString(nameColumnIndex);
        mapNameOriginal=mapName;
        double mapArea = cursor.getDouble(areaColumnIndex);
        cropType = cursor.getString(cropColumnIndex);
        polygonID = cursor.getString(polyIDColumnIndex);
        coordinates = cursor.getString(coordinatesColumnIndex);
        ArrayList<LatLng> allCoordinates = getCoordinates(coordinates);
        weatherJsonResponse = sharedPref.getString(polygonID+"_weather",null);
        forcastJsonResponse = sharedPref.getString(polygonID+"_forecast",null);
        currentTime = System.currentTimeMillis() / 1000L;

        // if no repsonse for forecast and weather response is saved, then download iff network is available
        if(forcastJsonResponse == null && LocaleUtils.isNetworkAvailable(context)){
            Toast.makeText(context,context.getString(R.string.wait_dialog),Toast.LENGTH_LONG).show();
            getForecast(allCoordinates.get(0), context);
        }

        if(weatherJsonResponse==null && LocaleUtils.isNetworkAvailable(context)) {
            Toast.makeText(context,context.getString(R.string.wait_dialog),Toast.LENGTH_LONG).show();
            getWeather(allCoordinates.get(0), context);
        }

        // download weather data if weatherJsonResponse is not equal to null and mentioned time has passed
        // 3 hours for weather, and 1 day for forecast to update
        if(weatherJsonResponse!=null)
        {
            Log.v("response",weatherJsonResponse);
            long time = WeatherUtils.getTime(weatherJsonResponse);

            if(currentTime - time < 22000 ||  !LocaleUtils.isNetworkAvailable(context)){
                int temp = WeatherUtils.getTemp(weatherJsonResponse) - 273;
                int humid = WeatherUtils.getHumidity(weatherJsonResponse);
                temperature.setText(temp+"°C");
                humidity.setText(humid+"%");
            }
            else if(currentTime - time >= 22000 && LocaleUtils.isNetworkAvailable(context)){
                Toast.makeText(context,context.getString(R.string.wait_dialog),Toast.LENGTH_LONG).show();
                getWeather(allCoordinates.get(0), context);
            }
        }

        if(forcastJsonResponse!=null){
            Log.v("response",forcastJsonResponse);
            forecastTime = ForcastUtils.getDate(forcastJsonResponse,0);
            if(currentTime - forecastTime < 86400 || !LocaleUtils.isNetworkAvailable(context)){
                setForcast(forcastJsonResponse);
            }
            else if(currentTime - forecastTime >= 86400 && LocaleUtils.isNetworkAvailable(context)){
                Toast.makeText(context,context.getString(R.string.wait_dialog),Toast.LENGTH_LONG).show();
                getForecast(allCoordinates.get(0), context);
            }
        }

        if(mapName.length()>5){
            if(mapName.substring(0,5).equals("Field")){
                mapName = context.getString(R.string.field)+mapName.substring(5,mapName.length());
                nameTextView.setText(mapName);
            }
            else{
                nameTextView.setText(mapName);
            }
        }
        else{
            nameTextView.setText(mapName);
        }

        String crop = context.getString(R.string.crop)+":\t";
        switch(cropType){
            case "Corn":        crop=context.getString(R.string.corn); break;
            case "Wheat":       crop=context.getString(R.string.wheat);break;
            case "Rice":        crop=context.getString(R.string.rice);break;
            case "Cotton":      crop=context.getString(R.string.cotton);break;
            case "Sugarcane":   crop=context.getString(R.string.sugarcane);break;
            case "Potato":      crop=context.getString(R.string.potato);break;
            default:            crop=context.getString(R.string.other_crop);break;
        }
        cropTextView.setText(crop);
        areaTextView.setText(Math.round(mapArea*10)/10.0+" "+context.getString(R.string.acre));
        String path = context.getFilesDir().getAbsolutePath() + "/" + polygonID+"_"+0+".png";
        File file = new File(path);
        Log.v("path",file.toString());
        if(file.exists())
            imageView.setImageBitmap(DownloadImageUtils.loadImageBitmap(context, polygonID,0));

        /*askToDelete = new AlertDialog.Builder(context);
        deleteButton.setTag(new MapField());
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteDialog(context);
            }
        });
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEditMapActivity(context);
            }
        });*/
    }

/*
    private void showDeleteDialog(final Context context){
        askToDelete.setMessage(context.getString(R.string.ask_to_delete))
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deletePolygon(context,polygonID,context.getString(R.string.Agro_API_Key),mapNameOriginal);
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        //Creating dialog box
        AlertDialog alert = askToDelete.create();
        alert.show();
    }

    private void deletePolygon(Context context, String polygonID, String apiKey, String name){
        HttpDeleteTask httpDeleteTask = new HttpDeleteTask();
        httpDeleteTask.execute(polygonID, apiKey);
        try{ Thread.sleep(3000); }catch(InterruptedException e){ }
        String userID =FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference dR = FirebaseDatabase.getInstance().getReference(userID);

        dR.child("Map Data").child(name).removeValue();
        context.getContentResolver().delete(MapsEntry.CONTENT_URI,MapsEntry._ID + "=" + MAP_ID,null );


        File file = context.getFileStreamPath(polygonID+"_"+0+".png");
        if(file.delete()) {Log.d("delete File","deleted file");}
    }


    private class HttpDeleteTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... args) {
            return PolygonUtils.HttpDelete(args[0],args[1]);

        }

        @Override
        protected void onPostExecute(String result) {
        }
    }

    private void openEditMapActivity(Context context){
        Intent viewMap = new Intent (context, EditMapActivity.class);
        Uri currentMapUri = ContentUris.withAppendedId(MapsEntry.CONTENT_URI,MAP_ID);
        viewMap.setData(currentMapUri);
        context.startActivity(viewMap);
    }
*/
    private ArrayList<LatLng> getCoordinates( String coordinateString){
        String coordinates = coordinateString.substring(8, coordinateString.length() - 2);
        String[] myList = coordinates.split(";\nlat/lng:");

        ArrayList<LatLng> latLngs1 = new ArrayList<>();

        for (int i = 0; i < myList.length; i++) {
            String s = myList[i].substring(2, myList[i].length() - 1);
            String[] s1 = s.split(",");
            double lat = Double.parseDouble(s1[0]);
            double lng = Double.parseDouble(s1[1]);
            LatLng latLng = new LatLng(lat, lng);
            latLngs1.add(latLng);
        }
        return latLngs1;
    }

    private class WeatherDownloadTask extends AsyncTask<LatLng, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(LatLng...latLngs) {
            String result = WeatherUtils.weatherResponse(latLngs[0],"81c800ebb073b4f27808689c60ec87aa");
            weatherJsonResponse = result;
            return result;
        }
        @Override
        protected void onPostExecute(String result) {
            if(result != null){
                weatherJsonResponse = result;
                int temp = WeatherUtils.getTemp(result) - 273;
                int humid = WeatherUtils.getHumidity(result);
                temperature.setText(temp+"°C");
                humidity.setText(humid+"%");
                editor.putString(polygonID+"_weather", weatherJsonResponse);
                editor.apply();
            }
        }
    }

    private class ForcastDownloadTask extends AsyncTask<LatLng, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(LatLng...latLngs) {
            String result =ForcastUtils.forcastResponse(latLngs[0],"81c800ebb073b4f27808689c60ec87aa");
            forcastJsonResponse = result;
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if(result!=null){
                forcastJsonResponse = result;
                setForcast(result);
                editor.putString(polygonID+"_forecast", forcastJsonResponse);
                editor.apply();
            }
        }
    }

    private void getWeather(LatLng latLng, Context context){
            WeatherDownloadTask weatherDownloadTask = new WeatherDownloadTask();
            weatherDownloadTask.execute(latLng);
//            try{Thread.sleep(1000);}catch(Exception e){}

    }

    private void getForecast(LatLng latLng, Context context){
        ForcastDownloadTask forcastDownloadTask = new ForcastDownloadTask();
        forcastDownloadTask.execute(latLng);
    }

    private void initializeForcastBlock(View view){

        dateDay1 = view.findViewById(R.id.date_day_1);
        dateDay2 = view.findViewById(R.id.date_day_2);
        dateDay3 = view.findViewById(R.id.date_day_3);
        dateDay4 = view.findViewById(R.id.date_day_4);
        dateDay5 = view.findViewById(R.id.date_day_5);
        tempDay1 = view.findViewById(R.id.temp_day_1);
        tempDay2 = view.findViewById(R.id.temp_day_2);
        tempDay3 = view.findViewById(R.id.temp_day_3);
        tempDay4 = view.findViewById(R.id.temp_day_4);
        tempDay5 = view.findViewById(R.id.temp_day_5);
        imageDay1 = view.findViewById(R.id.image_day_1);
        imageDay2 = view.findViewById(R.id.image_day_2);
        imageDay3 = view.findViewById(R.id.image_day_3);
        imageDay4 = view.findViewById(R.id.image_day_4);
        imageDay5 = view.findViewById(R.id.image_day_5);
        imageDay1.setTypeface(weatherFont);
        imageDay2.setTypeface(weatherFont);
        imageDay3.setTypeface(weatherFont);
        imageDay4.setTypeface(weatherFont);
        imageDay5.setTypeface(weatherFont);
    }
    private long date1, date2, date3, date4, date5;
    private int id1, id2, id3, id4, id5;
    private int temp1, temp2, temp3, temp4, temp5;
    private String icon1, icon2, icon3, icon4, icon5;

    private void updatevariables(String forcastResponse, int i){
        date1 = ForcastUtils.getDate(forcastResponse,i);
        date2 = ForcastUtils.getDate(forcastResponse,i+8);
        date3 = ForcastUtils.getDate(forcastResponse,i+16);
        date4 = ForcastUtils.getDate(forcastResponse,i+24);
        date5 = ForcastUtils.getDate(forcastResponse,i+32);
        id1 = ForcastUtils.getWeatherID(forcastResponse,i);
        id2 = ForcastUtils.getWeatherID(forcastResponse,i+8);
        id3 = ForcastUtils.getWeatherID(forcastResponse,i+16);
        id4 = ForcastUtils.getWeatherID(forcastResponse,i+24);
        id5 = ForcastUtils.getWeatherID(forcastResponse,i+32);
        temp1 = ForcastUtils.getTemp(forcastResponse, i);
        temp2 = ForcastUtils.getTemp(forcastResponse, i+8);
        temp3 = ForcastUtils.getTemp(forcastResponse, i+16);
        temp4 = ForcastUtils.getTemp(forcastResponse, i+24);
        temp5 = ForcastUtils.getTemp(forcastResponse, i+32);
    }
    private void setForcast(String forcastResponse){

        if(currentTime-forecastTime < 10800){
            updatevariables(forcastResponse, 0);
        }
        else if (currentTime-forecastTime  < 2*10800){
            updatevariables(forcastResponse, 1);

        }
        else if (currentTime-forecastTime  < 3*10800){
            updatevariables(forcastResponse, 2);

        }else if (currentTime-forecastTime  < 4*10800){
            updatevariables(forcastResponse, 3);

        }else if (currentTime-forecastTime  < 5*10800){
            updatevariables(forcastResponse, 4);

        }else if (currentTime-forecastTime  < 6*10800){
            updatevariables(forcastResponse, 5);

        }else{
            updatevariables(forcastResponse, 6);
        }
        long a = currentTime-forecastTime ;
        Log.v("time",""+a);
        icon1 = ForcastUtils.setWeatherIcon(id1);
        icon2 = ForcastUtils.setWeatherIcon(id2);
        icon3 = ForcastUtils.setWeatherIcon(id3);
        icon4 = ForcastUtils.setWeatherIcon(id4);
        icon5 = ForcastUtils.setWeatherIcon(id5);

        dateDay1.setText(unixToStandardTime(date1));
        dateDay2.setText(unixToStandardTime(date2));
        dateDay3.setText(unixToStandardTime(date3));
        dateDay4.setText(unixToStandardTime(date4));
        dateDay5.setText(unixToStandardTime(date5));

        imageDay1.setText(Html.fromHtml(icon1));
        imageDay2.setText(Html.fromHtml(icon2));
        imageDay3.setText(Html.fromHtml(icon3));
        imageDay4.setText(Html.fromHtml(icon4));
        imageDay5.setText(Html.fromHtml(icon5));

        tempDay1.setText(temp1+"°C");
        tempDay2.setText(temp2+"°C");
        tempDay3.setText(temp3+"°C");
        tempDay4.setText(temp4+"°C");
        tempDay5.setText(temp5+"°C");

    }

    private String unixToStandardTime(long unixTime){
        Date date = new java.util.Date(unixTime*1000L);
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+5"));
        String formattedDate = sdf.format(date);
        return formattedDate;
    }
}
