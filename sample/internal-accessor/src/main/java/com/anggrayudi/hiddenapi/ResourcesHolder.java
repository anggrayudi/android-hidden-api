package com.anggrayudi.hiddenapi;

import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Anggrayudi on 11/03/2016.<p>
 * A class that holds internal resources which is retrieved by {@link InternalAccessor}.
 * We need to hold the resources we just retrieved, because of <code>InternalAccessor</code> uses reflection,
 * and Java reflection may slow down user's machine performance. Using reflection every time you reflect the
 * same value is not a good practice. So, use this class instead.
 * <p>Also, you can use this class to holds non-internal resources, according to your needs.
 *
 * <p>An example to use this class is:
 * <pre>
 *     ResourcesHolder holder = new ResourcesHolder()
 *              .putString("my_string_key", InternalAccessor.getString(this, "accept"))
 *              .putResourceId("my_res_id_key", InternalAccessor.getResourceId("drawable", "loading_tile_android");
 *
 *     // Retrieve the values from another place
 *     holder.getString("my_string_key");
 *     holder.getResourceId("my_res_id_key");
 *
 *     // If you plan to send 'holder' to another class via BroadcastReceiver or LocalBroadcastManager
 *     Intent intent = new Intent(ResourcesHolder.ACTION_SEND_RESOURCES_HOLDER);
 *     intent.putExtra("holder", holder);
 *
 *     // send via BroadcastReceiver
 *     sendBroadcast(intent);
 *
 *     // send via LocalBroadcastManager
 *     LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
 * </pre>
 */
public class ResourcesHolder implements Parcelable {

    public static final String ACTION_SEND_RESOURCES_HOLDER = "com.anggrayudi.hiddenapi.ACTION_SEND_RESOURCES_HOLDER";

    private static final String TAG = "ResourcesHolder";

    private HashMap<String, Integer>           ints        = new HashMap<>();
    private HashMap<String, XmlResourceParser> xpps        = new HashMap<>();
    private HashMap<String, int[]>             arrayInt    = new HashMap<>();
    private HashMap<String, String[]>          arrayString = new HashMap<>();
    private HashMap<String, Boolean>           bools       = new HashMap<>();
    private HashMap<String, Drawable>          drawables   = new HashMap<>();
    private HashMap<String, Float>             floats      = new HashMap<>();
    private HashMap<String, InputStream>       streams     = new HashMap<>();
    private HashMap<String, String>            strings     = new HashMap<>();
    private HashMap<String, Object>            objects     = new HashMap<>();

    /**
     * Hold or save resource id. This can be id for <code>menu, layout, attr, id, string</code>, etc.
     * @param resId resource id in integer. For example:
     *              <ul>
     *                  <li>17694844</li>
     *                  <li>R.attr.colorPrimary</li>
     *                  <li>InternalAccessor.getResourceId("string", "accept")</li>
     *              </ul>
     */
    public ResourcesHolder putResourceId(String key, int resId){
        ints.put("id_" + key, resId);
        return this;
    }

    public ResourcesHolder putAnim(String key, XmlResourceParser anim){
        xpps.put("anim_"+key, anim);
        return this;
    }

    public ResourcesHolder putIntArray(String key, int[] array){
        arrayInt.put(key, array);
        return this;
    }

    public ResourcesHolder putStringArray(String key, String[] array){
        arrayString.put(key, array);
        return this;
    }

    public ResourcesHolder putBoolean(String key, boolean bool){
        bools.put(key, bool);
        return this;
    }

    public ResourcesHolder putColor(String key, int color){
        ints.put("color_"+key, color);
        return this;
    }

    public ResourcesHolder putDimension(String key, float dimen){
        floats.put("dimen_"+key, dimen);
        return this;
    }

    public ResourcesHolder putDrawable(String key, Drawable drawable){
        drawables.put("drawable_"+key, drawable);
        return this;
    }

    /**
     * An additional method to holds <code>float</code> value, though Android doesn't provide <code>float</code> resource value.
     */
    public ResourcesHolder putFloat(String key, float value){
        floats.put("float_"+key, value);
        return this;
    }

    public ResourcesHolder putFraction(String key, float fraction){
        floats.put("fraction_"+key, fraction);
        return this;
    }

    public ResourcesHolder putInt(String key, int value){
        ints.put("int_"+key, value);
        return this;
    }

    public ResourcesHolder putLayout(String key, XmlResourceParser layout){
        xpps.put("layout_"+key, layout);
        return this;
    }

    public ResourcesHolder putMenu(String key, XmlResourceParser menu){
        xpps.put("menu_"+key, menu);
        return this;
    }

    public ResourcesHolder putMipmap(String key, Drawable mipmap){
        drawables.put("mipmap_"+key, mipmap);
        return this;
    }

    public ResourcesHolder putRaw(String key, InputStream raw){
        streams.put(key, raw);
        return this;
    }

    public ResourcesHolder putString(String key, String string){
        strings.put(key, string);
        return this;
    }

    public ResourcesHolder putXml(String key, XmlResourceParser xml){
        xpps.put("xml_"+key, xml);
        return this;
    }

    /**
     * An additional method to holds an <code>Object</code>.
     */
    public ResourcesHolder putObject(String key, Object object){
        objects.put(key, object);
        return this;
    }

    public int getResourceId(String key){
        return ints.get("id_"+key);
    }

    public XmlResourceParser getAnim(String key){
        return xpps.get("anim_"+key);
    }

    public int[] getIntArray(String key){
        return arrayInt.get(key);
    }

    public String[] getStringArray(String key){
        return arrayString.get(key);
    }

    public boolean getBoolean(String key){
        return bools.get(key);
    }

    public int getColor(String key){
        return ints.get("color_"+key);
    }

    public float getDimension(String key){
        return floats.get("dimen_"+key);
    }

    public Drawable getDrawable(String key){
        return drawables.get("drawable_"+key);
    }

    public float getFloat(String key){
        return floats.get("float_"+key);
    }

    public float getFraction(String key){
        return floats.get("fraction_"+key);
    }

    public int getInt(String key){
        return ints.get("int_"+key);
    }

    public XmlResourceParser getLayout(String key){
        return xpps.get("layout_"+key);
    }

    public XmlResourceParser getMenu(String key){
        return xpps.get("menu_"+key);
    }

    public Drawable getMipmap(String key){
        return drawables.get("mipmap_"+key);
    }

    public InputStream getRaw(String key){
        return streams.get(key);
    }

    public String getString(String key){
        return strings.get(key);
    }

    public XmlResourceParser getXml(String key){
        return xpps.get("xml_"+key);
    }

    public Object getObject(String key){
        return objects.get(key);
    }

    /**
     * Clear all values saved in this class.
     */
    public void clear(){
        ints.clear();
        xpps.clear();
        arrayInt.clear();
        arrayString.clear();
        bools.clear();
        drawables.clear();
        floats.clear();
        streams.clear();
        strings.clear();
        objects.clear();
    }

    /**
     * Print all values that is saved via <code>put*(Key, Value)</code> method.
     */
    public void printAll(){
        // Sort the value we want to print with TreeMap, because HashMap doesn't support sorting value
        TreeMap<String, Integer>           ints        = new TreeMap<>();
        TreeMap<String, XmlResourceParser> xpps        = new TreeMap<>();
        TreeMap<String, int[]>             arrayInt    = new TreeMap<>();
        TreeMap<String, String[]>          arrayString = new TreeMap<>();
        TreeMap<String, Boolean>           bools       = new TreeMap<>();
        TreeMap<String, Drawable>          drawables   = new TreeMap<>();
        TreeMap<String, Float>             floats      = new TreeMap<>();
        TreeMap<String, InputStream>       streams     = new TreeMap<>();
        TreeMap<String, String>            strings     = new TreeMap<>();
        TreeMap<String, Object>            objects     = new TreeMap<>();

        ints.putAll(this.ints);
        xpps.putAll(this.xpps);
        arrayInt.putAll(this.arrayInt);
        arrayString.putAll(this.arrayString);
        bools.putAll(this.bools);
        drawables.putAll(this.drawables);
        floats.putAll(this.floats);
        streams.putAll(this.streams);
        strings.putAll(this.strings);
        objects.putAll(this.objects);

        for (Map.Entry<String, Integer> entry : ints.entrySet()) {
            switch (getPre(entry.getKey())){
                case "id"   : Log.d(TAG, "RESOURCE ID, "+ removePre(entry.getKey())+"="+entry.getValue()); break;
                case "int"  : Log.d(TAG, "INTEGER, "+ removePre(entry.getKey())+"="+entry.getValue()); break;
                case "color": Log.d(TAG, "COLOR, "+ removePre(entry.getKey())+"="+entry.getValue()); break;
            }
        }
        for (Map.Entry<String, XmlResourceParser> entry : xpps.entrySet()) {
            switch (getPre(entry.getKey())){
                case "anim"  : Log.d(TAG, "ANIM, "+ removePre(entry.getKey()) +"="+entry.getValue()); break;
                case "menu"  : Log.d(TAG, "MENU, "+ removePre(entry.getKey()) +"="+entry.getValue()); break;
                case "layout": Log.d(TAG, "LAYOUT, "+ removePre(entry.getKey()) +"="+entry.getValue()); break;
                case "xml"   : Log.d(TAG, "XML, "+ removePre(entry.getKey()) +"="+entry.getValue()); break;
            }
        }
        for (Map.Entry<String, int[]> entry : arrayInt.entrySet())
            Log.d(TAG, "ARRAY INTEGER, "+ entry.getKey() +"="+ Arrays.toString(entry.getValue()));

        for (Map.Entry<String, String[]> entry : arrayString.entrySet())
            Log.d(TAG, "ARRAY STRING, "+ entry.getKey() +"="+ Arrays.toString(entry.getValue()));

        for (Map.Entry<String, Boolean> entry : bools.entrySet())
            Log.d(TAG, "BOOLEAN, "+ entry.getKey() +"="+ entry.getValue());

        for (Map.Entry<String, Drawable> entry : drawables.entrySet()) {
            switch (getPre(entry.getKey())) {
                case "drawable" :Log.d(TAG, "DRAWABLE, "+ removePre(entry.getKey()) +"="+ entry.getValue()); break;
                case "mipmap"   :Log.d(TAG, "MIPMAP, "+ removePre(entry.getKey()) +"="+ entry.getValue()); break;
            }
        }
        for (Map.Entry<String, Float> entry : floats.entrySet()) {
            switch (getPre(entry.getKey())) {
                case "dimen"   :Log.d(TAG, "DIMENSION, "+ removePre(entry.getKey()) +"="+ entry.getValue()); break;
                case "fraction":Log.d(TAG, "FRACTION, "+ removePre(entry.getKey()) +"="+ entry.getValue()); break;
                case "float"   :Log.d(TAG, "FLOAT, "+ removePre(entry.getKey()) +"="+ entry.getValue()); break;
            }
        }
        for (Map.Entry<String, InputStream> entry : streams.entrySet())
            Log.d(TAG, "RAW, InputStream, "+ entry.getKey() +"="+ entry.getValue());

        for (Map.Entry<String, String> entry : strings.entrySet())
            Log.d(TAG, "STRING, "+ entry.getKey() +"="+ entry.getValue());

        for (Map.Entry<String, Object> entry : objects.entrySet())
            Log.d(TAG, "OBJECT, "+ entry.getKey() +"="+ entry.getValue());
    }

    private static String removePre(String key){
        return key.substring(key.indexOf("_") + 1);
    }

    private static String getPre(String key){
        return key.substring(0, key.indexOf("_"));
    }

    public ResourcesHolder(){}

    @SuppressWarnings("unchecked")
    private ResourcesHolder(Parcel in) {
        ints = (HashMap<String, Integer>) in.readValue(HashMap.class.getClassLoader());
        xpps = (HashMap<String, XmlResourceParser>) in.readValue(HashMap.class.getClassLoader());
        arrayInt = (HashMap<String, int[]>) in.readValue(HashMap.class.getClassLoader());
        arrayString = (HashMap<String, String[]>) in.readValue(HashMap.class.getClassLoader());
        bools = (HashMap<String, Boolean>) in.readValue(HashMap.class.getClassLoader());
        drawables = (HashMap<String, Drawable>) in.readValue(HashMap.class.getClassLoader());
        floats = (HashMap<String, Float>) in.readValue(HashMap.class.getClassLoader());
        streams = (HashMap<String, InputStream>) in.readValue(HashMap.class.getClassLoader());
        strings = (HashMap<String, String>) in.readValue(HashMap.class.getClassLoader());
        objects = (HashMap<String, Object>) in.readValue(HashMap.class.getClassLoader());
    }

    public static final Creator<ResourcesHolder> CREATOR = new Creator<ResourcesHolder>() {
        @Override
        public ResourcesHolder createFromParcel(Parcel in) {
            return new ResourcesHolder(in);
        }

        @Override
        public ResourcesHolder[] newArray(int size) {
            return new ResourcesHolder[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(ints);
        dest.writeValue(xpps);
        dest.writeValue(arrayInt);
        dest.writeValue(arrayString);
        dest.writeValue(bools);
        dest.writeValue(drawables);
        dest.writeValue(floats);
        dest.writeValue(streams);
        dest.writeValue(strings);
        dest.writeValue(objects);
    }
}
