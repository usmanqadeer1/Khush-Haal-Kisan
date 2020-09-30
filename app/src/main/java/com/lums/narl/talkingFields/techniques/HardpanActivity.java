package com.lums.narl.talkingFields.techniques;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lums.narl.talkingFields.R;

public class HardpanActivity extends AppCompatActivity {

    boolean flag1 = false, flag2 = false, flag3 = false, flag4 = false;
    TextView definition, causes, causes1, effects, solution;
    LinearLayout definition1, effects1,solution1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hardpan);

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setTitle(getString(R.string.hardpan));
        definition = findViewById(R.id.definition);
        definition1 = findViewById(R.id.definition1);
        causes = findViewById(R.id.causes);
        causes1 = findViewById(R.id.causes1);
        effects = findViewById(R.id.effects);
        effects1 = findViewById(R.id.effetcs1);
        solution = findViewById(R.id.solution);
        solution1 = findViewById(R.id.solution1);

        definition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!flag1){
                    definition1.setVisibility(View.VISIBLE);
                    flag1 = true;
                }else{
                    definition1.setVisibility(View.GONE);
                    flag1 = false;
                }
            }
        });

        causes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!flag2){
                    causes1.setVisibility(View.VISIBLE);
                    flag2 = true;
                }else{
                    causes1.setVisibility(View.GONE);
                    flag2 = false;
                }
            }
        });
        effects.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!flag3){
                    effects1.setVisibility(View.VISIBLE);
                    flag3 = true;
                }else{
                    effects1.setVisibility(View.GONE);
                    flag3 = false;
                }
            }
        });

        solution.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!flag4){
                    solution1.setVisibility(View.VISIBLE);
                    flag4 = true;
                }else{
                    solution1.setVisibility(View.GONE);
                    flag4 = false;
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
