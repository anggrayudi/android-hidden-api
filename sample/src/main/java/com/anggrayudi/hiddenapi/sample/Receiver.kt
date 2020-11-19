package com.anggrayudi.hiddenapi.sample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.anggrayudi.hiddenapi.ResourcesHolder

/**
 * Created by Anggrayudi on 12/03/2016.
 */
class Receiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Toast.makeText(context, "ResourceHolder is received. See detail on LogCat.", Toast.LENGTH_SHORT).show()
        val holder = intent.getParcelableExtra<ResourcesHolder>("holder") ?: return
        holder.sort(true)
        holder.printAll()
    }
}