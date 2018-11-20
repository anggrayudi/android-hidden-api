package com.anggrayudi.hiddenapi.sample;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Anggrayudi on 11/03/2016.
 */
class Adapter extends BaseAdapter {

    private static class ViewHolder {
        TextView source, result, desc;
    }

    private ArrayList<Model> models;

    Adapter(ArrayList<Model> models){
        this.models = models;
    }

    @Override
    public int getCount() {
        return models.size();
    }

    @Override
    public Object getItem(int position) {
        return models.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)
                    parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_layout, parent, false);

            ViewHolder viewHolder = new ViewHolder();
            viewHolder.source = convertView.findViewById(R.id.source);
            viewHolder.result = convertView.findViewById(R.id.result);
            viewHolder.desc = convertView.findViewById(R.id.desc);
            convertView.setTag(viewHolder);
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();
        Model model = models.get(position);

        holder.source.setText(model.source);

        holder.result.setVisibility(model.result.equals("Result = ") ? View.GONE : View.VISIBLE);
        holder.result.setText(model.result);

        holder.desc.setVisibility(TextUtils.isEmpty(model.description) ? View.GONE : View.VISIBLE);
        holder.desc.setText(model.description);

        return convertView;
    }
}
