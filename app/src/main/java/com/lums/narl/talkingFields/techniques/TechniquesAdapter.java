package com.lums.narl.talkingFields.techniques;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lums.narl.talkingFields.R;

import java.util.ArrayList;
import java.util.List;

public class TechniquesAdapter extends ArrayAdapter {
    private Context mContext;
    private List<Technique> techniquesList = new ArrayList<>();

    public TechniquesAdapter(@NonNull Context context, ArrayList<Technique> list) {
        super(context, 0 , list);
        mContext = context;
        techniquesList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.technique_item,parent,false);

        Technique currentTechnique = techniquesList.get(position);

        ImageView image = listItem.findViewById(R.id.techniqueImage);
        image.setImageResource(currentTechnique.getImageDrawable());

        TextView name = listItem.findViewById(R.id.techniqueName);
        name.setText(currentTechnique.getName());

        return listItem;
    }
}
