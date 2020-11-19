package com.anggrayudi.hiddenapi.sample

import android.content.Intent
import android.net.Uri
//import android.net.wifi.WifiLinkLayerStats
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.view.MenuItem
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import com.anggrayudi.hiddenapi.InternalAccessor
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

/**
 * Created by Anggrayudi on 11/03/2016.
 *
 * An example class for Hidden API.
 *
 * If you plan to use only Android internal resources rather than internal classes or methods,
 * just add <br></br>`compile 'com.anggrayudi:android-hidden-api:30.0'`<br></br> library
 * to your app's module without need to replace `android.jar`. This version does not use
 * Java reflection anymore, and certainly safe.
 * See the [Usage](https://github.com/anggrayudi/android-hidden-api#usage).
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Test if Editable included in custom android.jar
        var editable: Editable

        val items = mutableListOf(
//            Model(
//                /*
//                formatShortElapsedTime method will show error 'Cannot resolve symbol' if you don't use custom android.jar
//                Since custom android.jar v28, some methods are no longer accessible. I don't know why.
//                Android Studio will say, "formatShortElapsedTime has private access".
//                A workaround is you HAVE TO copy this static method into your own code.
//                */
//                "Formatter.formatShortElapsedTime(this, 100000000)",
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) Formatter.formatShortElapsedTime(this, 100000000) else null,
//                """
//                Accessing hidden method. This method only available for API 21+. If you run it on a device with API 20 and lower,
//                you'll get java.lang.NoSuchMethodError exception.
//                """.trimIndent()
//            ),
            Model(
                "com.android.internal.R.string.accept",
                description = "Accessing hidden String resource. We cannot access internal resources directly. "
                        + "Sometimes, IDE says 'error: cannot find symbol variable accept' "
                        + "once you run the app, or your app picks wrong resource id. If you want to have the internal resources, "
                        + "copy them to your project or use InternalAccessor utility class. Below are the example."
            ),
            Model(
                "InternalAccessor.getString(\"accept\")",
                InternalAccessor.getString("accept"),
                "Accessing hidden String resource. Because above method is not working, "
                        + "so we need to use InternalAccessor.getString() method."
            )
        )
        items.add(
            Model(
                "InternalAccessor.getDimension(\"status_bar_height\")",
                InternalAccessor.getDimension("status_bar_height").toString(),
                "Accessing hidden dimension resource."
            )
        )
        items.add(
            Model(
                "InternalAccessor.getColor(\"config_defaultNotificationColor\")",
                InternalAccessor.getColor("config_defaultNotificationColor").toString(),
                "Accessing hidden color resource."
            )
        )
        items.add(
            Model(
                "Info", description = "For more information, download this app's source code on " +
                        "https://github.com/anggrayudi/android-hidden-api"
            )
        )

        listView.adapter = Adapter(items)
        listView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
            if (position == items.size - 1) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/anggrayudi/android-hidden-api")))
            }
        }

        if (Build.VERSION.SDK_INT >= 22) {
            /*
            Accessing WifiLinkLayerStats that is a hidden class.
            If you want to find out which API was built to, just type method, resource or class name
            to search box on http://jcs.mobile-utopia.com/servlet/Source?type=s&q=WifiLinkLayerStats
            And then, look at 'Category' column.

            WifiLinkLayerStats method will show error 'Cannot resolve symbol' if you don't use custom android.jar
            For source code: http://jcs.mobile-utopia.com/jcs/7601_WifiLinkLayerStats.java
            */
//            var wifiLinkLayer: WifiLinkLayerStats
        }

        // If you want to check whether WifiLinkLayerStats exists without checking API level, you can call:
        val isClassExists = InternalAccessor.isClassExists("android.net.wifi.WifiLinkLayerStats")
        Timber.d("isClassExists = $isClassExists")

        // Check whether a method exists
        val isMethodExists = InternalAccessor.isMethodExists("android.content.Intent", "getExtra")
        Timber.d("isMethodExists = $isMethodExists")

        // This will retrieve resource id named accelerate_cubic in com.android.internal.R.interpolator class.
        Timber.d("interpolator.accelerate_cubic = %s", InternalAccessor.getResourceId(InternalAccessor.INTERPOLATOR, "accelerate_cubic"))
        Timber.d("plurals.duration_hours = %s", InternalAccessor.getResourceId(InternalAccessor.PLURALS, "last_num_days"))
        Timber.d("transition.no_transition = %s", InternalAccessor.getResourceId(InternalAccessor.TRANSITION, "no_transition"))

        /*
        DEPRECATED EXAMPLE OF InternalAccessor.Builder

        // Using InternalAccessor with other code styling
        InternalAccessor.Builder builder = new InternalAccessor.Builder(true);
        boolean b = builder.getBoolean("config_sip_wifi_only");
        String accept = builder.getString("accept");

        // Because we set true to 'saveToResourcesHolder' in the Builder constructor, every value we got always
        // saved to ResourcesHolder automatically. We can retrieve the holder now:
        ResourcesHolder accessorHolder = builder.getResourcesHolder();
        b = accessorHolder.getAsBoolean("config_sip_wifi_only");
        accept = accessorHolder.getAsString("accept");
        */
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}