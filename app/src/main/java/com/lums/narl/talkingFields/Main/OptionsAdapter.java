package com.lums.narl.talkingFields.Main;

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

public class OptionsAdapter extends ArrayAdapter<MainOption> {

    private Context mContext;
    private List<MainOption> optionsList = new ArrayList<>();

    public OptionsAdapter(@NonNull Context context, ArrayList<MainOption> list) {
        super(context, 0 , list);
        mContext = context;
        optionsList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.option_list_item,parent,false);

        MainOption currentOption = optionsList.get(position);

        ImageView image = listItem.findViewById(R.id.option_image);
        image.setImageResource(currentOption.getImageDrawable());

        TextView name = listItem.findViewById(R.id.option_name);
        name.setText(currentOption.getOptionName());

        return listItem;
    }
}
