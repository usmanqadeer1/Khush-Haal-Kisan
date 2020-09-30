package com.lums.narl.talkingFields;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
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
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.maps.android.SphericalUtil;
import com.lums.narl.talkingFields.MapsDatabase.MapsContract.MapsEntry;
import com.lums.narl.talkingFields.MapsDatabase.MapsDbHelper;
import com.lums.narl.talkingFields.Utils.DownloadImageUtils;
import com.lums.narl.talkingFields.Utils.LocaleUtils;
import com.lums.narl.talkingFields.Utils.NdviUtils;
import com.lums.narl.talkingFields.Utils.PolygonUtils;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

//import android.location.LocationListener;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        com.google.android.gms.location.LocationListener,
        LoaderManager.LoaderCallbacks<Cursor>, DatePickerDialog.OnDateSetListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    Uri mCurrentMapUri;
    private GoogleMap mMap;
    private Button clear_button, okButton, saveButton, cancelButton;
    private EditText fieldName;
    private TextView cropField, areaField, idField, dateField;
    TextView agroResponse;
    private String mapName, coordinates,POLY_ID,cropType;
    public double areaMap;
    private ArrayList<LatLng> latLngs;
    PopupWindow popupWindow;


    private Location mLocation;
    private LocationManager locationManager;
    private LocationRequest mLocationRequest;
    private LatLng myLatLng;
    Polygon polygon;

    public static final String user_data = "USER_DATA";
    private static final String TAG = "MAPS_ACTIVITY";
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private String userID;
    private DatabaseReference usersDatabase;
    private Spinner mCropSpinner;
    private TextView dateTextView;
    private String AGRO_API_KEY, sowingDate;
    private Button currentLocation;
    private Marker currentMarker;
    private String ndviJsonResponse;
    private ArrayList<Long> unixDates;
    private Button datePicker;
    private GoogleApiClient mGoogleApiClient;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private ProgressBar loadingIndicator;
    private long FINAL_DATE = System.currentTimeMillis() / 1000L;
    private long START_DATE = 1451606400;
    private int imageFlag =0;
    private Handler mHandler;
    PopupWindow popupWaitWindow;
    LayoutInflater layoutInflater;
    Marker m1, m2, m3, m4;
    LatLng l1, l2, l3, l4;
    private ShowcaseView showcaseView;
    private int contador = 0;
    private Target t1, t2, t3, t4, t5, t6, t7;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleUtils.loadLocale(this);
        setContentView(R.layout.activity_maps);
        setTitle(R.string.app_name);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPref.edit();

        int count = getTotalNumberOfFields()+1;
        /*if(mapsList!=null){
            count = mapsList.getCount() + 1;
        }*/

        mapName = "Field" + " " + count;

        clear_button = findViewById(R.id.clear);
        okButton = findViewById(R.id.Save);
        mCropSpinner = findViewById(R.id.spinner_crop);
        agroResponse = findViewById(R.id.agro_response);
        currentLocation = findViewById(R.id.current_location);
        loadingIndicator = findViewById(R.id.loading_indicator);
        datePicker = findViewById(R.id.date_picker);
        dateTextView = findViewById(R.id.date_view);
        latLngs = new ArrayList<>();
        mHandler = new Handler();
        boolean isActivityOpened = sharedPref.getBoolean("maps_activity_opened", false);
        if(!isActivityOpened){
            showcase();
            editor.putBoolean("maps_activity_opened",true);
            editor.apply();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setupSpinner();
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);                                                  //display back button

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        userID = firebaseUser.getUid();
        usersDatabase = FirebaseDatabase.getInstance().getReference();
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isConditionFulfilled() && LocaleUtils.isNetworkAvailable(MapsActivity.this)){
                    okButton.setEnabled(false);
                    clear_button.setVisibility(View.GONE);
                    mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(LatLng latLng) {
                        }
                    }); // disable maps editing
                    getPolygonID(); // get polygon id from agro monitoring

                }
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        locationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        if(myLatLng!=null)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15));
        currentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLocation();
                CheckPermission();
                addCurrentMarker();
                if(myLatLng!=null)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, 15));

            }
        });

        datePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });
        getLoaderManager().initLoader(0, null, this);

    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(POLY_ID!=null && POLY_ID.length()==24){
            HttpDeleteTask httpDeleteTask = new HttpDeleteTask();
            httpDeleteTask.execute(POLY_ID);
        }

    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, monthOfYear, dayOfMonth);

        SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy");
        SimpleDateFormat format1 = new SimpleDateFormat("dd/MM/yyyy");
        String strDate = format1.format(calendar.getTime());
        this.dateTextView.setText(strDate);

        sowingDate = format1.format(calendar.getTime());
    }

    @Override
    public boolean onNavigateUp(){
        finish();
        return true;
    }

    private void setupSpinner() {

        ArrayAdapter cropSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_crop_options, android.R.layout.simple_spinner_dropdown_item);

        cropSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        mCropSpinner.setAdapter(cropSpinnerAdapter);
        mCropSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position)
                {
                    case 1: cropType ="Corn";break;
                    case 2: cropType ="Wheat";break;
                    case 3: cropType ="Rice";break;
                    case 4: cropType ="Cotton";break;
                    case 5: cropType ="Sugarcane";break;
                    case 6: cropType ="Potato";break;
                    case 7: cropType ="Other Crop";break;
                    default: cropType = "Select Crop";break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void onMapSearch(View view) {
        EditText locationSearch = findViewById(R.id.editText);
        String location = locationSearch.getText().toString();
        List<Address> addressList = null;

        if (location != null || !location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 10);

            } catch (IOException e) {
                e.printStackTrace();
            }
            if (addressList.size() > 0){
                Address address = addressList.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.addMarker(new MarkerOptions().position(latLng).title(location));
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }

        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng pak = new LatLng(31.5, 74);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pak, 15));
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mMap.addMarker(new MarkerOptions().position(latLng).title(" " + latLng.longitude + "," + " " + latLng.latitude));
                latLngs.add(latLng);
                if (latLngs.size() >= 3) {
                    if(polygon!=null) polygon.remove();
                    polygon = mMap.addPolygon(new PolygonOptions()
                            .addAll(latLngs)
                            .strokeColor(Color.RED)
                            .fillColor(0x5500ff00));
                }

            }
        });

        clear_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearMap();
            }

        });
    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public void CheckPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
        }
    }

    private void addCurrentMarker(){

        if (myLatLng != null){
            if(currentMarker!=null) currentMarker.remove();
            Drawable circleDrawable = getResources().getDrawable(R.drawable.current_location_24dp);
            BitmapDescriptor current= getMarkerIconFromDrawable(circleDrawable);
            currentMarker=mMap.addMarker(new MarkerOptions().position(myLatLng).title("You").icon(current));
        }
    }


    public Boolean isConditionFulfilled(){
        if (latLngs.size() < 3) {
            Toast.makeText(this, getString(R.string.make_closed_figure), Toast.LENGTH_SHORT).show();
            return false;
        }
        areaMap = SphericalUtil.computeArea(latLngs)*2.47/10000;

        if (areaMap < 2.5) {
            Toast.makeText(this, getString(R.string.small_field), Toast.LENGTH_SHORT).show();
            clearMap();
            return false;
        }
        if(areaMap > 100) {
            Toast.makeText(this, getString(R.string.large_field), Toast.LENGTH_SHORT).show();
            clearMap();
            return false;
        }
        if(cropType == null || cropType.equals("Select Crop")){
            Toast.makeText(this, getString(R.string.crop_not_selected), Toast.LENGTH_SHORT).show();
            return false;
        }

        if(sowingDate == null){
            Toast.makeText(this, "Select a date of sowing", Toast.LENGTH_SHORT).show();
            return false;
        }


        return true;
    }

    public void getPolygonID(){
        HTTPAsyncTask httpAsyncTask = new HTTPAsyncTask();
        httpAsyncTask.execute(getString(R.string.Agro_API_Key));
    }

    public void saveMap() {
        ContentValues values = new ContentValues();
        coordinates = "";

        for (int i = 0; i < latLngs.size(); i++) {
            coordinates = coordinates + latLngs.get(i).toString() + ";\n";
        }

        mapName = fieldName.getText().toString();

        //saving in android database
        values.put(MapsEntry.COLUMN_MAP_NAME, mapName);
        values.put(MapsEntry.COLUMN_MAP_COORDINATES, coordinates);
        values.put(MapsEntry.COLUMN_MAP_AREA, areaMap);
        values.put(MapsEntry.COLUMN_POLYGON_ID,POLY_ID);
        values.put(MapsEntry.COLUMN_MAP_DATE,sowingDate);
        values.put(MapsEntry.COLUMN_CROP_TYPE,cropType);

        saveOnFirebase();
        String selection = MapsEntry._ID + "=?";
        Uri newUri = getContentResolver().insert(MapsEntry.CONTENT_URI, values);

    }

    private void saveOnFirebase() {
        MapField mapField = new MapField(mapName,POLY_ID,coordinates,areaMap,sowingDate,cropType);
        usersDatabase.child(firebaseUser.getUid()).child("Map Data").child(mapName).setValue(mapField);
    }

    private void  clearMap(){
        mMap.clear();
        latLngs.clear();
        addCurrentMarker();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {MapsEntry._ID,
                MapsEntry.COLUMN_POLYGON_ID,
                MapsEntry.COLUMN_MAP_NAME,
                MapsEntry.COLUMN_MAP_COORDINATES,
                MapsEntry.COLUMN_MAP_AREA,
                MapsEntry.COLUMN_MAP_DATE,
                MapsEntry.COLUMN_CROP_TYPE
        };

        String selection = MapsEntry._ID + "=?";

        if (mCurrentMapUri != null) {
            String[] selectionArgs = new String[]{String.valueOf(ContentUris.parseId(mCurrentMapUri))};

            return new CursorLoader(this,
                    MapsEntry.CONTENT_URI,                                                          // The content URI of the words table
                    projection,                                                                     // The columns to return for each row
                    selection,                                                                      // Selection criteria
                    selectionArgs,                                                                  // Selection criteria
                    null);                                                                // The sort order for the returned rows
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }
        if (cursor.moveToFirst()) {
            int nameColumnIndex = cursor.getColumnIndex(MapsEntry.COLUMN_MAP_NAME);
            int coordinatesColumnIndex = cursor.getColumnIndex(MapsEntry.COLUMN_MAP_COORDINATES);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    //get polygon id
    private class HTTPAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showWaitWindow();
        }

        @Override
        protected String doInBackground(String... apiKey) {

            try {
                try {
                    String s = PolygonUtils.HttpPost(apiKey[0], userID, latLngs);
                    if(s == null) return null;
                    return PolygonUtils.getPolygonId(s);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return "Error!";
                }
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if(result != null){
                Toast.makeText(getApplicationContext(), getString(R.string.published_to_agro), Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(getApplicationContext(), getString(R.string.could_not_publish_coordinates), Toast.LENGTH_SHORT).show();
            }
            agroResponse.setText(result);
            POLY_ID = result;

            // check if polygon ID is null and has length = 24
            if(POLY_ID!=null){
                if(POLY_ID.length()==24){
                    popupWaitWindow.dismiss();
                    showEditPopup();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Could not connect to server",Toast.LENGTH_SHORT);
                    finish();
                }
            }

        }
    }

    //download current json response of ndvi
    public class NdviDownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... urls) {
            ndviJsonResponse = NdviUtils.polygonNdviResponse(POLY_ID,START_DATE,FINAL_DATE,getString(R.string.Agro_API_Key));
            return ndviJsonResponse;
        }
        @Override
        protected void onPostExecute(String result) {
            if (ndviJsonResponse != null) {
                Toast.makeText(getApplicationContext(), getString(R.string.got_json_response), Toast.LENGTH_SHORT).show();
                editor.putString(POLY_ID,ndviJsonResponse);
                editor.apply();
                saveMap();
                NDVIAllImagesDownloadTask ndviAllImagesDownloadTask = new NDVIAllImagesDownloadTask();
                ndviAllImagesDownloadTask.execute(POLY_ID);

            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.api_not_responding), Toast.LENGTH_SHORT).show();
            }
        }
    }

    //download ndvi images for all previous dates
    private class NDVIAllImagesDownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(ndviJsonResponse!=null)
                unixDates = NdviUtils.getAllDates(ndviJsonResponse);
        }

        @Override
        protected String doInBackground(String... urls) {
            String path;
            int n = unixDates.size();

            for(int i = 0; i<n; i++){
                String bitmapImageUrl = NdviUtils.getNdviBitmapUrl(ndviJsonResponse,unixDates.size()-i-1);
                bitmapImageUrl = bitmapImageUrl+ "&paletteid=4";
                if(i==0){
                    path = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + POLY_ID+"_"+0+".png";
                }
                else{
                    path = getApplicationContext().getFilesDir().getAbsolutePath() + "/" + POLY_ID+"_"+unixDates.get(i)+".png";
                }
                File file = new File(path);
                new DownloadImage().execute(bitmapImageUrl);
                // try{ Thread.sleep(3000); }catch(InterruptedException e){ }
            }

             return null;
        }
        @Override
        protected void onPostExecute(String result) {
            popupWaitWindow.dismiss();
            finish();
        }
    }

    //delete task
    private class HttpDeleteTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... polygonID) {
            // params comes from the execute() call: params[0] is the url.
            return PolygonUtils.HttpDelete(polygonID[0],getString(R.string.Agro_API_Key));
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
        }
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

        protected void onPostExecute(Bitmap result) {                                               // store latest image for list display
            if(imageFlag==0){
                // image name = PolygonId from AgroMonitoring_0
                DownloadImageUtils.saveImage(getApplicationContext(), result, POLY_ID,0);
            }
            else{
                // image name = (PolygonId from AgroMonitoring_) + (date in unix format)
                DownloadImageUtils.saveImage(getApplicationContext(), result, POLY_ID,unixDates.get(imageFlag));
            }
            imageFlag++;
        }
    }

    private void showWaitWindow(){
        layoutInflater =(LayoutInflater)getBaseContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        View customView = layoutInflater.inflate(R.layout.popup_wait,null,false);
        popupWaitWindow = new PopupWindow(customView, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,true);
        FrameLayout frameLayout = this.findViewById(R.id.maps_frame);
        popupWaitWindow.showAtLocation(frameLayout, Gravity.CENTER, 0, 0);
    }

    private void showDatePicker(){
        Calendar calendar = Calendar.getInstance();
        int thisYear = calendar.get(Calendar.YEAR);
        int thisMonth = calendar.get(Calendar.MONTH);
        int thisDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(MapsActivity.this, MapsActivity.this, 2013, 2, 18);
        dialog.updateDate(thisYear, thisMonth, thisDay);
        dialog.show();
    }

    public void showEditPopup() {

        layoutInflater = (LayoutInflater) getBaseContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        View customView = layoutInflater.inflate(R.layout.save_field_form, null);
        popupWindow = new PopupWindow(customView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, true);
        FrameLayout frameLayout = this.findViewById(R.id.maps_frame);
        popupWindow.showAtLocation(frameLayout, Gravity.CENTER, 0, 0);

        cancelButton = customView.findViewById(R.id.cancel_update);
        saveButton = customView.findViewById(R.id.update);
        fieldName = customView.findViewById(R.id.edit_field_name);
        cropField= customView.findViewById(R.id.field_crop);
        dateField = customView.findViewById(R.id.field_date);
        areaField = customView.findViewById(R.id.field_area);
        idField = customView.findViewById(R.id.field_id);

        fieldName.setText(mapName);
        idField.setText(POLY_ID);
        areaField.setText("" + Math.round(areaMap*10)/10.0+" " + getString(R.string.acre));
        cropField.setText(cropType);
        dateField.setText(sowingDate);

        //close the popup window on button click
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                HttpDeleteTask httpDeleteTask = new HttpDeleteTask();
                httpDeleteTask.execute(POLY_ID);
                finish();
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                while(checkIfNameExists(mapName)){
                    mapName = fieldName.getText().toString();
                    fieldName.setError("Field by this name already exists");
                }
                popupWindow.dismiss();
                showWaitWindow();
                NdviDownloadTask ndviDownloadTask = new NdviDownloadTask();
                ndviDownloadTask.execute();
            }
        });
    }


    private void showcase(){

        t1 = new ViewTarget(R.id.current_location, this);
        t2 = new ViewTarget(R.id.spinner_crop, this);
        t3 = new ViewTarget(R.id.date_picker, this);
        t4 = new ViewTarget(R.id.clear, this);
        t5 = new ViewTarget(R.id.map, this);
        t6 = new ViewTarget(R.id.editText,this);
        t7 = new ViewTarget(R.id.Save,this);

        showcaseView = new ShowcaseView.Builder(this).setTarget(Target.NONE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switch(contador){
                            case 0: showcaseView.setShowcase(t5, true);
                                showcaseView.setContentTitle(getString(R.string.maps_t5_title));
                                showcaseView.setContentText(getString(R.string.maps_t5_text));
                                makeTutorialPolygon();
                                break;
                            case 1: showcaseView.setShowcase(t1, true);
                                showcaseView.setContentTitle(getString(R.string.maps_t1_title));
                                showcaseView.setContentText(getString(R.string.maps_t1_text));
                                break;
                            case 2: showcaseView.setShowcase(t6, true);
                                showcaseView.setContentTitle(getString(R.string.maps_t6_title));
                                showcaseView.setContentText(getString(R.string.maps_t6_text));
                                break;
                            case 3: showcaseView.setShowcase(t3, true);
                                showcaseView.setContentTitle(getString(R.string.maps_t3_title));
                                showcaseView.setContentText(getString(R.string.maps_t3_text));
                                RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                lps.addRule(RelativeLayout.ALIGN_BASELINE);
                                lps.addRule(RelativeLayout.CENTER_HORIZONTAL);
                                showcaseView.setButtonPosition(lps);
                                break;
                            case 4: showcaseView.setShowcase(t4, true);
                                showcaseView.setContentTitle(getString(R.string.maps_t4_title));
                                showcaseView.setContentText(getString(R.string.maps_t4_text));
                                break;
                            case 5: showcaseView.setShowcase(t7, true);
                                showcaseView.setContentTitle(getString(R.string.maps_t7_title));
                                showcaseView.setContentText(getString(R.string.maps_t7_text));
                                break;
                            case 6: showcaseView.hide();
                                break;
                        }
                        contador++;
                    }
                }).setContentTitle("Tutorial").setContentText("").build();
        showcaseView.setShowcase(t2, true);
        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        lps.setMargins(20,20,20,20);
        showcaseView.setContentTitle(getString(R.string.maps_t2_title));
        showcaseView.setContentText(getString(R.string.maps_t2_text));
        showcaseView.setStyle(R.style.CustomShowcaseTheme);
        showcaseView.setButtonText("OK");

    }

    private void makeTutorialPolygon(){
        showcaseView.setEnabled(false);
        Drawable circleDrawable = getResources().getDrawable(R.drawable.hand_icon);
        final BitmapDescriptor hand = getMarkerIconFromDrawable(circleDrawable);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                l1 =new LatLng(31.500622,73.997482);
                latLngs.add(l1);
                m1 = mMap.addMarker(new MarkerOptions().position(l1).icon(hand));
            }
        },1000);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                m1.remove();
                mMap.addMarker(new MarkerOptions().position(l1));
                l2 =new LatLng(31.498853,73.998798);
                latLngs.add(l2);
                m2 = mMap.addMarker(new MarkerOptions().position(l2).icon(hand));
            }
        },2000);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                m2.remove();
                mMap.addMarker(new MarkerOptions().position(l2));
                l3 =new LatLng(31.500219,74.001974);
                latLngs.add(l3);
                m3  = mMap.addMarker(new MarkerOptions().position(l3).icon(hand));
            }
        },3000);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                m3.remove();
                mMap.addMarker(new MarkerOptions().position(l3));
                l4 = new LatLng(31.502281,74.001144);
                latLngs.add(l4);
                m4  = mMap.addMarker(new MarkerOptions().position(l4).icon(hand));


            }
        },4000);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                m4.remove();
                m4  = mMap.addMarker(new MarkerOptions().position(l4));
                mMap.addPolygon(new PolygonOptions()
                        .addAll(latLngs)
                        .strokeColor(Color.RED)
                        .fillColor(Color.BLUE));
                showcaseView.setEnabled(true);
            }
        },5000);

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                clearMap();
                showcaseView.setEnabled(true);
            }
        },6000);

    }

    @Override
    public void onLocationChanged(Location location) {
        myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        addCurrentMarker();
        Log.v("map", myLatLng.toString());
    }

    private boolean checkLocation() {
        if(!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                    }
                });
        dialog.show();
    }

    private boolean isLocationEnabled() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
            return;
        }

        startLocationUpdates();

        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(mLocation == null){
            startLocationUpdates();
        }
        if (mLocation != null) {

            // mLatitudeTextView.setText(String.valueOf(mLocation.getLatitude()));
            //mLongitudeTextView.setText(String.valueOf(mLocation.getLongitude()));
        } else {
            Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000)
                .setFastestInterval(2000);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
        Log.d("reque", "--->>>>");
    }

    private int getTotalNumberOfFields(){
        MapsDbHelper db = new MapsDbHelper(getApplicationContext());
        ArrayList<String> polygonNames = db.getAllNames();
        return polygonNames.size();
    }

    private boolean checkIfNameExists(String in){
        MapsDbHelper db = new MapsDbHelper(getApplicationContext());
        ArrayList<String> polygonNames = db.getAllNames();

        return polygonNames.contains(in);
    }
}