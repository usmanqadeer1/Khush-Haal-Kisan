package com.lums.narl.talkingFields;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;
import com.lums.narl.talkingFields.MapsDatabase.MapsDbHelper;
import com.lums.narl.talkingFields.Utils.LocaleUtils;
import com.lums.narl.talkingFields.Utils.NdviUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class graphActivity extends AppCompatActivity {

    private long FINAL_DATE = System.currentTimeMillis() / 1000L;
    //    private long START_DATE = 1451606400;
    private long START_DATE = FINAL_DATE - 34214400; //1 year back from current date
    private long statsCheckTime;
    SimpleDateFormat sdf = new java.text.SimpleDateFormat("d MMM yyyy");

    private SharedPreferences.Editor editor;
    private SharedPreferences sharedPref;

    private ArrayList<String> polygonNames;
    private ArrayList<String> polygonIDs;
    private ArrayList<String> statsResponses;

    private ArrayList<Double> meanValues;
    private ArrayList<Double> stdValues;
    private ArrayList<Long> unixDates;
    private ArrayList<String> standardDates;

    private int number_of_fields;

    //shared preferences key
    public static String STATS_CHECK_TIME = "stats_check_time";
    // for each stat the key = stats+POLY_ID

    private Spinner spinner;
    private GraphView graphView1;
    private RadioButton showLastMonth, showLastYear, showMean, showStd;
    private ProgressBar loadingIndicator;
    private TextView warning;
    private LinearLayout graphFrame;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleUtils.loadLocale(this);
        setContentView(R.layout.activity_graph);
        setTitle(R.string.dashboard);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPref.edit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        //initialize UI items
        graphFrame = findViewById(R.id.graph_frame);
        spinner = findViewById(R.id.field_name_spinner);
        graphView1 = findViewById(R.id.graph);
        showLastMonth = findViewById(R.id.last_month);
        showLastYear = findViewById(R.id.last_year);
        showMean = findViewById(R.id.mean);
        showStd = findViewById(R.id.std);
        loadingIndicator = findViewById(R.id.loading_indicator);
        warning = findViewById(R.id.test);
        loadingIndicator.setIndeterminate(true);
        loadingIndicator.getIndeterminateDrawable().setColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.MULTIPLY);
        loadSpinnerData();

        statsResponses = new ArrayList<>();
        standardDates = new ArrayList<>();
        //initialize ArrayLists
        number_of_fields = polygonNames.size();

        // get statchecktime to get time when stat response was last updated
        statsCheckTime = sharedPref.getLong(STATS_CHECK_TIME,0);
        //get all stats responses from shared preferences
        for (int i = 0; i < number_of_fields; i++) {
            String stats = sharedPref.getString("stats" + polygonIDs.get(i), null);
            if (stats != null) {
                statsResponses.add(stats);
            }
        }

        if (!polygonNames.isEmpty()) {
            // download json stats response for all polygons again
            // if time has passed more than 5 days since last update
            // or if statsResponse is empty
            if (((statsResponses == null) || (statsCheckTime - FINAL_DATE >= 432000)) &&
                    LocaleUtils.isNetworkAvailable(this)) {
                downloadStatsResponses();
            } else if ((statsResponses.size() != number_of_fields) && LocaleUtils.isNetworkAvailable(this)) {
                downloadStatsResponses();
            } else {
                makeGraph();
            }
        } else {
            Toast.makeText(this, "Go Back and refresh data", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadStatsResponses() {
        NdviStatsTask ndviStatsTask = new NdviStatsTask();
        ndviStatsTask.execute(polygonIDs);
    }

    private void updateStatsResponses() {
        statsCheckTime = System.currentTimeMillis() / 1000L;
        editor.putLong(STATS_CHECK_TIME, statsCheckTime);

        //then update statsResponses ArrayList
        for (int i = 0; i < number_of_fields; i++) {
            String stats = sharedPref.getString("stats" + polygonIDs.get(i), null);
            if (stats != null) {
                statsResponses.add(stats);
            }
        }
    }

    public class NdviStatsTask extends AsyncTask<ArrayList<String>, Void, ArrayList<String>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            graphView1.setVisibility(View.INVISIBLE);
            loadingIndicator.setVisibility(View.VISIBLE);
            Toast.makeText(getApplicationContext(), getString(R.string.wait_dialog), Toast.LENGTH_SHORT).show();
        }

        @Override
        protected ArrayList<String> doInBackground(ArrayList<String>... polyids) {
            for (String POLY_ID : polyids[0]) {
                String response = NdviUtils.getStatsData(POLY_ID, START_DATE, FINAL_DATE, getString(R.string.Agro_API_Key));
                editor.putString("stats" + POLY_ID, response);
                editor.apply();
            }
            return statsResponses;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            if (statsResponses != null) {
                Toast.makeText(getApplicationContext(), getString(R.string.got_json_response), Toast.LENGTH_SHORT).show();
                loadingIndicator.setVisibility(View.GONE);
                graphView1.setVisibility(View.VISIBLE);
                updateStatsResponses();

                //
                spinner.setSelection(0);
                String response = statsResponses.get(0);
                meanValues = NdviUtils.getMeanValues(response);
                stdValues = NdviUtils.getStdValues(response);
                unixDates = NdviUtils.getAllDates(response);
                graphView1.removeAllSeries();
                showLastMonth.setChecked(true);
                showMean.setChecked(true);
                plotGraph(graphView1, unixDates, meanValues, 10, "Last Month Progress", "time", "NDVI");
                makeGraph();

            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.api_not_responding), Toast.LENGTH_SHORT).show();
                warning.setVisibility(View.VISIBLE);
                loadingIndicator.setVisibility(View.GONE);
                warning.setText(getString(R.string.api_not_responding));
            }
        }
    }

    private void loadSpinnerData() {
        MapsDbHelper db = new MapsDbHelper(getApplicationContext());

        // Spinner Drop down elements
        polygonNames = db.getAllNames();
        polygonIDs = db.getAllPolygonIds();

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, polygonNames);

        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);


        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);
    }

    private void makeGraph() {

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String response = statsResponses.get(position);
                meanValues = NdviUtils.getMeanValues(response);
                stdValues = NdviUtils.getStdValues(response);
                unixDates = NdviUtils.getAllDates(response);
                graphView1.removeAllSeries();
                showLastMonth.setChecked(true);
                showMean.setChecked(true);
                plotGraph(graphView1, unixDates, meanValues, 10, "Last Month Progress", "time", "NDVI");


                showLastMonth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        graphView1.removeAllSeries();
                        if (showMean.isChecked()) {
                            plotGraph(graphView1, unixDates, meanValues, 50, "Last Year Progress", "time", "NDVI");
                        } else if (showStd.isChecked()) {
                            plotGraph(graphView1, unixDates, stdValues, 50, "Last Year Uncertainty", "time", "Uncetainty");
                        }
                    }
                });
                showLastYear.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        graphView1.removeAllSeries();
                        if (showMean.isChecked()) {
                            plotGraph(graphView1, unixDates, meanValues, 10, "Last Month Progress", "time", "NDVI");
                        } else if (showStd.isChecked()) {
                            plotGraph(graphView1, unixDates, stdValues, 10, "Last Month Uncertainty", "time", "Uncetainty");
                        }
                    }
                });
                showMean.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        graphView1.removeAllSeries();
                        if (showLastMonth.isChecked()) {
                            plotGraph(graphView1, unixDates, stdValues, 10, "Last Month Uncertainty", "time", "NDVI");
                        } else if (showLastYear.isChecked()) {
                            plotGraph(graphView1, unixDates, stdValues, 50, "Last Year Uncertainty", "time", "Uncetainty");
                        }
                    }
                });
                showStd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        graphView1.removeAllSeries();
                        if (showLastMonth.isChecked()) {
                            plotGraph(graphView1, unixDates, meanValues, 10, "Last Month Progress", "time", "NDVI");
                        } else if (showLastYear.isChecked()) {
                            plotGraph(graphView1, unixDates, meanValues, 50, "Last Year Progress", "time", "Uncetainty");
                        }
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void plotGraph(GraphView graph, ArrayList<Long> dates, ArrayList<Double> data, int number_of_points,
                           String title, String labelX, String labelY) {

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(
                makeDataForGraph(dates, data, number_of_points));
        PointsGraphSeries<DataPoint> series1 = new PointsGraphSeries<>(
                makeDataForGraph(dates, data, number_of_points));

        series1.setCustomShape(new PointsGraphSeries.CustomShape() {
            @Override
            public void draw(Canvas canvas, Paint paint, float x, float y, DataPointInterface dataPoint) {
                canvas.drawCircle(x, y, 7, paint);
            }
        });
        graph.addSeries(series);
        graph.addSeries(series1);

        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalable(true);

        graph.getGridLabelRenderer().setHorizontalLabelsAngle(90);
        graph.getGridLabelRenderer().setLabelsSpace(20);
        graph.getGridLabelRenderer().setGridColor(Color.WHITE);
        graph.setTitleColor(Color.WHITE);
        graph.getViewport().setBackgroundColor(Color.BLACK);

        graph.getViewport().setDrawBorder(true);
        graph.getGridLabelRenderer().setHorizontalLabelsColor(Color.WHITE);
        graph.getGridLabelRenderer().setVerticalAxisTitleColor(Color.WHITE);
        graph.getGridLabelRenderer().setVerticalLabelsColor(Color.WHITE);

        graph.setTitle(title);
        graph.setTitleTextSize(50);
        graph.getGridLabelRenderer().setTextSize(30);
//        graph.getGridLabelRenderer().setHorizontalAxisTitle(labelX);
//        graph.getGridLabelRenderer().setVerticalAxisTitle(labelY);

        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    return NdviUtils.unixToStandardTimeForGraph((long) value);
                } else {
                    return super.formatLabel(value, isValueX);
                }
            }
        });
    }

    private DataPoint[] makeDataForGraph(ArrayList<Long> x, ArrayList<Double> y, int number_of_points) {

        int len = x.size();
        //dates and mean values are ordered from recent to older values i.e. descending
        DataPoint[] dataPoints = new DataPoint[number_of_points];

        for (int i = len - 1, j = number_of_points - 1; i >= len - number_of_points; i--, j--) {
            DataPoint pt = new DataPoint(x.get(i), y.get(i));
            dataPoints[j] = pt;
        }
        return dataPoints;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}
