package com.lums.narl.talkingFields;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.lums.narl.talkingFields.techniques.HardpanActivity;
import com.lums.narl.talkingFields.techniques.MulchingActivity;
import com.lums.narl.talkingFields.techniques.OrganicActivity;
import com.lums.narl.talkingFields.techniques.SoilWashingActivity;
import com.lums.narl.talkingFields.techniques.Technique;
import com.lums.narl.talkingFields.techniques.TechniquesAdapter;

import java.util.ArrayList;

public class PqnkActivty extends AppCompatActivity {

    GridView techniquesListView;
    ArrayList<Technique> techniques;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pqnk);

        setTitle(getString(R.string.improve_crop1));
        techniquesListView = findViewById(R.id.techniques_list);
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String[] techniquesNames = getResources().getStringArray(R.array.array_techniques);
        int[] imageDrawables = {R.drawable.mulching, R.drawable.hardpan, R.drawable.soil_ph, R.drawable.organic_matter};
        techniques = new ArrayList<>();
        for(int i = 0; i < imageDrawables.length;i++){
            techniques.add(new Technique(techniquesNames[i],imageDrawables[i]));
        }
        TechniquesAdapter techniquesAdapter = new TechniquesAdapter(this, techniques);
        techniquesListView.setAdapter(techniquesAdapter);

        techniquesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch(position){
                    case 0: startActivity(new Intent(getApplicationContext(), MulchingActivity.class)); break;
                    case 1: startActivity(new Intent(getApplicationContext(), HardpanActivity.class)); break;
                    case 2: startActivity(new Intent(getApplicationContext(), SoilWashingActivity.class)); break;
                    case 3: startActivity(new Intent(getApplicationContext(), OrganicActivity.class)); break;
                }
            }
        });


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
