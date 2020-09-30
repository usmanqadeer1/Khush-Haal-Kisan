package com.lums.narl.talkingFields.Utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public final class PolygonUtils {

    private static final String LOG_TAG = PolygonUtils.class.getSimpleName();
    private static BufferedReader reader;
    Context mContext;

    private PolygonUtils() {
    }

    public static String HttpPost(String apiKey, String fieldName, ArrayList<LatLng> latLngs) throws IOException, JSONException {

        String myUrl = "http://api.agromonitoring.com/agro/1.0/polygons?appid="+apiKey;

        URL url = new URL(myUrl);

        // 1. create HttpURLConnection
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("Content-Type", "application/json");

        JSONObject jsonObject = polygonJsonObject(fieldName, latLngs);
        setPostRequestContent(urlConnection, jsonObject);
        urlConnection.connect();
        if (urlConnection.getResponseCode() != 201) {

            Log.v("ndvi",urlConnection.getResponseCode()+"");
            return null;
        }
        String response = readPolygonData(urlConnection);
        Log.i(LOG_TAG, response);
        urlConnection.disconnect();
        return response;

    }

    private static JSONObject polygonJsonObject(String name, ArrayList<LatLng> latLngs) throws JSONException {

        JSONArray coordinates = new JSONArray();
        coordinates.put(putAllCoordinates(latLngs));

        JSONObject geometry = new JSONObject();
        geometry.put("type", "Polygon");
        geometry.put("coordinates", coordinates);

        JSONObject properties = new JSONObject();


        JSONObject features = new JSONObject();


        features.put("type", "Feature");
        features.put("properties", properties);
        features.put("geometry", geometry);

        JSONArray featureArray = new JSONArray();
        featureArray.put(features);

        JSONObject geoJson = new JSONObject();
        geoJson.put("type", "FeatureCollection");
        geoJson.put("features", featureArray);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("geo_json", geoJson);

        return jsonObject;
    }

    private static void setPostRequestContent(HttpURLConnection conn,
                                              JSONObject jsonObject) throws IOException {

        OutputStream os = conn.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(jsonObject.toString());
        Log.i(LOG_TAG, jsonObject.toString());
        writer.flush();
        writer.close();
        os.close();
    }


    private static JSONArray putAllCoordinates(ArrayList<LatLng> latLngs) {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < latLngs.size(); i++) {
            jsonArray.put(putCoordinate(latLngs.get(i)));
        }
        jsonArray.put(putCoordinate(latLngs.get(0)));
        return jsonArray;
    }

    private static JSONArray putCoordinate(LatLng latLng) {
        JSONArray jsonArray = new JSONArray();
        try {
//            double lon = (double) Math.round(latLng.longitude * 10000) / 10000;
//            double lat = (double) Math.round(latLng.latitude * 10000) / 10000;
            double lon = latLng.longitude;
            double lat = latLng.latitude;
            jsonArray.put(lon);
            jsonArray.put(lat);
        } catch (org.json.JSONException e) {
            return null;
        }
        return jsonArray;
    }

    public static String readPolygonData(URLConnection conn) {
        StringBuilder sb = null;
        try {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            sb = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (Exception ex) {
        } finally {
            try {
                reader.close();
                return sb.toString();
            } catch (Exception ex) {
            }
        }
        return sb.toString();
    }

    public static String HttpDelete(String polygonID, String apiKey) {
        String myUrl = "http://api.agromonitoring.com/agro/1.0/polygons/"+polygonID+"?appid="+apiKey;

        String response = null;
        try {
            URL url = new URL(myUrl);

            // 1. create HttpURLConnection
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("DELETE");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.connect();
            response = urlConnection.getResponseMessage() + " " + urlConnection.getResponseCode();
            Log.i(LOG_TAG, response);
            urlConnection.disconnect();

        } catch (java.net.MalformedURLException e) {
            Log.v(LOG_TAG, "wrong url");
        } catch (java.net.ProtocolException e) {
            Log.v(LOG_TAG, "no such method");
        } catch (IOException e) {
            Log.v(LOG_TAG, "IO Exception");
        }
        return response;
    }

    public static String getPolygonId(String in){
        String polygonId=null;
        try{
           JSONObject reader = new JSONObject(in);
           polygonId = reader.getString("id");
        }catch(org.json.JSONException e){
        }
       return polygonId;
    }



    public boolean checkNetworkConnection() {
        ConnectivityManager connMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        boolean isConnected = false;
        if (networkInfo != null && (isConnected = networkInfo.isConnected())) {
            Toast.makeText(mContext, "Connected", Toast.LENGTH_LONG);
        } else {
            Toast.makeText(mContext, "Not Connected", Toast.LENGTH_LONG);

        }

        return isConnected;
    }

}
