package com.lums.narl.talkingFields.Utils;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;

public class WeatherUtils {

    private static final String LOG_TAG = WeatherUtils.class.getSimpleName();

    private WeatherUtils(){

    }

    public static String weatherResponse(LatLng latLng, String apiKey){
        String responseData = null;
//        String myUrl="http://api.openweathermap.org/data/2.5/weather?appid=81c800ebb073b4f27808689c60ec87aa&lat=31&lon=71";
        String myUrl = "http://api.openweathermap.org/data/2.5/weather?appid="+apiKey+"&lat="+latLng.latitude+"&lon="+latLng.longitude;

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

    public static String getCityName(String in){

        try{
            JSONObject reader = new JSONObject(in);
            String  city= reader.getString("name");
            Log.v("city",city);
            return city;
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");

        }
        return null;
    }

    public static String getWeather(String in){
        try{
            JSONObject reader = new JSONObject(in);
            JSONArray weather = reader.getJSONArray("weather");
            JSONObject first = weather.getJSONObject(0);
            String main= first.getString("main");
            String desctiption= first.getString("description");
            Log.v("city",main+weather);
            return main + ","+ desctiption;
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");

        }
        return null;
    }

    public static int getTemp(String in){
        try{
            JSONObject reader = new JSONObject(in);
            JSONObject main = reader.getJSONObject("main");
            int temp = main.getInt("temp");

            return temp;
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");
        }
        return 1;
    }

    public static int getHumidity(String in){
        try{
            JSONObject reader = new JSONObject(in);
            JSONObject main = reader.getJSONObject("main");
            int humidity = main.getInt("humidity");

            return humidity;
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");
        }
        return 1;
    }

    public static int getMinTemp(String in){
        try{
            JSONObject reader = new JSONObject(in);
            JSONObject main = reader.getJSONObject("main");
            int temp_min = main.getInt("temp_min");

            return temp_min;
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");
        }
        return 1;
    }

    public static int getMaxTemp(String in){
        try{
            JSONObject reader = new JSONObject(in);
            JSONObject main = reader.getJSONObject("main");
            int temp_max = main.getInt("temp_max");

            return temp_max;
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");
        }
        return 1;
    }

    public static String getCountry(String in){
        try{
            JSONObject reader = new JSONObject(in);
            JSONObject main = reader.getJSONObject("sys");
            String temp = reader.getString("country");

            return main.toString();
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");
        }
        return null;
    }

    public static long getTime(String in){
        try{
            JSONObject reader = new JSONObject(in);
            long time = reader.getInt("dt");

            return time;
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");
        }
        return 1;
    }

}
