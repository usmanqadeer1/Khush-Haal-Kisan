package com.lums.narl.talkingFields.Utils;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

public class ForcastUtils {

    private static final String LOG_TAG = ForcastUtils.class.getSimpleName();

    private ForcastUtils(){

    }

    public static String forcastResponse(LatLng latLng, String apiKey){
        String responseData = null;
        String myUrl = "http://api.openweathermap.org/data/2.5/forecast?appid="+apiKey+"&lat="+latLng.latitude+"&lon="+latLng.longitude;

        try{
            Log.v(LOG_TAG,myUrl);

            URL url = new URL(myUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            if (urlConnection.getResponseMessage().equals("OK")) {
                responseData = PolygonUtils.readPolygonData(urlConnection);                         //reads json response
            }
            urlConnection.disconnect();
            return responseData;

        }catch(java.net.MalformedURLException e){
            Log.e(LOG_TAG,"Malformed Exception");
        }
        catch(java.io.IOException e){
            Log.e(LOG_TAG,"IO Exception");
        }
        return null;
    }


    public static String setWeatherIcon(int actualId){
        int id = actualId / 100;
        String icon = "";
        if(actualId == 800){
            /*long currentTime = new Date().getTime();
            if(currentTime>=sunrise && currentTime<sunset) {
                icon = "&#xf00d;";
            } else {
                icon = "&#xf02e;";
            }*/
            icon = "&#xf00d;";
        } else {
            switch(id) {
                case 2 : icon = "&#xf01e;";
                    break;
                case 3 : icon = "&#xf01c;";
                    break;
                case 7 : icon = "&#xf014;";
                    break;
                case 8 : icon = "&#xf013;";
                    break;
                case 6 : icon = "&#xf01b;";
                    break;
                case 5 : icon = "&#xf019;";
                    break;
            }
        }
        return icon;
    }

    public static int getWeatherID(String in, int index){
        JSONArray list = getList(in);
        try{
            JSONObject item = list.getJSONObject(index);
            JSONArray weather = item.getJSONArray("weather");
            JSONObject a = weather.getJSONObject(0);
            int id = a.getInt("id");
            return id;
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");
        }
        return 1;
    }

    public static int getTemp(String in, int index){
        JSONArray list = getList(in);
        try{
            JSONObject item = list.getJSONObject(index);
            JSONObject detail = item.getJSONObject("main");
            int temp = detail.getInt("temp") -273;
            return temp;
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");
        }
        return 1;
    }


    public static long getDate(String in, int index){
        JSONArray list = getList(in);
        try{
            JSONObject item = list.getJSONObject(index);
            long date = item.getLong("dt");
            return date;
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");
        }
        return 1;
    }


    public static JSONArray getList(String in) {
        try{
            JSONObject reader = new JSONObject(in);
            JSONArray list= reader.getJSONArray("list");
            return  list;
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");
        }
        return null;
    }

}
