package com.lums.narl.talkingFields;

import android.app.ActionBar;
import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.lums.narl.talkingFields.MapsDatabase.MapsContract;
import com.lums.narl.talkingFields.Utils.LocaleUtils;
import com.lums.narl.talkingFields.Utils.PolygonUtils;
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

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

public class EditMapActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener, LoaderManager.LoaderCallbacks<Cursor> {


    public static double areaMap;
    Uri mCurrentMapUri;
    String name, coordinateString, crop;
    Double area;
    Button clear_button, saveButton;
    TextView agroResponse;
    ArrayList<LatLng> latLngs;
    LocationManager locationManager;
    LatLng myLatLng;
    PopupWindow popupWindow;
    Button cancelUpdate;
    Button update;
    EditText updateFieldName;
    String updatedCropType;
    Spinner spinnerUpdateCrop;
    LayoutInflater layoutInflater;
    ContentValues updateValues;
    Polygon polygon;
    private int MAP_ID;
    private GoogleMap mMap;
    private String POLY_ID = "";
    private String OLD_POLY_ID;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private DatabaseReference usersDatabase;
    private String userID;
    private String mapName, coordinates, cropType;
    private String AGRO_API_KEY;
    private Button currentLocation;
    private Marker currentMarker;
    private TextView fieldArea, fieldID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_map);

        LocaleUtils.loadLocale(this);
        setTitle(R.string.app_name);
        clear_button = findViewById(R.id.clear);
        saveButton = findViewById(R.id.Save);
        agroResponse = findViewById(R.id.agro_response);
        currentLocation = findViewById(R.id.current_location);
        latLngs = new ArrayList<>();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            userID = firebaseUser.getUid();
            usersDatabase = FirebaseDatabase.getInstance().getReference(userID);
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);//display back button

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isConditionFulfilled()) {
                    coordinates = "";
                    for (int i = 0; i < latLngs.size(); i++) {
                        coordinates = coordinates + latLngs.get(i).toString() + ";\n";
                    }
                    if (!coordinateString.equals(coordinates)) {                                      //if you do not want to change coordinates
                        getPolygonID();
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showPopup();
                        }
                    }, 2000);
                }
            }
        });

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

        Intent intent = getIntent();
        mCurrentMapUri = intent.getData();
        getLoaderManager().initLoader(0, null, this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng pak = new LatLng(31.5, 74);
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

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {MapsContract.MapsEntry._ID,
                MapsContract.MapsEntry.COLUMN_MAP_NAME,
                MapsContract.MapsEntry.COLUMN_POLYGON_ID,
                MapsContract.MapsEntry.COLUMN_MAP_COORDINATES,
                MapsContract.MapsEntry.COLUMN_MAP_AREA,
                MapsContract.MapsEntry.COLUMN_CROP_TYPE};

        String selection = MapsContract.MapsEntry._ID + "=?";
        String[] selectionArgs = new String[]{String.valueOf(ContentUris.parseId(mCurrentMapUri))};

        return new CursorLoader(this,
                MapsContract.MapsEntry.CONTENT_URI,          // The content URI of the words table
                projection,                     // The columns to return for each row
                selection,                  // Selection criteria
                selectionArgs,               // Selection criteria
                null);                 // The sort order for the returned rows
    }

    public Boolean isConditionFulfilled() {
        if (latLngs.size() < 3) {
            Toast.makeText(this, getString(R.string.make_closed_figure), Toast.LENGTH_LONG).show();
            return false;
        }
        areaMap = SphericalUtil.computeArea(latLngs) * 0.000247105;
        if (areaMap < 2.5) {
            Toast.makeText(this, getString(R.string.small_field), Toast.LENGTH_LONG).show();
            clearMap();
            return false;
        }
        if (areaMap > 100) {
            Toast.makeText(this, getString(R.string.large_field), Toast.LENGTH_LONG).show();
            clearMap();
            return false;
        }

        return true;
    }

    public void getPolygonID() {
        HTTPAsyncTask httpAsyncTask = new HTTPAsyncTask();
        httpAsyncTask.execute(getString(R.string.Agro_API_Key));
    }

    private void clearMap() {
        mMap.clear();
        latLngs.clear();
        addCurrentMarker();
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

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.v("create", "loader finished");

        if (cursor == null || cursor.getCount() < 1) {
            return;
        }
        if (cursor.moveToFirst()) {
            int id = cursor.getColumnIndex(MapsContract.MapsEntry._ID);
            int nameColumnIndex = cursor.getColumnIndex(MapsContract.MapsEntry.COLUMN_MAP_NAME);
            int polygonIdColumnIndex = cursor.getColumnIndex(MapsContract.MapsEntry.COLUMN_POLYGON_ID);
            int coordinatesColumnIndex = cursor.getColumnIndex(MapsContract.MapsEntry.COLUMN_MAP_COORDINATES);
            int areaColumnIndex = cursor.getColumnIndex(MapsContract.MapsEntry.COLUMN_MAP_AREA);
            int cropColumIndex = cursor.getColumnIndex(MapsContract.MapsEntry.COLUMN_CROP_TYPE);

            MAP_ID = cursor.getInt(id);
            name = cursor.getString(nameColumnIndex);
            POLY_ID = cursor.getString(polygonIdColumnIndex);
            OLD_POLY_ID = POLY_ID;
            coordinateString = cursor.getString(coordinatesColumnIndex);
            area = cursor.getDouble(areaColumnIndex);
            crop = cursor.getString(cropColumIndex);
            loadData();
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


        for (int i = 0; i < myList.length; i++) {
            String s = myList[i].substring(2, myList[i].length() - 1);
            String[] s1 = s.split(",");
            double lat = Double.parseDouble(s1[0]);
            double lng = Double.parseDouble(s1[1]);
            LatLng latLng = new LatLng(lat, lng);
            latLngs.add(latLng);
            mMap.addMarker(new MarkerOptions().position(latLng));
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.get(1), 15));
        Polygon polygon = mMap.addPolygon(new PolygonOptions()
                .addAll(latLngs)
                .strokeColor(Color.RED)
                .fillColor(Color.TRANSPARENT));
    }

    public void showPopup() {

        layoutInflater = (LayoutInflater) getBaseContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        View customView = layoutInflater.inflate(R.layout.edit_field_form, null);
        popupWindow = new PopupWindow(customView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, true);
        FrameLayout frameLayout = this.findViewById(R.id.map_edit_frame);
        popupWindow.showAtLocation(frameLayout, Gravity.CENTER, 0, 0);

        cancelUpdate = customView.findViewById(R.id.cancel_update);
        update = customView.findViewById(R.id.update);
        updateFieldName = customView.findViewById(R.id.update_field_name);
        spinnerUpdateCrop = customView.findViewById(R.id.spinner_update_crop);
        fieldArea = customView.findViewById(R.id.field_area);
        fieldID = customView.findViewById(R.id.field_id);

        updateFieldName.setText(name);
        fieldID.setText(POLY_ID);
        fieldArea.setText("" + Math.round(areaMap*10)/10.0 + getString(R.string.acre));
        setupSpinnerCropUpdate(spinnerUpdateCrop);

        //close the popup window on button click
        cancelUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
                deletePolygon(POLY_ID);
                finish();
            }
        });
        updateFieldDetails();
    }


    private void updateFieldDetails() {
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateValues = new ContentValues();
                String updatedFieldname = updateFieldName.getText().toString();

                if (updatedFieldname != null && !updatedFieldname.equals("") && !updatedCropType.equals("Select Crop")) {
                    crop = updatedCropType;

                    area = areaMap;
                    MapField mapField = new MapField(updatedFieldname, POLY_ID, coordinates, area,"", crop);

                    usersDatabase.child("Map Data").child(name).removeValue();
                    usersDatabase.child("Map Data").child(updatedFieldname).setValue(mapField);     //update on firebase
                    name = updatedFieldname;

                    updateValues.put(MapsContract.MapsEntry.COLUMN_MAP_NAME, updatedFieldname);
                    updateValues.put(MapsContract.MapsEntry.COLUMN_CROP_TYPE, crop);
                    updateValues.put(MapsContract.MapsEntry.COLUMN_MAP_COORDINATES, coordinates);
                    updateValues.put(MapsContract.MapsEntry.COLUMN_MAP_AREA, area);
                    updateValues.put(MapsContract.MapsEntry.COLUMN_POLYGON_ID, POLY_ID);
                    popupWindow.dismiss();

                    if (!coordinateString.equals(coordinates)) {                                      //if you do not want to change coordinates
                        deletePolygon(OLD_POLY_ID);
                    }
                    int rowsAffected = getContentResolver().update(MapsContract.MapsEntry.CONTENT_URI, updateValues, MapsContract.MapsEntry._ID + "=" + MAP_ID, null);
                    finish();

                } else {
                    Toast.makeText(EditMapActivity.this, "invalid data", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupSpinnerCropUpdate(Spinner spinner) {

        ArrayAdapter cropSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_crop_options, android.R.layout.simple_spinner_dropdown_item);

        cropSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinner.setAdapter(cropSpinnerAdapter);

        String[] crops = {"Select Crop", "Corn", "Wheat", "Rice", "Cotton", "Sugarcane", "Potato", "Other Crop"};
        for (int i = 0; i < crops.length; i++) {
            if (crop.equals(crops[i])) {
                spinner.setSelection(i);
            }
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1:
                        updatedCropType = "Corn";
                        break;
                    case 2:
                        updatedCropType = "Wheat";
                        break;
                    case 3:
                        updatedCropType = "Rice";
                        break;
                    case 4:
                        updatedCropType = "Cotton";
                        break;
                    case 5:
                        updatedCropType = "Sugarcane";
                        break;
                    case 6:
                        updatedCropType = "Potato";
                        break;
                    case 7:
                        updatedCropType = "Other Crop";
                        break;
                    default:
                        updatedCropType = "Select Crop";
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    private void deletePolygon(String id) {
        HttpDeleteTask httpDeleteTask = new HttpDeleteTask();
        httpDeleteTask.execute(id);

    }

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

    private class HTTPAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... apiKey) {

            try {
                try {
                    String s = PolygonUtils.HttpPost(apiKey[0], userID, latLngs);
                    if (s == null) return null;
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
            if (result != null) {
                Toast.makeText(getApplicationContext(), getString(R.string.published_to_agro), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.could_not_publish_coordinates), Toast.LENGTH_LONG).show();
            }
            agroResponse.setText(result);
            POLY_ID = result;
        }
    }

    private class HttpDeleteTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... polygonID) {
            // params comes from the execute() call: params[0] is the url.
            return PolygonUtils.HttpDelete(polygonID[0], getString(R.string.Agro_API_Key));
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
        }
    }
}
