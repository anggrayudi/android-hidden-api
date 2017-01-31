package com.anggrayudi.hiddenapi.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.anggrayudi.hiddenapi.ResourcesHolder;

/**
 * Created by Anggrayudi on 12/03/2016.
 */
public class Receiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "ResourceHolder is received. See detail on LogCat.", Toast.LENGTH_SHORT).show();

        ResourcesHolder holder = intent.getParcelableExtra("holder");
        holder.sort(true);
        holder.printAll();
    }
}
