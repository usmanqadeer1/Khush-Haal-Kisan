package com.lums.narl.talkingFields;

import com.lums.narl.talkingFields.Utils.LocaleUtils;
import com.lums.narl.talkingFields.Utils.NdviUtils;
import com.lums.narl.talkingFields.Utils.PolygonUtils;
import com.lums.narl.talkingFields.Utils.DownloadImageUtils;

import android.app.ActionBar;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.lums.narl.talkingFields.MapsDatabase.MapsContract.MapsEntry;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import info.hoang8f.android.segmented.SegmentedGroup;


public class MapViewActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, LoaderManager.LoaderCallbacks<Cursor> {

    private int MAP_ID;
    private int imageFlag = 0, moveFlag = 0;
    Uri mCurrentMapUri;
    private GoogleMap mMap;
    String name, coordinateString, crop;
    Double area;
    SupportMapFragment mapFragment;
    ArrayList<LatLng> latLngs1;
    String ndviJsonResponse = "";
    private String POLY_ID = "";
    private long FINAL_DATE = System.currentTimeMillis() / 1000L;
    //    private long START_DATE = FINAL_DATE - 2419200;
    private long START_DATE = 1451606400;
    private long ndviJsonCheckTime;
    public static String NDVIJSONCHECKTIME = "ndvi_json_check_time";
    private String mImageryDate;
    private String imageDatetoDownload;
    RadioButton radioNdvi, radioSatellite;
    SegmentedGroup segmented2;


    private Spinner mDateSpinner;
    private ProgressBar loadingIndicator;
    private TextView fieldID, fieldArea, editFieldMap;
    private ImageView ndviClip, graphImageView;
    private ImageView previous, next;
    private Button currentLocation;
    private TileOverlay ndvi, previous_ndvi;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DatabaseReference usersDatabase;
    private String userID;
    ArrayList<String> standardDates;
    ArrayList<Long> unixDates;
    private Handler mapViewHandler;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleUtils.loadLocale(this);

        setContentView(R.layout.activity_map_view);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPref.edit();

        mDateSpinner = findViewById(R.id.spinner_dates);
        loadingIndicator = findViewById(R.id.loading_indicator);
        currentLocation = findViewById(R.id.current_location);
        segmented2 = findViewById(R.id.segmented2);
        radioNdvi = findViewById(R.id.ndvi);
        radioSatellite = findViewById(R.id.satellite);
        segmented2.setTintColor(Color.rgb(18, 135, 36));
        next = findViewById(R.id.next);
        previous = findViewById(R.id.previous);
        graphImageView = findViewById(R.id.ndvi_image_clip);
        radioNdvi.setChecked(true);
        mapViewHandler = new Handler();

        //get the time when ndvijsonresposne was last updated
        ndviJsonCheckTime = sharedPref.getLong(NDVIJSONCHECKTIME,0);

        //change color of loading indicator
        loadingIndicator.setIndeterminate(true);
        loadingIndicator.getIndeterminateDrawable().setColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.MULTIPLY);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            userID = firebaseUser.getUid();
            usersDatabase = FirebaseDatabase.getInstance().getReference(userID);
        }

        //display back button
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_view);
        mapFragment.getMapAsync(this);

        currentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckPermission();
                getLocation();
                addCurrentMarker();
                if (myLatLng != null)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15));
            }
        });

        radioNdvi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (ndvi != null) ndvi.setTransparency(1.0f);
            }
        });
        radioSatellite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (ndvi != null) ndvi.setTransparency(0.0f);
            }
        });
        Intent intent = getIntent();
        mCurrentMapUri = intent.getData();
        getLoaderManager().initLoader(0, null, this);
    }


    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng pak = new LatLng(31.5, 74);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pak, 10));
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    }

    private void setupSpinner(final ArrayList<String> dates) {

        ArrayAdapter<String> dateSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dates);

        dateSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        mDateSpinner.setAdapter(dateSpinnerAdapter);
        mDateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) next.setVisibility(View.INVISIBLE);
                if (position == dates.size() - 1) previous.setVisibility(View.INVISIBLE);
                mImageryDate = (String) parent.getItemAtPosition(position);
                if (previous_ndvi != null) {
                    previous_ndvi.remove();
                }
                waitForDownload(3000);
                String ndviImageUrl = NdviUtils.getNdviImageUrl(ndviJsonResponse, dates.size() - position - 1);              //get TileOverlay for NDVI
                ndviImageUrl = ndviImageUrl + "&paletteid=4";
                ndvi = loadOverlay(ndviImageUrl);
                previous_ndvi = ndvi;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previous.setVisibility(View.VISIBLE);
                int i = mDateSpinner.getSelectedItemPosition();
                if (i > 0) {
                    i = i - 1;
                    mDateSpinner.setSelection(i);
                }
                if (i == 0)
                    next.setVisibility(View.INVISIBLE);

            }
        });

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                next.setVisibility(View.VISIBLE);
                int i = mDateSpinner.getSelectedItemPosition();
                if (i < unixDates.size() - 1) {
                    i = i + 1;
                    mDateSpinner.setSelection(i);
                }
                if (i == unixDates.size() - 1)
                    previous.setVisibility(View.INVISIBLE);
            }
        });

    }

    private void waitForDownload(final long millis) {
        Thread displayIndicator = new Thread(new Runnable() {
            @Override
            public void run() {
                mapViewHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mDateSpinner.setEnabled(false);
                        next.setEnabled(false);
                        previous.setEnabled(false);
                        loadingIndicator.setVisibility(View.VISIBLE);
                    }
                });
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mapViewHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mDateSpinner.setEnabled(true);
                        next.setEnabled(true);
                        previous.setEnabled(true);
                        loadingIndicator.setVisibility(View.GONE);

                    }
                });
            }
        });
        displayIndicator.start();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {MapsEntry._ID,
                MapsEntry.COLUMN_MAP_NAME,
                MapsEntry.COLUMN_POLYGON_ID,
                MapsEntry.COLUMN_MAP_COORDINATES,
                MapsEntry.COLUMN_MAP_AREA,
                MapsEntry.COLUMN_CROP_TYPE};

        String selection = MapsEntry._ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(ContentUris.parseId(mCurrentMapUri))};

        return new CursorLoader(this,
                MapsEntry.CONTENT_URI,          // The content URI of the words table
                projection,                     // The columns to return for each row
                selection,                  // Selection criteria
                selectionArgs,               // Selection criteria
                null);                 // The sort order for the returned rows
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.v("create", "loader finished");

        if (cursor == null || cursor.getCount() < 1) {
            return;
        }
        if (cursor.moveToFirst()) {
            int id = cursor.getColumnIndex(MapsEntry._ID);
            int nameColumnIndex = cursor.getColumnIndex(MapsEntry.COLUMN_MAP_NAME);
            int polygonIdColumnIndex = cursor.getColumnIndex(MapsEntry.COLUMN_POLYGON_ID);
            int coordinatesColumnIndex = cursor.getColumnIndex(MapsEntry.COLUMN_MAP_COORDINATES);
            int areaColumnIndex = cursor.getColumnIndex(MapsEntry.COLUMN_MAP_AREA);
            int cropColumIndex = cursor.getColumnIndex(MapsEntry.COLUMN_CROP_TYPE);

            MAP_ID = cursor.getInt(id);
            name = cursor.getString(nameColumnIndex);
            POLY_ID = cursor.getString(polygonIdColumnIndex);
            coordinateString = cursor.getString(coordinatesColumnIndex);
            area = cursor.getDouble(areaColumnIndex);
            crop = cursor.getString(cropColumIndex);
            loadData();
            makeTileOverlay();
        }
        if (name.length() > 5) {
            if (name.substring(0, 5).equals("Field")) {
                String modified_name = getString(R.string.field) + name.substring(5, name.length());
                getActionBar().setTitle(modified_name);
            } else {
                getActionBar().setTitle(name);
            }
        } else {
            getActionBar().setTitle(name);
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    public void loadData() {
        String coordinates = coordinateString.substring(8, coordinateString.length() - 2);
        String[] myList = coordinates.split(";\nlat/lng:");

        latLngs1 = new ArrayList<>();

        for (int i = 0; i < myList.length; i++) {
            String s = myList[i].substring(2, myList[i].length() - 1);
            String[] s1 = s.split(",");
            double lat = Double.parseDouble(s1[0]);
            double lng = Double.parseDouble(s1[1]);
            LatLng latLng = new LatLng(lat, lng);
            latLngs1.add(latLng);
            mMap.addMarker(new MarkerOptions().position(latLng).visible(false));
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs1.get(1), 15));
        Polygon polygon = mMap.addPolygon(new PolygonOptions()
                .addAll(latLngs1)
                .strokeColor(Color.RED)
                .fillColor(Color.TRANSPARENT));
    }

    private void makeTileOverlay() {

        ndviJsonResponse = sharedPref.getString(POLY_ID, null);
        if ((ndviJsonResponse == null || (ndviJsonCheckTime - FINAL_DATE >= 432000)) && LocaleUtils.isNetworkAvailable(getApplicationContext())) {
            NdviDownloadTask ndviDownloadTask = new NdviDownloadTask();
            ndviDownloadTask.execute();
        }
        else{
            unixDates = NdviUtils.getAllDates(ndviJsonResponse);
            standardDates = NdviUtils.getAllStandardDates(unixDates);
            downloadLatestAbsentImage(0);                                                                // download most recent image
            setupSpinner(standardDates);
        }

    }

    private TileOverlay loadOverlay(String imageUrl) {
        final String myUrl = imageUrl.replace("{z}/{x}/{y}", "%d/%d/%d");

        TileOverlay tileOverlay = mMap.addTileOverlay(new CachingUrlTileProvider(this, 256, 256) {
            @Override
            public String getTileUrl(int x, int y, int z) {
                String s = String.format(Locale.US, myUrl, z, x, y);
                return s;
            }
        }.createTileOverlayOptions());

        //TileOverlay tileOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(tileProvider));

        return tileOverlay;
    }

    private void deleteField() {
        //delete from agromonitoring
        HttpDeleteTask httpDeleteTask = new HttpDeleteTask();
        httpDeleteTask.execute(POLY_ID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();

        inflater.inflate(R.menu.menu_map_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.refresh:
                refreshData();
                return true;

            case R.id.ndvi_clip:
                showClipPopup();
                return true;

            case R.id.field_details:
                showPopup();
                return true;

            case R.id.map_edit:
                openEditMapActivity();
                return true;

            case R.id.map_delete:
                deleteField();
                return true;

            default:
                break;
        }

        return false;
    }

    private class HttpDeleteTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... polygonID) {
            return PolygonUtils.HttpDelete(polygonID[0], getString(R.string.Agro_API_Key));
        }

        @Override
        protected void onPostExecute(String result) {
            //delete from firebase
            usersDatabase.child("Map Data").child(name).removeValue();

            //delete from local database
            if (mCurrentMapUri != null) {
                String selection = MapsEntry._ID + "=?";
                String[] selectionArgs = new String[]{String.valueOf(ContentUris.parseId(mCurrentMapUri))};
                int rowsDeleted = getContentResolver().delete(MapsEntry.CONTENT_URI, selection, selectionArgs);
            }

            editor.remove("stats"+POLY_ID).apply(); //delete stats of the field stored
            editor.remove(POLY_ID).apply();         // delete ndvi json response
            editor.remove(POLY_ID+"_weather");      // delete weather data of the field

            // delete all image files for the field
            String path = getApplicationContext().getFilesDir().getAbsolutePath();
            File directory = new File(path);
            for (File f : directory.listFiles()) {
                if (f.getName().startsWith(POLY_ID)) {
                    f.delete();
                }
            }
            loadingIndicator.setVisibility(View.GONE);
            finish();
            Toast.makeText(MapViewActivity.this, getString(R.string.deleted_from_agro), Toast.LENGTH_LONG).show();
        }
    }


    public class NdviDownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... urls) {
            ndviJsonResponse = NdviUtils.polygonNdviResponse(POLY_ID, START_DATE, FINAL_DATE, getString(R.string.Agro_API_Key));
            return ndviJsonResponse;
        }

        @Override
        protected void onPostExecute(String result) {
            loadingIndicator.setVisibility(View.GONE);
            if (ndviJsonResponse != null){
                Toast.makeText(getApplicationContext(), getString(R.string.got_json_response), Toast.LENGTH_LONG).show();

                //saving ndvi json response
                editor.putString(POLY_ID, ndviJsonResponse);
                editor.apply();

                //get dates from json response
                unixDates = NdviUtils.getAllDates(ndviJsonResponse);

                //saving most recent date
                ndviJsonCheckTime = unixDates.get(0);
                editor.putLong(NDVIJSONCHECKTIME,ndviJsonCheckTime);

                //convert unix to standard dates
                standardDates = NdviUtils.getAllStandardDates(unixDates);
                downloadLatestAbsentImage(0);                                                                // download most recent image
                setupSpinner(standardDates);
            }
            else
                Toast.makeText(getApplicationContext(), getString(R.string.api_not_responding), Toast.LENGTH_LONG).show();
        }
    }


    private void openEditMapActivity() {
        Intent viewMap = new Intent(MapViewActivity.this, EditMapActivity.class);
        Uri currentMapUri = ContentUris.withAppendedId(MapsEntry.CONTENT_URI, MAP_ID);
        viewMap.setData(currentMapUri);
        startActivity(viewMap);
        finish();
    }

    PopupWindow popupWindow, popupClip;
    LayoutInflater layoutInflater;
    private TextView fieldNameTextView, fieldAreaTextView, fiedIDTextView, fieldCropTextView;

    public void showPopup() {

        layoutInflater = (LayoutInflater) getBaseContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        View customView = layoutInflater.inflate(R.layout.details_pop_up, null, false);
        popupWindow = new PopupWindow(customView, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        FrameLayout frameLayout = this.findViewById(R.id.map_view_frame);
        popupWindow.showAtLocation(frameLayout, Gravity.CENTER, 0, 0);

        fieldNameTextView = customView.findViewById(R.id.field_name);
        fieldAreaTextView = customView.findViewById(R.id.field_area);
        fieldCropTextView = customView.findViewById(R.id.field_crop);
        fiedIDTextView = customView.findViewById(R.id.field_id);
        ImageView closeButton = customView.findViewById(R.id.close_popup);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });


        String cropType;

        fieldNameTextView.setText(name);
        fiedIDTextView.setText(POLY_ID);
        fieldAreaTextView.setText("" + Math.round(area) + getString(R.string.acre));
        switch (crop) {
            case "Corn":
                cropType = getString(R.string.corn);
                break;
            case "Wheat":
                cropType = getString(R.string.wheat);
                break;
            case "Rice":
                cropType = getString(R.string.rice);
                break;
            case "Cotton":
                cropType = getString(R.string.cotton);
                break;
            case "Sugarcane":
                cropType = getString(R.string.sugarcane);
                break;
            case "Potato":
                cropType = getString(R.string.potato);
                break;
            case "Other Crop":
                cropType = getString(R.string.other_crop);
                break;
            default:
                cropType = getString(R.string.other_crop);
        }
        fieldCropTextView.setText(cropType);
    }


    private LatLng myLatLng;
    private LocationManager locationManager;
    private Marker currentMarker;

    public void CheckPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
        }
    }


    public void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, this);
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "gps not enabled", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        addCurrentMarker();
        Log.v("map", myLatLng.toString());
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private void addCurrentMarker() {

        if (myLatLng != null) {
            if (currentMarker != null) currentMarker.remove();
            Drawable circleDrawable = getResources().getDrawable(R.drawable.current_location_24dp);
            BitmapDescriptor current = getMarkerIconFromDrawable(circleDrawable);
            currentMarker = mMap.addMarker(new MarkerOptions().position(myLatLng).title("You").icon(current));
        }
    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private class DownloadImage extends AsyncTask<String, Void, Bitmap> {
        private String TAG = "DownloadImage";

        private Bitmap downloadImageBitmap(String sUrl) {
            Bitmap bitmap = null;
            try {
                InputStream inputStream = new URL(sUrl).openStream();   // Download Image from URL
                bitmap = BitmapFactory.decodeStream(inputStream);       // Decode Bitmap
                inputStream.close();
            } catch (Exception e) {
                Log.d(TAG, "Exception 1, Something went wrong!");
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            return downloadImageBitmap(params[0]);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Bitmap result) {                                               // store latest image for list display
            if (moveFlag == 0) {
                DownloadImageUtils.saveImage(getApplicationContext(), result, POLY_ID, 0);
            } else {
                DownloadImageUtils.saveImage(getApplicationContext(), result, POLY_ID, unixDates.get(moveFlag));
            }
        }
    }


    int n;
    ProgressBar popupWaitDownload;
    ImageView ndvis;

    public void showClipPopup() {
        n = unixDates.size();

        layoutInflater = (LayoutInflater) getBaseContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        View customView = layoutInflater.inflate(R.layout.popup_ndvi_clip, null, false);
        popupClip = new PopupWindow(customView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, true);
        FrameLayout frameLayout = this.findViewById(R.id.map_view_frame);
        popupClip.showAtLocation(frameLayout, Gravity.CENTER, 0, 0);
        ndvis = customView.findViewById(R.id.ndvi_images);
        ImageView closeButton = customView.findViewById(R.id.close_clip);
        final TextView ndviDate = customView.findViewById(R.id.ndvi_date);
        final ImageView forward = customView.findViewById(R.id.move_forward);
        final ImageView backward = customView.findViewById(R.id.move_backward);
        popupWaitDownload = customView.findViewById(R.id.waiting_indicator);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupClip.dismiss();
            }
        });
        ndvis.setImageBitmap(DownloadImageUtils.loadImageBitmap(getBaseContext(), POLY_ID, 0));
        downloadAbsentImage(0);
        ndviDate.setText(standardDates.get(0));

        if (moveFlag == 0) forward.setVisibility(View.INVISIBLE);
        backward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (moveFlag < unixDates.size()) {
                    moveFlag++;
                    forward.setVisibility(View.VISIBLE);
                    downloadAbsentImage(moveFlag);
                    if (moveFlag == unixDates.size() - 1) {
                        backward.setVisibility(View.INVISIBLE);
                    }
                    ndvis.setImageBitmap(DownloadImageUtils.loadImageBitmap(getBaseContext(), POLY_ID, unixDates.get(moveFlag)));
                    ndviDate.setText(standardDates.get(moveFlag));
                }
            }
        });
        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (moveFlag > 0) {
                    moveFlag--;
                    downloadAbsentImage(moveFlag);
                    backward.setVisibility(View.VISIBLE);

                    if (moveFlag == 0) {
                        forward.setVisibility(View.INVISIBLE);
                        ndvis.setImageBitmap(DownloadImageUtils.loadImageBitmap(getBaseContext(), POLY_ID, 0));
                        ndviDate.setText(standardDates.get(moveFlag));
                    } else {
                        ndvis.setImageBitmap(DownloadImageUtils.loadImageBitmap(getBaseContext(), POLY_ID, unixDates.get(moveFlag)));
                        ndviDate.setText(standardDates.get(moveFlag));
                    }
                }

            }
        });
    }

    private void downloadAbsentImage(final int flag) {
        String bitmapImageUrl = NdviUtils.getNdviBitmapUrl(ndviJsonResponse, unixDates.size() - flag - 1);
        bitmapImageUrl = bitmapImageUrl + "&paletteid=4";
        String path;
        if (flag == 0) {
            path = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + POLY_ID + "_" + 0 + ".png";

        } else {
            path = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + POLY_ID + "_" + unixDates.get(flag) + ".png";
        }
        File file = new File(path);
        if ((!file.exists() || file.length()<500) && LocaleUtils.isNetworkAvailable(getApplicationContext())) {
            new DownloadImage().execute(bitmapImageUrl);
            Picasso.get().load(bitmapImageUrl).into(ndvis);
        }

    }

    private void downloadLatestAbsentImage(final int flag) {
        String bitmapImageUrl = NdviUtils.getNdviBitmapUrl(ndviJsonResponse, unixDates.size() - flag - 1);
        bitmapImageUrl = bitmapImageUrl + "&paletteid=4";
        String path;
        if (flag == 0) {
            path = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + POLY_ID + "_" + 0 + ".png";

        } else {
            path = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + POLY_ID + "_" + unixDates.get(flag) + ".png";
        }
        File file = new File(path);
        if ((!file.exists() || file.length()<500) && LocaleUtils.isNetworkAvailable(getApplicationContext()) && flag!=0) {
            new DownloadImage().execute(bitmapImageUrl);
        }
        else{
            new DownloadImage().execute(bitmapImageUrl);
        }

    }

    private void refreshData() {

        if (LocaleUtils.isNetworkAvailable(this)) {
            NdviDownloadTask ndviDownloadTask = new NdviDownloadTask();
            ndviDownloadTask.execute();
        } else {
            Toast.makeText(this, getString(R.string.network_not_available), Toast.LENGTH_SHORT).show();
        }

    }

    private void downloadNdviImages() {
        String path;
        int n = unixDates.size();

        for (int i = 0; i < n; i++) {
            final String bitmapImageUrl = NdviUtils.getNdviBitmapUrl(ndviJsonResponse, unixDates.size() - i - 1)+ "&paletteid=4";
            if (i == 0) {
                path = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + POLY_ID + "_" + 0 + ".png";
            } else {
                path = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + POLY_ID + "_" + unixDates.get(i) + ".png";
            }
            File file = new File(path);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    new DownloadAllImages().execute(bitmapImageUrl);
                }
            });
            thread.start();

        }

    }

    private class DownloadAllImages extends AsyncTask<String, Void, Bitmap> {
        private String TAG = "DownloadImage";
        private Bitmap downloadImageBitmap(String sUrl) {
            Bitmap bitmap = null;
            try {
                InputStream inputStream = new URL(sUrl).openStream();   // Download Image from URL
                bitmap = BitmapFactory.decodeStream(inputStream);       // Decode Bitmap
                inputStream.close();
            } catch (Exception e) {
                Log.d(TAG, "Exception 1, Something went wrong!");
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            return downloadImageBitmap(params[0]);
        }

        protected void onPostExecute(Bitmap result) {                                               // store latest image for list display
            if(imageFlag==0){
                DownloadImageUtils.saveImage(getApplicationContext(), result, POLY_ID,0);
            }
            else{
                DownloadImageUtils.saveImage(getApplicationContext(), result, POLY_ID,unixDates.get(imageFlag));
            }
            imageFlag++;
        }
    }

}
