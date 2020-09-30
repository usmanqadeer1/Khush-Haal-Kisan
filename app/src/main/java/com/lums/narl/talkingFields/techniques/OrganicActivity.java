package com.lums.narl.talkingFields.techniques;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.lums.narl.talkingFields.R;

public class OrganicActivity extends AppCompatActivity {

    boolean flag1 = false, flag2 = false, flag3 = false;
    TextView definition, objective, objective1, methodology;
    LinearLayout definition1,methodology1;
    ScrollView scrollView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organic);
        setTitle(getString(R.string.increase_organic_matter));
        scrollView = findViewById(R.id.organic_matter);

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        definition = findViewById(R.id.definition);
        definition1 = findViewById(R.id.definition1);
        objective = findViewById(R.id.objective);
        objective1 = findViewById(R.id.objective1);
        methodology = findViewById(R.id.methodology);
        methodology1 = findViewById(R.id.methodology1);
        setTitle(getString(R.string.mulching));

        definition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!flag1){
                    definition1.setVisibility(View.VISIBLE);
                    flag1 = true;
                }
                else{
                    definition1.setVisibility(View.GONE);
                    flag1 = false;
                }
            }
        });

        objective.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!flag2){
                    objective1.setVisibility(View.VISIBLE);
                    flag2 = true;
                }
                else{
                    objective1.setVisibility(View.GONE);
                    flag2 = false;
                }
            }
        });

        methodology.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!flag3){
                    methodology1.setVisibility(View.VISIBLE);
                    flag3 = true;
                }
                else{
                    methodology1.setVisibility(View.GONE);
                    flag3 = false;
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
