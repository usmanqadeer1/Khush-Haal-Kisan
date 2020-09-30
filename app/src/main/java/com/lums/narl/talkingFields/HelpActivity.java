package com.lums.narl.talkingFields;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.lums.narl.talkingFields.Help.HelpAdapter;
import com.lums.narl.talkingFields.Help.HelpLink;
import com.lums.narl.talkingFields.Tutorial.IntroActivity;
import com.lums.narl.talkingFields.Utils.LocaleUtils;

import java.util.ArrayList;

public class HelpActivity extends AppCompatActivity {

    private ArrayList<HelpLink> helpLinks;
    private ListView helpListView;
    private Button tutorial;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleUtils.loadLocale(this);
        setContentView(R.layout.activity_help);
        helpListView = findViewById(R.id.help_link_list);
        tutorial = findViewById(R.id.tutorial);

        tutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), IntroActivity.class));
            }
        });

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        linksArrayInitialize();

        setTitle(getString(R.string.help));
        HelpAdapter helpAdapter = new HelpAdapter(this, helpLinks);
        helpListView.setAdapter(helpAdapter);

        helpListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openVideo(helpLinks.get(position).getLink());
            }
        });
    }

    private void openVideo(String link){
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));

    }

    private void linksArrayInitialize(){
        /*String[] links = {"https://youtu.be/OgnGQ-iEeMU","https://youtu.be/BU-DzCaG0Xs","https://youtu.be/bUNxecwl8ck",
                "https://youtu.be/aR91TJO2qUY","https://youtu.be/DfGRHfd_HZ0"};*/
        String[] links = {"https://www.youtube.com/watch?v=OgnGQ-iEeMU&list=PLoWXTTiSxzLqD8HKLheUaiOwAZVHnYdmx&index=3",
                "https://www.youtube.com/watch?v=QDHDqGSlmwQ&list=PLoWXTTiSxzLqD8HKLheUaiOwAZVHnYdmx&index=5",
                "https://www.youtube.com/watch?v=fq4aap_0xbw&list=PLoWXTTiSxzLqD8HKLheUaiOwAZVHnYdmx&index=4",
                "https://www.youtube.com/watch?v=aR91TJO2qUY&list=PLoWXTTiSxzLqD8HKLheUaiOwAZVHnYdmx&index=2",
                "https://www.youtube.com/watch?v=DfGRHfd_HZ0&list=PLoWXTTiSxzLqD8HKLheUaiOwAZVHnYdmx&index=1"};
        String[] linksNames = getResources().getStringArray(R.array.array_help_links);
        helpLinks = new ArrayList<>();
        for(int i = 0; i<links.length;i++){
            helpLinks.add(new HelpLink(linksNames[i],links[i],R.drawable.youtube));
        }

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
