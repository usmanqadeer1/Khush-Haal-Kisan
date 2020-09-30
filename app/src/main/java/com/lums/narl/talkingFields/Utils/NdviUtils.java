package com.lums.narl.talkingFields.Utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class NdviUtils {

    private static final String LOG_TAG = PolygonUtils.class.getSimpleName();

    private NdviUtils(){

    }

    public static String polygonNdviResponse(String polygonID, long start, long end, String apiKey){
        String responseData = null;
                String myUrl = "http://api.agromonitoring.com/agro/1.0/image/search?start="+start+"&end="+end+"&type=s2&polyid="+polygonID+"&appid="+apiKey;
        try{
            Log.v("myurl",myUrl);
            URL url = new URL(myUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            if (urlConnection.getResponseMessage().equals("OK")) {
                responseData = PolygonUtils.readPolygonData(urlConnection);
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

    public static String getStatsData(String polygonID, long start, long end, String apiKey){
        String responseData = null;
        String myUrl = "http://api.agromonitoring.com/agro/1.0/ndvi/history?start="+start+"&end="+end+"&type=s2&polyid="+polygonID+"&appid="+apiKey;
        try{
            Log.v("myurl",myUrl);
            URL url = new URL(myUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            if (urlConnection.getResponseMessage().equals("OK")) {
                responseData = PolygonUtils.readPolygonData(urlConnection);
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



    public static String getNdviImageUrl(String in, int index){

        try{
            JSONArray reader = new JSONArray(in);
            JSONObject first = reader.getJSONObject(index);
            Log.v("ndvi",first.toString());

            JSONObject image = first.getJSONObject("tile");
            String ndviUrl = image.getString("ndvi");
            Log.v(LOG_TAG, ndviUrl);
            return ndviUrl;
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");

        }
        return null;

    }

    public static String getNdviBitmapUrl(String in, int index){

        try{
            JSONArray reader = new JSONArray(in);
            JSONObject first = reader.getJSONObject(index);
            Log.v("ndvi",first.toString());

            JSONObject image = first.getJSONObject("image");
            String ndviUrl = image.getString("ndvi");
            Log.v(LOG_TAG, ndviUrl);
            return ndviUrl;
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");
        }
        return null;

    }

    public static String getRgbImageUrl(String in, int index){

        try{
            JSONArray reader = new JSONArray(in);
            JSONObject first = reader.getJSONObject(index);
            JSONObject image = first.getJSONObject("tile");
            String ndviUrl = image.getString("truecolor");
            Log.v(LOG_TAG, ndviUrl);
            return ndviUrl;
        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");

        }
        return null;

    }

    public static ArrayList<Long> getAllDates(String in){
       ArrayList<Long> dates = new ArrayList<>();

        try{
            JSONArray reader = new JSONArray(in);
            int length = reader.length();
            for(int i = length - 1; i>=0;   i--){                                                  //getting dates from recent to past
                JSONObject first = reader.getJSONObject(i);
                long date = first.getLong("dt");
                dates.add(date);
            }
            Log.v("ndvi",dates.toString());

        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");
        }
        return dates;
    }

    public static ArrayList<String> getAllStandardDates(ArrayList<Long> unixDates){
        int n = unixDates.size();
        ArrayList<String> standardDates = new ArrayList<>();
        for(int i = 0; i<n; i++){
            String standardDate = unixToStandardTime(unixDates.get(i));
            standardDates.add(standardDate);
        }
        return standardDates;
    }

    public static String unixToStandardTime(long unixTime){
        Date date = new java.util.Date(unixTime*1000L);
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE, d MMM yyyy");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+5"));
        String formattedDate = sdf.format(date);
        return formattedDate;
    }

    public static String unixToStandardTimeForGraph(long unixTime){
        Date date = new java.util.Date(unixTime*1000L);
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("d MMM yyyy");
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+5"));
        String formattedDate = sdf.format(date);
        return formattedDate;
    }

    public static ArrayList<Double> getMeanValues(String in){
        ArrayList<Double> means = new ArrayList<>();

        try{
            JSONArray reader = new JSONArray(in);
            int length = reader.length();
            for(int i = length - 1; i>=0;   i--){                                                  //getting dates from recent to past
                JSONObject first = reader.getJSONObject(i);
                JSONObject data = first.getJSONObject("data");
                double mean = data.getDouble("mean");
                means.add(mean);
            }
            Log.v("ndvi_mean",means.toString());

        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");
        }
        return means;
    }

    public static ArrayList<Double> getStdValues(String in){
        ArrayList<Double> stds = new ArrayList<>();

        try{
            JSONArray reader = new JSONArray(in);
            int length = reader.length();
            for(int i = length - 1; i>=0;   i--){                                                  //getting dates from recent to past
                JSONObject first = reader.getJSONObject(i);
                JSONObject data = first.getJSONObject("data");
                double std = data.getDouble("std");
                stds.add(std);
            }
            Log.v("ndvi_std",stds.toString());

        }catch(org.json.JSONException e){
            Log.v(LOG_TAG, "JSON Exception");
        }
        return stds;
    }

}
