package com.anggrayudi.hiddenapi.sample;

import android.content.Intent;
import android.net.EthernetManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.anggrayudi.hiddenapi.InternalAccessor;
import com.anggrayudi.hiddenapi.ResourcesHolder;

import java.util.ArrayList;

/**
 * Created by Anggrayudi on 11/03/2016.<p>
 * An example class for Hidden API.
 */
public class MainActivity extends AppCompatActivity {

    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ResourcesHolder holder = new ResourcesHolder()
                .putString("my_string", InternalAccessor.getString(this, "accept"))
                .putDimension("my_dimen", InternalAccessor.getDimension(this, "status_bar_height"))
                .putColor("my_color", InternalAccessor.getColor(this, "config_defaultNotificationColor"))
                .putInt("my_int", 700);

        intent = new Intent(ResourcesHolder.ACTION_SEND_RESOURCES_HOLDER);
        intent.putExtra("holder", holder);

        ArrayList<Model> items = new ArrayList<>();

        items.add(new Model("Formatter.formatShortElapsedTime(this, 100000000)", Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                ? Formatter.formatShortElapsedTime(this, 100000000) : "",
                "Accessing hidden method.\nThis method only available for API 21+. If you run it on a"+
                        " device with API 20 and lower, you'll get java.lang.NoSuchMethodError exception."));

        items.add(new Model("com.android.internal.R.string.accept", "",
                "Accessing hidden String resource.\nWe cannot access internal resources directly. Sometimes, IDE " +
                        "says 'error: cannot find symbol variable accept' once you run the app, or "+
                        "your app picks wrong resource id. If you want to have the internal resources, " +
                        "copy them to your project or use InternalAccessor utility class. Below are the example."));

        items.add(new Model("InternalAccessor.getString(this, \"accept\")", holder.getString("my_string"),
                "Accessing hidden String resource.\nBecause above method is not working, so we need to use "+
                        "InternalAccessor.getString() method."));

        items.add(new Model("InternalAccessor.getDimension(this, \"status_bar_height\")", holder.getDimension("my_dimen")+"",
                "Accessing hidden dimension resource."));

        items.add(new Model("InternalAccessor.getColor(this, \"config_defaultNotificationColor\")", holder.getColor("my_color")+"",
                "Accessing hidden color resource."));

        items.add(new Model("Info", "", "For more information, download this app's source code on " +
                "https://github.com/anggrayudi/android-hidden-api"));

        Adapter adapter = new Adapter(this, items);
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(adapter);

        if (Build.VERSION.SDK_INT >= 22){
        /*   Accessing EthernetManager that is a hidden class and only available for API 22+.
             If you want to find out which API was built to, just type method, resource or class name
             to search box on http://jcs.mobile-utopia.com/servlet/Source?type=s&q=android.net.EthernetManager%E2%80%8C%E2%80%8B
             And then, look at 'Category' column.
        */
            EthernetManager em;
        }

        try {
            // This will retrieve resource id named accelerate_cubic in com.android.internal.R.interpolator class
            Log.d("---", "interpolator.accelerate_cubic = "+ InternalAccessor.getResourceId("interpolator", "accelerate_cubic"));

            Log.d("---", "plurals.duration_hours = "+ InternalAccessor.getResourceId("plurals", "duration_hours"));
            Log.d("---", "transition.no_transition = "+ InternalAccessor.getResourceId("transition", "no_transition"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
//        new MyClass().shit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();

        // Sending 'holder' object to another class via BroadcastReceiver
        if (item.getItemId() == R.id.send_holder)
            sendBroadcast(intent);

        return super.onOptionsItemSelected(item);
    }

    void sendViaLocalBroadcastManager(ResourcesHolder holder){
        // Sending 'holder' object to another class via LocalBroadcastManager
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    void clearHolder(ResourcesHolder holder){
        // If you want to clear everything from 'holder', call clear()
        // This method will returns empty 'holder'
        holder.clear();
    }
}