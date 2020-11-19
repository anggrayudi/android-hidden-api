package com.anggrayudi.hiddenapi;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by Anggrayudi on 11/03/2016.<p>
 * Since version 0.0.5 the uses of this class is diverted. You don't need to avoid Java reflection by storing
 * the internal resources you just retrieved to {@link ResourcesHolder} and send them via Broadcast, because this version
 * does not use Java reflection anymore. Instead, you can use this class to store any objects you
 * might need in other classes (like passing objects from activity to service) and send them via Broadcast.
 * <hr>
 * <h2>Obsolete documentation (v0.0.4 and lower)</h2>
 * A class that holds internal resources which are retrieved by {@link InternalAccessor}.
 * We need to hold the resources we just retrieved, because <code>InternalAccessor</code> uses reflection,
 * and Java reflection may slow down user's machine performance. Using reflection every time you reflect the
 * same value is not a good practice. So, use this class instead.
 * <p>Also, you can use this class to holds non-internal resources, according to your needs.
 *
 * <p>An example to use this class is:
 * <pre>
 *     ResourcesHolder holder = new ResourcesHolder()
 *              .putString("my_string_key", InternalAccessor.getString("accept"))
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
 *     // send via LocalBroadcastManager
 *     LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
 *
 *     // send via BroadcastReceiver
 *     sendBroadcast(intent);
 *
 *     // Instead, you can do this in simple way
 *     holder.sendBroadcast(this, "holder");
 *     // or with this
 *     holder.sendViaLocalBroadcastManager(this, "holder");
 * </pre>
 */
public class ResourcesHolder implements Parcelable {
    public static final String ACTION_SEND_RESOURCES_HOLDER = "com.anggrayudi.hiddenapi.ACTION_SEND_RESOURCES_HOLDER";
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
    private static final String TAG = "ResourcesHolder";
    private HashMap<String, Object> mValues = new HashMap<>();

    public ResourcesHolder() {
    }

    @SuppressWarnings("unchecked")
    private ResourcesHolder(Parcel in) {
        mValues = (HashMap<String, Object>) in.readValue(HashMap.class.getClassLoader());
    }

    public ResourcesHolder put(String key, short value) {
        mValues.put(key, value);
        return this;
    }

    public ResourcesHolder put(String key, byte value) {
        mValues.put(key, value);
        return this;
    }

    public ResourcesHolder put(String key, char value) {
        mValues.put(key, value);
        return this;
    }

    public ResourcesHolder put(String key, boolean value) {
        mValues.put(key, value);
        return this;
    }

    public ResourcesHolder put(String key, int value) {
        mValues.put(key, value);
        return this;
    }

    public ResourcesHolder put(String key, long value) {
        mValues.put(key, value);
        return this;
    }

    public ResourcesHolder put(String key, float value) {
        mValues.put(key, value);
        return this;
    }

    public ResourcesHolder put(String key, double value) {
        mValues.put(key, value);
        return this;
    }

    public ResourcesHolder put(String key, String value) {
        mValues.put(key, value);
        return this;
    }

    /**
     * An additional method to holds an <code>Object</code>, for non-primitive data types. For example
     * <code>Drawable, XmlResourceParser, </code> etc. To retrieve it back to the original object,
     * cast it to its origin class. You also can store an array here. An example of using this method is:
     * <pre>
     *  List&lt;String&gt; strs = new ArrayList&lt;&gt;();
     *  strs.add("FOO");
     *  strs.add("BXY");
     *
     *  Person[] persons = {new Person(), null, new Person()};
     *
     *  XmlResourceParser parser = getResources().getXml(R.xml.preferences);
     *
     *  ResourcesHolder holder = new ResourcesHolder()
     *      .put("arrayInt", new int[]{9, 8, 5})
     *      .put("arrayStringList", strs)
     *      .put("arrayPerson", persons)
     *      .put("my_parser", parser);
     *
     *  // Then, retrieve them back to the original form
     *  int[] ints = holder.getAsIntArray("arrayInt");
     *  //noinspection unchecked
     *  strs = (List&lt;String&gt;) holder.getAsObject("arrayStringList");
     *  // cast them to the original class
     *  persons = (Person[]) holder.getAsObject("arrayPerson");
     *  parser = (XmlResourceParser) holder.getAsObject("my_parser");
     * </pre>
     */
    public ResourcesHolder put(String key, Object object) {
        mValues.put(key, object);
        return this;
    }

    public short getAsShort(String key) {
        return isCompatibleCast(key, Short.class) ? (short) mValues.get(key) : 0;
    }

    public byte getAsByte(String key) {
        return isCompatibleCast(key, Byte.class) ? (byte) mValues.get(key) : 0;
    }

    public char getAsChar(String key) {
        return isCompatibleCast(key, Character.class) ? (char) mValues.get(key) : '\u0000';
    }

    public boolean getAsBoolean(String key) {
        return isCompatibleCast(key, Boolean.class) && (boolean) mValues.get(key);
    }

    /**
     * Some resources use this data type, they are <code>color, resourceId</code> and <code>integerResource</code>.
     * You also can get any integer here.
     */
    public int getAsInteger(String key) {
        return isCompatibleCast(key, Integer.class) ? (int) mValues.get(key) : 0;
    }

    public long getAsLong(String key) {
        return isCompatibleCast(key, Long.class) ? (long) mValues.get(key) : 0;
    }

    /**
     * Some resources use this data type, they are <code>dimension</code> and <code>fraction</code>.
     * You also can get any <code>float</code> here.
     */
    public float getAsFloat(String key) {
        return isCompatibleCast(key, Float.class) ? (float) mValues.get(key) : 0;
    }

    public double getAsDouble(String key) {
        return isCompatibleCast(key, Double.class) ? (double) mValues.get(key) : 0;
    }

    public String getAsString(String key) {
        return isCompatibleCast(key, String.class) ? (String) mValues.get(key) : null;
    }

    public short[] getAsShortArray(String key) {
        return isCompatibleCast(key, short[].class) ? (short[]) mValues.get(key) : null;
    }

    public byte[] getAsByteArray(String key) {
        return isCompatibleCast(key, byte[].class) ? (byte[]) mValues.get(key) : null;
    }

    public char[] getAsCharArray(String key) {
        return isCompatibleCast(key, char[].class) ? (char[]) mValues.get(key) : null;
    }

    public boolean[] getAsBooleanArray(String key) {
        return isCompatibleCast(key, boolean[].class) ? (boolean[]) mValues.get(key) : null;
    }

    public int[] getAsIntArray(String key) {
        return isCompatibleCast(key, int[].class) ? (int[]) mValues.get(key) : null;
    }

    public long[] getAsLongArray(String key) {
        return isCompatibleCast(key, long[].class) ? (long[]) mValues.get(key) : null;
    }

    public float[] getAsFloatArray(String key) {
        return isCompatibleCast(key, float[].class) ? (float[]) mValues.get(key) : null;
    }

    public double[] getAsDoubleArray(String key) {
        return isCompatibleCast(key, double[].class) ? (double[]) mValues.get(key) : null;
    }

    public String[] getAsStringArray(String key) {
        return isCompatibleCast(key, String[].class) ? (String[]) mValues.get(key) : null;
    }

    /**
     * Get any values that is stored as <code>Object</code>, i.e. for non-primitive data types.
     * They can be an array or an instance from a class.
     */
    public Object getAsObject(String key) {
        return mValues.get(key);
    }

    /**
     * This method equals with {@link Context#sendBroadcast(Intent)}. Notice that the <code>Intent</code>
     * uses {@link ResourcesHolder#ACTION_SEND_RESOURCES_HOLDER} action. So that, register your
     * <code>BroadcastReceiver</code> with that action to make it able to receive the extra from <code>Intent</code>.
     * <p>See {@link ResourcesHolder#sendViaLocalBroadcastManager(Context, String)}</p>
     *
     * @param key key for the <code>Intent</code>
     */
    public void sendBroadcast(Context context, @NonNull String key) {
        Intent intent = new Intent(ACTION_SEND_RESOURCES_HOLDER);
        intent.putExtra(key, this);
        context.sendBroadcast(intent);
    }

    /**
     * Send <code>ResourcesHolder</code> instance to another class via <code>LocalBroadcastManager</code>.
     * Make sure that the <code>BroadcastReceiver</code> is registered with
     * {@link ResourcesHolder#ACTION_SEND_RESOURCES_HOLDER} action.
     *
     * @param key key for the <code>Intent</code>
     */
    public void sendViaLocalBroadcastManager(Context context, @NonNull String key) {
        Intent intent = new Intent(ACTION_SEND_RESOURCES_HOLDER);
        intent.putExtra(key, this);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    /**
     * Clear all values that is saved in this class.
     */
    public void clear() {
        mValues.clear();
    }

    public int size() {
        return mValues.size();
    }

    public boolean containsKey(String key) {
        return mValues.containsKey(key);
    }

    /**
     * Remove a value by key.
     */
    public void remove(String key) {
        mValues.remove(key);
    }

    /**
     * Sort <code>ResourcesHolder</code> by keys.
     *
     * @param descending or set to <code>false</code> to make it ascending
     */
    public void sort(boolean descending) {
        TreeMap<String, Object> treeMap = new TreeMap<>(mValues);
        if (descending) {
            treeMap.descendingMap();
        }
        mValues.clear();
        mValues.putAll(treeMap);
    }

    /**
     * Returns a set of all of the keys and values
     *
     * @return a set of all of the keys and values
     */
    public Set<Map.Entry<String, Object>> valueSet() {
        return mValues.entrySet();
    }

    /**
     * Returns a set of all of the keys
     *
     * @return a set of all of the keys
     */
    public Set<String> keySet() {
        return mValues.keySet();
    }

    /**
     * Print all values that is saved via <code>put*(Key, Value)</code> method.
     */
    public void printAll() {
        for (Map.Entry<String, Object> entry : mValues.entrySet()) {

            Object value = entry.getValue();
            String toPrint = value.toString();

            if (value.getClass().isArray()) {
                if (value instanceof boolean[])
                    toPrint = Arrays.toString((boolean[]) value);
                else if (value instanceof int[])
                    toPrint = Arrays.toString((int[]) value);
                else if (value instanceof long[])
                    toPrint = Arrays.toString((long[]) value);
                else if (value instanceof float[])
                    toPrint = Arrays.toString((float[]) value);
                else if (value instanceof double[])
                    toPrint = Arrays.toString((double[]) value);
                else if (value instanceof String[])
                    toPrint = Arrays.toString((String[]) value);
                else if (value instanceof short[])
                    toPrint = Arrays.toString((short[]) value);
                else if (value instanceof byte[])
                    toPrint = Arrays.toString((byte[]) value);
                else if (value instanceof char[])
                    toPrint = Arrays.toString((char[]) value);
            }
            Log.d(TAG, "key = " + entry.getKey() + ", value = " + toPrint);
        }
    }

    /**
     * Determine whether an <code>Object</code> is able to be casted to another class.
     *
     * @return <code>true</code> if the object is able to be casted.
     */
    private boolean isCompatibleCast(String key, Class<?> classToCast) {
        Object obj = mValues.get(key);
        try {
            if (mValues.get(key) instanceof Number && obj == null) {
                Log.e(TAG, "Cannot cast null value to a " + classToCast.getSimpleName() + " number format.");
                return false;
            }

            classToCast.cast(obj);
            return true;
        } catch (ClassCastException e) {
            Log.e(TAG, "Cannot cast object value from " + obj + " to " + classToCast.getSimpleName() + " for key '" + key + "'", e);
            return false;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(mValues);
    }
}
