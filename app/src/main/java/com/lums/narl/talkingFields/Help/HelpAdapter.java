package com.lums.narl.talkingFields.Help;

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


public class HelpAdapter extends ArrayAdapter<HelpLink> {

    private Context mContext;
    private List<HelpLink> helpLinksList = new ArrayList<>();

    public HelpAdapter(@NonNull Context context, ArrayList<HelpLink> list) {
        super(context, 0 , list);
        mContext = context;
        helpLinksList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.help_list_item,parent,false);

        HelpLink currentLink = helpLinksList.get(position);

        ImageView image = listItem.findViewById(R.id.linkImage);
        image.setImageResource(currentLink.getImageDrawable());

        TextView name = listItem.findViewById(R.id.linkName);
        name.setText(currentLink.getLinkName());

        return listItem;
    }
}
