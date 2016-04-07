package com.anggrayudi.hiddenapi;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import java.io.InputStream;
import java.lang.reflect.Method;

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
 *
 * <p>If you are bored about writing context in every static method of this class, you can do
 * the following (for example):
 * <pre>
 *     InternalAccessor.Builder accessor = new InternalAccessor.Builder(this);
 *     boolean b = accessor.getBoolean("screen_rotate_0_enter");
 *     String accept = accessor.getString("accept");
 * </pre>
 */
@SuppressWarnings({"TryWithIdenticalCatches", "EmptyCatchBlock", "JavaDoc"})
public final class InternalAccessor {

    private InternalAccessor(){}

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
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick <code>boolean</code> resources from <code>com.android.internal.R.bool</code> via reflection.
     * @return internal <code>boolean</code>
     */
    public static boolean getBoolean(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getBoolean(getResourceId("bool", resName));
        } catch (ClassNotFoundException e) {return false;}
    }

    /**
     * Pick color resources from <code>com.android.internal.R.color</code> via reflection.
     * @return internal color
     */
    public static int getColor(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getColor(getResourceId("color", resName));
        } catch (ClassNotFoundException e) {return 0;}
    }

    /**
     * Pick dimension resources from <code>com.android.internal.R.dimen</code> via reflection.
     * @return internal dimension
     */
    public static float getDimension(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getDimension(getResourceId("dimen", resName));
        } catch (ClassNotFoundException e) {return 0;}
    }

    /**
     * Pick <code>Drawable</code> resources from <code>com.android.internal.R.drawable</code> via reflection.
     * @return internal <code>Drawable</code>
     */
    public static Drawable getDrawable(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return ContextCompat.getDrawable(context, getResourceId("drawable", resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick fractional unit resources from <code>com.android.internal.R.fraction</code> via reflection.
     * @return internal fractional unit
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static float getFraction(Context context, @NonNull String resName, int base, int pbase) throws ResourceNotFoundException {
        try {
            return context.getResources().getFraction(getResourceId("fraction", resName), base, pbase);
        } catch (ClassNotFoundException e) {return 0;}
    }

    /**
     * Pick <code>Integer</code> resources from <code>com.android.internal.R.integer</code> via reflection.
     * @return internal <code>Integer</code>
     */
    public static int getInteger(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getInteger(getResourceId("integer", resName));
        } catch (ClassNotFoundException e) {return 0;}
    }

    /**
     * Pick layout resources from <code>com.android.internal.R.layout</code> via reflection.
     * @return internal <code>XmlResourceParser</code> layout
     */
    public static XmlResourceParser getLayout(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getLayout(getResourceId("layout", resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick menu resources from <code>com.android.internal.R.menu</code> via reflection.
     * @return internal <code>XmlResourceParser</code> menu
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static XmlResourceParser getMenu(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getLayout(getResourceId("menu", resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick mipmap drawable resources from <code>com.android.internal.R.mipmap</code> via reflection.
     * @return internal <code>Drawable</code> mipmap
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static Drawable getMipmap(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getDrawable(getResourceId("mipmap", resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick RAW resources from <code>com.android.internal.R.raw</code> via reflection.
     * @return internal <code>InputStream</code> raw
     */
    public static InputStream getRaw(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().openRawResource(getResourceId("raw", resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick <code>Integer</code> array resources from <code>com.android.internal.R.array</code> via reflection.
     * @return internal <code>Integer</code> array
     */
    public static int[] getIntArray(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getIntArray(getResourceId("array", resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick <code>String</code> array resources from <code>com.android.internal.R.array</code> via reflection.
     * @param resName for example = <code>emailAddressTypes</code>
     * @return internal <code>String</code> array
     */
    public static String[] getStringArray(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getStringArray(getResourceId("array", resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick string resources from <code>com.android.internal.R.string</code> via reflection.
     * @param resName for example = <code>accept, cancel, upload_file, find_on_page</code>
     * @return internal <code>String</code>
     */
    public static String getString(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getString(getResourceId("string", resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick XML resources from <code>com.android.internal.R.xml</code> via reflection.
     * @return internal <code>XmlResourceParser</code> XML
     */
    public static XmlResourceParser getXml(Context context, @NonNull String resName) throws ResourceNotFoundException {
        try {
            return context.getResources().getXml(getResourceId("xml", resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Check whether a class exists, and do your action when it's return <code>true</code>.
     * @param className class name with its package, e.g. <code>android.content.Intent</code>
     * @return true if the class exists
     */
    public static boolean isClassExists(String className){
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Check whether a method exists within its class. An example for this method is:
     * <pre>
     * boolean methodExists = InternalAccessor.isMethodExists("android.content.Intent", "putExtra", String.class, Integer.class);
     * if (methodExists){
     *     ...
     * }
     * </pre>
     * where <code>putExtra()</code> has two types of parameter, i.e. <code>String</code> and <code>Integer</code>.
     * If you look at the source code, you'll see that the real method is: <code>putExtra(String name, int value)</code>.
     * <p>See {@link InternalAccessor#isMethodExists(String, String)}</p>
     * @param className class name with its package, e.g. <code>android.content.Intent</code>
     * @param methodName method name within its class, e.g. <code>putExtra</code>
     * @param parameterTypes class of the method's parameter type, e.g. for <code>putExtra(String name, int value)</code>,
     *                       you should type <code>String.class, Integer.class</code>
     * @return <code>true</code> if the method exists, <code>false</code> otherwise or the class could not be found
     */
    public static boolean isMethodExists(String className, String methodName, Class<?>... parameterTypes){
        try {
            Class<?> cls = Class.forName(className);
            cls.getMethod(methodName, parameterTypes);
            return true;
        } catch (ClassNotFoundException e) {} catch (NoSuchMethodException e) {}
        return false;
    }

    /**
     * Check whether a method exists without checking its parameter types. This is similar with
     * {@link InternalAccessor#isMethodExists(String, String, Class[])}
     * @param className class name with its package, e.g. <code>android.content.Intent</code>
     * @param methodName method name within its class, e.g. <code>putExtra</code>
     * @return <code>true</code> if the method exists, <code>false</code> otherwise or the class could not be found
     */
    public static boolean isMethodExists(String className, String methodName){
        try {
            Class<?> cls = Class.forName(className);
            for (Method method : cls.getDeclaredMethods())
                if (method.getName().equals(methodName))
                    return true;
        } catch (ClassNotFoundException e) {}
        return false;
    }

    private static String errorMessage(String type, String resIdName){
        return "Resource '"+type+"' is not found for '"+resIdName+
                "', or this resource currently is not available for API "+ Build.VERSION.SDK_INT;
    }

    public static class Builder {

        final Context context;
        final boolean saveToResourcesHolder;
        ResourcesHolder holder = new ResourcesHolder();

        /**
         * @param saveToResourcesHolder determine whether once we get the value also store it to
         *                              <code>ResourcesHolder</code> automatically. The key for every data stored to
         *                              <code>ResourcesHolder</code> always uses <code>resName</code> argument of the method.
         */
        public Builder(Context context, boolean saveToResourcesHolder){
            this.context = context;
            this.saveToResourcesHolder = saveToResourcesHolder;
        }

        public ResourcesHolder getResourcesHolder(){
            return holder;
        }

        /**
         * Replacement for {@link InternalAccessor#getAnimation(Context, String)}
         */
        public XmlResourceParser getAnimation(@NonNull String resName) throws ResourceNotFoundException {
            XmlResourceParser anim = InternalAccessor.getAnimation(context, resName);
            if (saveToResourcesHolder)
                holder.put(resName, anim);
            return anim;
        }

        /**
         * Replacement for {@link InternalAccessor#getBoolean(Context, String)}
         */
        public boolean getBoolean(@NonNull String resName) throws ResourceNotFoundException {
            boolean bol = InternalAccessor.getBoolean(context, resName);
            if (saveToResourcesHolder)
                holder.put(resName, bol);
            return bol;
        }

        /**
         * Replacement for {@link InternalAccessor#getColor(Context, String)}
         */
        public int getColor(@NonNull String resName) throws ResourceNotFoundException {
            int color = InternalAccessor.getColor(context, resName);
            if (saveToResourcesHolder)
                holder.put(resName, color);
            return color;
        }

        /**
         * Replacement for {@link InternalAccessor#getDimension(Context, String)}
         */
        public float getDimension(@NonNull String resName) throws ResourceNotFoundException {
            float dimens = InternalAccessor.getDimension(context, resName);
            if (saveToResourcesHolder)
                holder.put(resName, dimens);
            return dimens;
        }

        /**
         * Replacement for {@link InternalAccessor#getDrawable(Context, String)}
         */
        public Drawable getDrawable(@NonNull String resName) throws ResourceNotFoundException {
            Drawable drawable = InternalAccessor.getDrawable(context, resName);
            if (saveToResourcesHolder)
                holder.put(resName, drawable);
            return drawable;
        }

        /**
         * Replacement for {@link InternalAccessor#getFraction(Context, String, int, int)}
         */
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public float getFraction(@NonNull String resName, int base, int pbase) throws ResourceNotFoundException {
            float fract = InternalAccessor.getFraction(context, resName, base, pbase);
            if (saveToResourcesHolder)
                holder.put(resName, fract);
            return fract;
        }

        /**
         * Replacement for {@link InternalAccessor#getInteger(Context, String)}
         */
        public int getInteger(@NonNull String resName) throws ResourceNotFoundException {
            int integer = InternalAccessor.getInteger(context, resName);
            if (saveToResourcesHolder)
                holder.put(resName, integer);
            return integer;
        }

        /**
         * Replacement for {@link InternalAccessor#getLayout(Context, String)}
         */
        public XmlResourceParser getLayout(@NonNull String resName) throws ResourceNotFoundException {
            XmlResourceParser layout = InternalAccessor.getLayout(context, resName);
            if (saveToResourcesHolder)
                holder.put(resName, layout);
            return layout;
        }

        /**
         * Replacement for {@link InternalAccessor#getMenu(Context, String)}
         */
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public XmlResourceParser getMenu(@NonNull String resName) throws ResourceNotFoundException {
            XmlResourceParser menu = InternalAccessor.getMenu(context, resName);
            if (saveToResourcesHolder)
                holder.put(resName, menu);
            return menu;
        }

        /**
         * Replacement for {@link InternalAccessor#getMipmap(Context, String)}
         */
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public Drawable getMipmap(@NonNull String resName) throws ResourceNotFoundException {
            Drawable mipmap = InternalAccessor.getMipmap(context, resName);
            if (saveToResourcesHolder)
                holder.put(resName, mipmap);
            return mipmap;
        }

        /**
         * Replacement for {@link InternalAccessor#getRaw(Context, String)}
         */
        public InputStream getRaw(@NonNull String resName) throws ResourceNotFoundException {
            InputStream stream = InternalAccessor.getRaw(context, resName);
            if (saveToResourcesHolder)
                holder.put(resName, stream);
            return stream;
        }

        /**
         * Replacement for {@link InternalAccessor#getIntArray(Context, String)}
         */
        public int[] getIntArray(@NonNull String resName) throws ResourceNotFoundException {
            int[] ints = InternalAccessor.getIntArray(context, resName);
            if (saveToResourcesHolder)
                holder.put(resName, ints);
            return ints;
        }

        /**
         * Replacement for {@link InternalAccessor#getStringArray(Context, String)}
         */
        public String[] getStringArray(@NonNull String resName) throws ResourceNotFoundException {
            String[] strings = InternalAccessor.getStringArray(context, resName);
            if (saveToResourcesHolder)
                holder.put(resName, strings);
            return strings;
        }

        /**
         * Replacement for {@link InternalAccessor#getString(Context, String)}
         */
        public String getString(@NonNull String resName) throws ResourceNotFoundException {
            String string = InternalAccessor.getString(context, resName);
            if (saveToResourcesHolder)
                holder.put(resName, string);
            return string;
        }

        /**
         * Replacement for {@link InternalAccessor#getXml(Context, String)}
         */
        public XmlResourceParser getXml(@NonNull String resName) throws ResourceNotFoundException {
            XmlResourceParser xml = InternalAccessor.getXml(context, resName);
            if (saveToResourcesHolder)
                holder.put(resName, xml);
            return xml;
        }
    }
}
