package com.anggrayudi.hiddenapi;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import java.io.InputStream;

/**
 * Created by Anggrayudi on 11/03/2016.<p>
 * A utility class that provides access to all resources in <code>com.android.internal.R</code> class.
 * <p>If you want to retrieve the same value again, don't get it by using this utility class. Save
 * to {@link ResourcesHolder} instead. Also, you can send <code>ResourcesHolder</code> class object to another class via:
 * <code>Intent.putExtra(String key, ResourcesHolder instance)</code> to avoid re-reflection to the same value.<p>
 *
 *     You can find which resources are available for current API level. See:
 *     <ul>
 *         <li>{@link com.anggrayudi.hiddenapi.r.R_10} - for API 10</li>
 *         <li>{@link com.anggrayudi.hiddenapi.r.R_16} - for API 16</li>
 *         <li>{@link com.anggrayudi.hiddenapi.r.R_17} - for API 17</li>
 *         <li>{@link com.anggrayudi.hiddenapi.r.R_21} - for API 21</li>
 *         <li>{@link com.anggrayudi.hiddenapi.r.R_22} - for API 22</li>
 *     </ul>
 */
@SuppressWarnings({"TryWithIdenticalCatches", "EmptyCatchBlock", "JavaDoc"})
public final class InternalAccessor {

    // These are primitive classes which are available since API 7
    private static final Class<?> anim          = com.android.internal.R.anim.class;
    private static final Class<?> array         = com.android.internal.R.array.class;
    private static final Class<?> attr          = com.android.internal.R.attr.class;
    private static final Class<?> bool          = com.android.internal.R.bool.class;
    private static final Class<?> color         = com.android.internal.R.color.class;
    private static final Class<?> dimen         = com.android.internal.R.dimen.class;
    private static final Class<?> drawable      = com.android.internal.R.drawable.class;
    private static final Class<?> id            = com.android.internal.R.id.class;
    private static final Class<?> integer       = com.android.internal.R.integer.class;
    private static final Class<?> layout        = com.android.internal.R.layout.class;
    private static final Class<?> plurals       = com.android.internal.R.plurals.class;
    private static final Class<?> raw           = com.android.internal.R.raw.class;
    private static final Class<?> string        = com.android.internal.R.string.class;
    private static final Class<?> style         = com.android.internal.R.style.class;
    private static final Class<?> styleable     = com.android.internal.R.styleable.class;
    private static final Class<?> xml           = com.android.internal.R.xml.class;

    // These are up to date classes and may not available in API 10
//    private static final Class<?> fraction      = com.android.internal.R.fraction.class;
//    private static final Class<?> interpolator  = com.android.internal.R.interpolator.class;
//    private static final Class<?> menu          = com.android.internal.R.menu.class;
//    private static final Class<?> mipmap        = com.android.internal.R.mipmap.class;
//    private static final Class<?> transition    = com.android.internal.R.transition.class;

    /**
     * Pick resource id from <code>com.android.internal.R</code> via reflection.
     * @param clas for example = <code>string, drawable, color, style, id, attr</code>.
     *             And they will be wrote as <code>com.android.internal.R.drawable</code>
     * @param resName for example = <code>cancel, ic_launcher, activity_main</code>
     * @return internal resource id in integer, for example: <code>com.android.internal.R.xml.audio_assets</code>
     * returns <code>17891329</code> in integer.
     */
    public static int getResourceId(@NonNull String clas, @NonNull String resName) throws ClassNotFoundException, ResourceNotFoundException {
        int id = 0;
        try {
            clas = clas.trim().toLowerCase();
            switch (clas){
                case "anim":
                    id = anim.getField(resName).getInt(null);
                    break;
                case "array":
                    id = array.getField(resName).getInt(null);
                    break;
                case "attr":
                    id = attr.getField(resName).getInt(null);
                    break;
                case "bool":
                    id = bool.getField(resName).getInt(null);
                    break;
                case "color":
                    id = color.getField(resName).getInt(null);
                    break;
                case "dimen":
                    id = dimen.getField(resName).getInt(null);
                    break;
                case "drawable":
                    id = drawable.getField(resName).getInt(null);
                    break;
                case "fraction":
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                        throw new ClassNotFoundException("Class com.android.internal.R.fraction only available for API 11 and higher.");

                    id = com.android.internal.R.fraction.class.getField(resName).getInt(null);
                    break;
                case "id":
                    id = InternalAccessor.id.getField(resName).getInt(null);
                    break;
                case "integer":
                    id = integer.getField(resName).getInt(null);
                    break;
                case "interpolator":
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                        throw new ClassNotFoundException("Class com.android.internal.R.interpolator only available for API 21 and higher.");

                    id = com.android.internal.R.interpolator.class.getField(resName).getInt(null);
                    break;
                case "layout":
                    id = layout.getField(resName).getInt(null);
                    break;
                case "menu":
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                        throw new ClassNotFoundException("Class com.android.internal.R.menu only available for API 11 and higher.");

                    id = com.android.internal.R.menu.class.getField(resName).getInt(null);
                    break;
                case "mipmap":
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                        throw new ClassNotFoundException("Class com.android.internal.R.mipmap only available for API 11 and higher.");

                    id = com.android.internal.R.mipmap.class.getField(resName).getInt(null);
                    break;
                case "plurals":
                    id = plurals.getField(resName).getInt(null);
                    break;
                case "raw":
                    id = raw.getField(resName).getInt(null);
                    break;
                case "string":
                    id = string.getField(resName).getInt(null);
                    break;
                case "style":
                    id = style.getField(resName).getInt(null);
                    break;
                case "styleable":
                    id = styleable.getField(resName).getInt(null);
                    break;
                case "transition":
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                        throw new ClassNotFoundException("Class com.android.internal.R.transition only available for API 21 and higher.");

                    id = com.android.internal.R.transition.class.getField(resName).getInt(null);
                    break;
                case "xml":
                    id = xml.getField(resName).getInt(null);
                    break;
            }
        } catch (NoSuchFieldException e) {
            throw new ResourceNotFoundException(errorMessage(clas, resName));
        } catch (NoClassDefFoundError e){} catch (IllegalAccessException e) {}
        return id;
    }

    /**
     * Pick Animation resources from <code>com.android.internal.R.anim</code> via reflection.
     * @param resName for example = <code>snackbar_in, abc_fade_in</code>
     * @return internal <code>XmlResourceParser</code> animation
     */
    public static XmlResourceParser getAnimation(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getAnimation(getResourceId("anim", resName));
        } catch (ClassNotFoundException e) {}
        return null;
    }

    /**
     * Pick <code>boolean</code> resources from <code>com.android.internal.R.bool</code> via reflection.
     * @return internal <code>boolean</code>
     */
    public static boolean getBoolean(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getBoolean(getResourceId("bool", resName));
        } catch (ClassNotFoundException e) {}
        return false;
    }

    /**
     * Pick color resources from <code>com.android.internal.R.color</code> via reflection.
     * @return internal color
     */
    public static int getColor(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getColor(getResourceId("color", resName));
        } catch (ClassNotFoundException e) {}
        return 0;
    }

    /**
     * Pick dimension resources from <code>com.android.internal.R.dimen</code> via reflection.
     * @return internal dimension
     */
    public static float getDimension(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getDimension(getResourceId("dimen", resName));
        } catch (ClassNotFoundException e) {}
        return 0;
    }

    /**
     * Pick <code>Drawable</code> resources from <code>com.android.internal.R.drawable</code> via reflection.
     * @return internal <code>Drawable</code>
     */
    public static Drawable getDrawable(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return ContextCompat.getDrawable(context, getResourceId("drawable", resName));
        } catch (ClassNotFoundException e) {}
        return null;
    }

    /**
     * Pick fractional unit resources from <code>com.android.internal.R.fraction</code> via reflection.
     * @return internal fractional unit
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static float getFraction(Context context, @NonNull String resName, int base, int pbase) throws ResourceNotFoundException {
        try {
            return context.getResources().getFraction(getResourceId("fraction", resName), base, pbase);
        } catch (ClassNotFoundException e) {}
        return 0;
    }

    /**
     * Pick <code>Integer</code> resources from <code>com.android.internal.R.integer</code> via reflection.
     * @return internal <code>Integer</code>
     */
    public static int getInteger(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getInteger(getResourceId("integer", resName));
        } catch (ClassNotFoundException e) {}
        return 0;
    }

    /**
     * Pick layout resources from <code>com.android.internal.R.layout</code> via reflection.
     * @return internal <code>XmlResourceParser</code> layout
     */
    public static XmlResourceParser getLayout(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getLayout(getResourceId("layout", resName));
        } catch (ClassNotFoundException e) {}
        return null;
    }

    /**
     * Pick menu resources from <code>com.android.internal.R.menu</code> via reflection.
     * @return internal <code>XmlResourceParser</code> menu
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static XmlResourceParser getMenu(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getLayout(getResourceId("menu", resName));
        } catch (ClassNotFoundException e) {}
        return null;
    }

    /**
     * Pick mipmap drawable resources from <code>com.android.internal.R.mipmap</code> via reflection.
     * @return internal <code>Drawable</code> mipmap
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static Drawable getMipmap(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getDrawable(getResourceId("mipmap", resName));
        } catch (ClassNotFoundException e) {}
        return null;
    }

    /**
     * Pick RAW resources from <code>com.android.internal.R.raw</code> via reflection.
     * @return internal <code>InputStream</code> raw
     */
    public static InputStream getRaw(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().openRawResource(getResourceId("raw", resName));
        } catch (ClassNotFoundException e) {}
        return null;
    }

    /**
     * Pick <code>Integer</code> array resources from <code>com.android.internal.R.array</code> via reflection.
     * @return internal <code>Integer</code> array
     */
    public static int[] getIntArray(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getIntArray(getResourceId("array", resName));
        } catch (ClassNotFoundException e) {}
        return null;
    }

    /**
     * Pick <code>String</code> array resources from <code>com.android.internal.R.array</code> via reflection.
     * @param resName for example = <code>emailAddressTypes</code>
     * @return internal <code>String</code> array
     */
    public static String[] getStringArray(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getStringArray(getResourceId("array", resName));
        } catch (ClassNotFoundException e) {}
        return null;
    }

    /**
     * Pick string resources from <code>com.android.internal.R.string</code> via reflection.
     * @param resName for example = <code>accept, cancel, upload_file, find_on_page</code>
     * @return internal <code>String</code>
     */
    public static String getString(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getString(getResourceId("string", resName));
        } catch (ClassNotFoundException e) {}
        return null;
    }

    /**
     * Pick XML resources from <code>com.android.internal.R.xml</code> via reflection.
     * @return internal <code>XmlResourceParser</code> XML
     */
    public static XmlResourceParser getXml(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getXml(getResourceId("xml", resName));
        } catch (ClassNotFoundException e) {}
        return null;
    }

    private static String errorMessage(String type, String resIdName){
        return "Resource '"+type+"' is not found for '"+resIdName+
                "', or this resource currently is not available for API "+ Build.VERSION.SDK_INT;
    }
}
