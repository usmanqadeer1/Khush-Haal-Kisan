package com.lums.narl.talkingFields;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.lums.narl.talkingFields.MapsDatabase.MapsContract;
import com.lums.narl.talkingFields.Utils.LocaleUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FieldsActivity extends AppCompatActivity implements  LoaderManager.LoaderCallbacks<Cursor> {

    private MapsAdapter mapAdapter;
    public static ListView mapsList;
    private static final int MAP_LOADER = 0;
    private static final String user_data = "USER_DATA";
    private FirebaseUser firebaseUser;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference usersDatabase;
    private String userID;
    private Boolean loginStatus;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private ProgressBar loadingIndicator;
    private FloatingActionButton fab;
    Handler mHandler;
    private ShowcaseView showcaseView;
    private int contador = 0;
    private Target t1, t2, t3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleUtils.loadLocale(this);
        setTitle(getString(R.string.my_fields));
        setContentView(R.layout.activity_fields);

        mHandler = new Handler();
        loadingIndicator = findViewById(R.id.loading_indicator);
        fab = findViewById(R.id.fab2);
        mapsList = findViewById(R.id.maps_list);

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);//this.getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        loginStatus = sharedPref.getBoolean(getString(R.string.login_status),true);              // reading the login status
        mapAdapter = new MapsAdapter(this, null);
        mapsList.setAdapter(mapAdapter);
        addMap();
        viewMap();
        setupFirebaseAuth();
        boolean isActivityOpened = sharedPref.getBoolean("fields_activity_opened", false);
        if(!isActivityOpened){
            showcase();
            editor.putBoolean("fields_activity_opened",true);
            editor.apply();
        }
        getLoaderManager().initLoader(MAP_LOADER, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapAdapter.notifyDataSetChanged();
    }

    private void addMap(){
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent maps = new Intent(FieldsActivity.this, MapsActivity.class);
                startActivity(maps);
            }
        });
    }

    private void setupFirebaseAuth(){
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        mFirebaseDatabase=FirebaseDatabase.getInstance();
        userID = firebaseUser.getUid();
        usersDatabase = mFirebaseDatabase.getReference(userID);

        if(!loginStatus){                                                                           //If user logged in first time, loginStatus = false
            loadAllUserData();
            waitForDownload(5000);
        }
    }

    private void loadAllUserData(){
        usersDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                getMapData(dataSnapshot);
                User user = dataSnapshot.getValue(User.class);
                editor.putBoolean(getString(R.string.login_status), true);//writing loginStatus = true
                editor.apply();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getMapData(DataSnapshot dataSnapshot){

        for(DataSnapshot ds: dataSnapshot.child("Map Data").getChildren()){
            MapField mapField = ds.getValue(MapField.class);
            String coordinates = mapField.getCoordinates();

            String mapName = mapField.getFieldName();
            Double areaMap = mapField.getArea();
            String POLY_ID = mapField.getPolygonID();
            String cropType = mapField.getCropType();
            String date = mapField.getDate();
            insertInDatabase(mapName,coordinates,areaMap,POLY_ID, cropType, date);
        }
    }

    private void insertInDatabase(String mapName, String coordinates, Double areaMap, String POLY_ID, String cropType, String date){
        ContentValues values = new ContentValues();

        values.put(MapsContract.MapsEntry.COLUMN_MAP_NAME, mapName);
        values.put(MapsContract.MapsEntry.COLUMN_MAP_COORDINATES, coordinates);
        values.put(MapsContract.MapsEntry.COLUMN_MAP_AREA, areaMap);
        values.put(MapsContract.MapsEntry.COLUMN_POLYGON_ID,POLY_ID);
        values.put(MapsContract.MapsEntry.COLUMN_MAP_DATE,date);
        values.put(MapsContract.MapsEntry.COLUMN_CROP_TYPE,cropType);

        String selection = MapsContract.MapsEntry._ID + "=?";
        Uri newUri = getContentResolver().insert(MapsContract.MapsEntry.CONTENT_URI, values);

    }
    private void deleteMaps(){
        int rowsDeleted = getContentResolver().delete(MapsContract.MapsEntry.CONTENT_URI, null, null);
        Log.v("MainActivity", rowsDeleted + " all maps deleted from database");
    }

    private void viewMap(){
        mapsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent viewMap = new Intent (FieldsActivity.this, MapViewActivity.class);
                Uri currentMapUri = ContentUris.withAppendedId(MapsContract.MapsEntry.CONTENT_URI, id);
                viewMap.setData(currentMapUri);
                startActivity(viewMap);
            }
        });
        mapsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return false;
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {MapsContract.MapsEntry._ID,
                MapsContract.MapsEntry.COLUMN_MAP_NAME,
                MapsContract.MapsEntry.COLUMN_POLYGON_ID,
                MapsContract.MapsEntry.COLUMN_MAP_COORDINATES,
                MapsContract.MapsEntry.COLUMN_MAP_AREA,
                MapsContract.MapsEntry.COLUMN_MAP_DATE,
                MapsContract.MapsEntry.COLUMN_CROP_TYPE};

        return new CursorLoader(this,
                MapsContract.MapsEntry.CONTENT_URI,          // The content URI of the words table
                projection,                     // The columns to return for each row
                null,                  // Selection criteria
                null,               // Selection criteria
                null);                 // The sort order for the returned rows
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mapAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mapAdapter.swapCursor(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();

        inflater.inflate(R.menu.menu_main, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;

            case R.id.refresh:
                if(LocaleUtils.isNetworkAvailable(this)){
                    deleteMaps();
                    loadAllUserData();
                    waitForDownload(5000);
                }
                else{
                Toast.makeText(this, getString(R.string.network_not_available),Toast.LENGTH_SHORT).show();
            }

                return true;
            default:
                break;
        }
        return false;
    }

    private void waitForDownload(final long millis){
        Thread displayIndicator = new Thread(new Runnable() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadingIndicator.setVisibility(View.VISIBLE);
                    }
                });
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadingIndicator.setVisibility(View.GONE);
                    }
                });
            }
        });
        displayIndicator.start();

    }

    private void showcase(){
        t1 = new ViewTarget(R.id.fab2, this);
        t2 = new ViewTarget(R.id.maps_list, this);

        showcaseView = new ShowcaseView.Builder(this).setTarget(Target.NONE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switch(contador){
                            case 0: showcaseView.setShowcase(t2, true);
                                    showcaseView.setContentTitle(getString(R.string.field_t2_title));
                                    showcaseView.setContentText(getString(R.string.field_t2_text));
                                    break;
                            case 1: showcaseView.hide();
                                    break;
                        }
                        contador++;
                    }
                }).setContentTitle("Tutorial").setContentText("").build();

        showcaseView.setShowcase(t1, true);
        showcaseView.setContentTitle(getString(R.string.field_t1_title));
        showcaseView.setContentText(getString(R.string.field_t1_text));

        showcaseView.setButtonText("Ok");

        RelativeLayout.LayoutParams lps = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lps.addRule(RelativeLayout.ALIGN_BASELINE);
        lps.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lps.setMargins(20,20,20,20);

        showcaseView.setButtonPosition(lps);
    }

}
