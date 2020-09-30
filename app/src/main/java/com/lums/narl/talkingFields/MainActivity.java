package com.lums.narl.talkingFields;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lums.narl.talkingFields.Main.MainOption;
import com.lums.narl.talkingFields.Main.OptionsAdapter;
import com.lums.narl.talkingFields.Tutorial.IntroActivity;
import com.lums.narl.talkingFields.Utils.LocaleUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lums.narl.talkingFields.MapsDatabase.MapsContract.MapsEntry;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String user_data = "USER_DATA";
    private static final String TAG = "MainActivity";
    private TextView Name, Phone;
    private String userName, phoneNumber;
    private String userData;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference usersDatabase;
    private String userID;
    private Boolean loginStatus;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private DrawerLayout drawer;
    private ProgressBar loadingIndicator;
    private ArrayList<MainOption> optionsList;
    private ListView optionsListView;
    private Handler handler;
    String maps_intent = "view_map";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleUtils.loadLocale(this);

        handler = new Handler();
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);
        Toolbar toolbar = findViewById(R.id.toolbar);
        loadingIndicator = findViewById(R.id.loading_indicator_refresh);
        optionsListView = findViewById(R.id.options_list);
        setSupportActionBar(toolbar);


        drawer = findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);//this.getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        loginStatus = sharedPref.getBoolean(getString(R.string.login_status),true);              // reading the login status
        userName = sharedPref.getString("user_name","");
        phoneNumber = sharedPref.getString("phone_number","");

        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        Name = headerView.findViewById(R.id.user_name);
        Phone = headerView.findViewById(R.id.phone_number);
        Name.setText(userName);
        Phone.setText(phoneNumber);
        navigationView.setNavigationItemSelectedListener(this);
        setupFirebaseAuth();

        optionsArrayInitialize();
        OptionsAdapter helpAdapter = new OptionsAdapter(this, optionsList);
        optionsListView.setAdapter(helpAdapter);

        optionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch(position){
                    case 0: startActivity(new Intent(MainActivity.this, FieldsActivity.class)); break;
                    case 1: startActivity(new Intent(MainActivity.this, PqnkActivty.class)); break;
                }
            }
        });
    }

    private void optionsArrayInitialize(){

        String[] optionsNames = getResources().getStringArray(R.array.array_main_options);
        int[] drawables = {R.drawable.my_fields,R.drawable.pqnk};
        optionsList = new ArrayList<>();
        for(int i = 0; i<optionsNames.length;i++){
            optionsList.add(new MainOption(optionsNames[i],drawables[i]));
        }

    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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
            startActivity(new Intent(this, IntroActivity.class));
        }

    }

    private void loadAllUserData(){
        usersDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                getMapData(dataSnapshot);
                User user = dataSnapshot.getValue(User.class);
                userName = user.getUsername();
                phoneNumber= user.getPhone();
                editor.putBoolean(getString(R.string.login_status), true);//writing loginStatus = true
                editor.putString("user_name",userName);
                editor.putString("phone_number",phoneNumber);
                editor.apply();
                Name.setText(userName);
                Phone.setText(phoneNumber);

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

        values.put(MapsEntry.COLUMN_MAP_NAME, mapName);
        values.put(MapsEntry.COLUMN_MAP_COORDINATES, coordinates);
        values.put(MapsEntry.COLUMN_MAP_AREA, areaMap);
        values.put(MapsEntry.COLUMN_POLYGON_ID,POLY_ID);
        values.put(MapsEntry.COLUMN_MAP_DATE,date);
        values.put(MapsEntry.COLUMN_CROP_TYPE,cropType);

        String selection = MapsEntry._ID + "=?";
        Uri newUri = getContentResolver().insert(MapsEntry.CONTENT_URI, values);

    }

    private void deleteMaps(){
        int rowsDeleted = getContentResolver().delete(MapsEntry.CONTENT_URI, null, null);
        Log.v("MainActivity", rowsDeleted + " all maps deleted from database");
    }

    private void showChangeLanguageDialog(){
        final String[] listItems = {"English","Deutsche","اردو"};
        AlertDialog.Builder mBuilder = new  AlertDialog.Builder(MainActivity.this);
        mBuilder.setTitle(getString(R.string.change_language));
        mBuilder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(i==0){
                    LocaleUtils.setLocale("en",getApplicationContext());
                    recreate();
                }
                if(i==1){
                    LocaleUtils.setLocale("de",getApplicationContext());
                    recreate();
                }
                if(i==2){
                    LocaleUtils.setLocale("ur",getApplicationContext());
                    recreate();
                }
                dialogInterface.dismiss();

            }
        });

        mBuilder.setCancelable(true);
        AlertDialog mDialog = mBuilder.create();
        mDialog.show();
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
            case R.id.refresh:
                if(LocaleUtils.isNetworkAvailable(this)){
                    deleteMaps();
                    loadAllUserData();
                    waitForDownload(5000);
                }else{
                    Toast.makeText(this, getString(R.string.network_not_available),Toast.LENGTH_SHORT).show();
                }

                return true;
            default:
                break;
        }
        return false;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        // Handle navigation view item clicks here.
        switch (item.getItemId()){

            case R.id.home:
                drawer.closeDrawers();
                return true;

            case R.id.dashboard:
                Intent graph = new Intent(MainActivity.this, graphActivity.class);
                graph.putExtra(user_data, userData);
                startActivity(graph);
                return true;

            case R.id.add_field:
                 Intent maps = new Intent(MainActivity.this, MapsActivity.class);
                 maps.putExtra(user_data, userData);
                 startActivity(maps);
                 return true;


            case R.id.help:
                startActivity(new Intent(this, HelpActivity.class));
                return true;

            case R.id.contact:
                sendEmail();
                return true;

            case R.id.language:
                showChangeLanguageDialog();
                return true;

            case R.id.logout:
                firebaseAuth.signOut();                                                             // sign out from firebase
                editor.putBoolean(getString(R.string.login_status), false);                                        // writing loginStatus = false so that user can load data agin on sign in
                editor.apply();
                deleteMaps();                                                                       //deleting all data from android sql database
                Intent home = new Intent(MainActivity.this,LoginActivity.class);
                startActivity(home);                                                                //switch to login screen
                finish();
                return true;

            default:
                break;
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void waitForDownload(final long millis){
        Thread displayIndicator = new Thread(new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() {
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
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadingIndicator.setVisibility(View.GONE);
                    }
                });
            }
        });
        displayIndicator.start();

    }

    private void sendEmail(){
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO,
                Uri.parse("mailto:" + Uri.encode("fieldstalking@gmail.com")));

        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
        startActivity(Intent.createChooser(emailIntent, "Send email via..."));
    }

}
