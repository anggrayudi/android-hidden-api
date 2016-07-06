package com.anggrayudi.hiddenapi;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;

import com.anggrayudi.hiddenapi.r.versions.R_10;
import com.anggrayudi.hiddenapi.r.versions.R_16;
import com.anggrayudi.hiddenapi.r.versions.R_17;
import com.anggrayudi.hiddenapi.r.versions.R_21;
import com.anggrayudi.hiddenapi.r.versions.R_22;
import com.anggrayudi.hiddenapi.r.versions.R_23;

import java.io.InputStream;
import java.lang.reflect.Method;

/**
 * Created by Anggrayudi on 11/03/2016.<p>
 * A utility class that provides access to all resources in <code>com.android.internal.R</code> class.
 * This class does not use Java reflection anymore, and certainly safe.
 *
 *     You can find which resources are available for current API level. See:
 *     <ul>
 *         <li>{@link R_10} - for API 10</li>
 *         <li>{@link R_16} - for API 16</li>
 *         <li>{@link R_17} - for API 17</li>
 *         <li>{@link R_21} - for API 21</li>
 *         <li>{@link R_22} - for API 22</li>
 *         <li>{@link R_23} - for API 23</li>
 *     </ul>
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

    public static final String ANIM = "anim";
    public static final String ARRAY = "array";
    public static final String ATTR = "attr";
    public static final String BOOL = "bool";
    public static final String COLOR = "color";
    public static final String DIMEN = "dimen";
    public static final String DRAWABLE = "drawable";
    public static final String ID = "id";
    public static final String INTEGER = "integer";
    public static final String LAYOUT = "layout";
    public static final String PLURALS = "plurals";
    public static final String RAW = "raw";
    public static final String STRING = "string";
    public static final String STYLE = "style";
    public static final String STYLEABLE = "styleable";
    public static final String XML = "xml";
    public static final String FRACTION = "fraction";
    public static final String INTERPOLATOR = "interpolator";
    public static final String MENU = "menu";
    public static final String MIPMAP = "mipmap";
    public static final String TRANSITION = "transition";

    /**
     * Pick resource id from <code>com.android.internal.R</code>
     * @param clas for example = <code>string, drawable, color, style, id, attr</code>.
     *             And they will be wrote as <code>com.android.internal.R.drawable</code>
     * @param resName for example = <code>cancel, ic_launcher, activity_main</code>
     * @return internal resource id in integer, for example: <code>com.android.internal.R.xml.audio_assets</code>
     * returns <code>17891329</code> in integer.
     */
    public static int getResourceId(@NonNull String clas, @NonNull String resName) throws ClassNotFoundException, ResourceNotFoundException {
        int id = Resources.getSystem().getIdentifier(resName, clas, "android");
        if (id == 0)
            throw new ResourceNotFoundException("Cannot find '"+clas+"' for '"+resName+
                    "', or this resource currently is not available for API "+ Build.VERSION.SDK_INT);

        switch (clas) {
            case ANIM:
            case ARRAY:
            case ATTR:
            case BOOL:
            case COLOR:
            case DIMEN:
            case DRAWABLE:
            case ID:
            case INTEGER:
            case LAYOUT:
            case PLURALS:
            case RAW:
            case STRING:
            case STYLE:
            case STYLEABLE:
            case XML:
                return id;

            case FRACTION:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    throw new ClassNotFoundException("Class com.android.internal.R.fraction only available for API 11 and higher.");
                break;

            case INTERPOLATOR:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                    throw new ClassNotFoundException("Class com.android.internal.R.interpolator only available for API 21 and higher.");
                break;

            case MENU:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    throw new ClassNotFoundException("Class com.android.internal.R.menu only available for API 11 and higher.");
                break;

            case MIPMAP:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                    throw new ClassNotFoundException("Class com.android.internal.R.mipmap only available for API 11 and higher.");
                break;

            case TRANSITION:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                    throw new ClassNotFoundException("Class com.android.internal.R.transition only available for API 21 and higher.");
                break;
        }
        return id;
    }

    /**
     * Pick Animation resources from <code>com.android.internal.R.anim</code>
     * @param resName for example = <code>snackbar_in, abc_fade_in</code>
     * @return internal <code>XmlResourceParser</code> animation
     */
    public static XmlResourceParser getAnimation(@NonNull String resName) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getAnimation(getResourceId(ANIM, resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick <code>boolean</code> resources from <code>com.android.internal.R.bool</code>
     * @return internal <code>boolean</code>
     */
    public static boolean getBoolean(@NonNull String resName) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getBoolean(getResourceId(BOOL, resName));
        } catch (ClassNotFoundException e) {return false;}
    }

    /**
     * Pick color resources from <code>com.android.internal.R.color</code>
     * @return internal color
     */
    @SuppressWarnings("deprecation")
    public static int getColor(@NonNull String resName) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getColor(getResourceId(COLOR, resName));
        } catch (ClassNotFoundException e) {return 0;}
    }

    /**
     * Pick dimension resources from <code>com.android.internal.R.dimen</code>
     * @return internal dimension
     */
    public static float getDimension(@NonNull String resName) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getDimension(getResourceId(DIMEN, resName));
        } catch (ClassNotFoundException e) {return 0;}
    }

    /**
     * Pick <code>Drawable</code> resources from <code>com.android.internal.R.drawable</code>
     * @return internal <code>Drawable</code>
     */
    @SuppressWarnings("deprecation")
    public static Drawable getDrawable(@NonNull String resName) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getDrawable(getResourceId(DRAWABLE, resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick <code>Drawable</code> resources from <code>com.android.internal.R.drawable</code>
     * @return internal <code>Drawable</code>
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Drawable getDrawable(@NonNull String resName, Resources.Theme theme) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getDrawable(getResourceId(DRAWABLE, resName), theme);
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick fractional unit resources from <code>com.android.internal.R.fraction</code>
     * @return internal fractional unit
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static float getFraction(@NonNull String resName, int base, int pbase) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getFraction(getResourceId(FRACTION, resName), base, pbase);
        } catch (ClassNotFoundException e) {return 0;}
    }

    /**
     * Pick <code>Integer</code> resources from <code>com.android.internal.R.integer</code>
     * @return internal <code>Integer</code>
     */
    public static int getInteger(@NonNull String resName) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getInteger(getResourceId(INTEGER, resName));
        } catch (ClassNotFoundException e) {return 0;}
    }

    /**
     * Pick layout resources from <code>com.android.internal.R.layout</code>
     * @return internal <code>XmlResourceParser</code> layout
     */
    public static XmlResourceParser getLayout(@NonNull String resName) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getLayout(getResourceId(LAYOUT, resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick menu resources from <code>com.android.internal.R.menu</code>
     * @return internal <code>XmlResourceParser</code> menu
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static XmlResourceParser getMenu(@NonNull String resName) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getLayout(getResourceId(MENU, resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick mipmap drawable resources from <code>com.android.internal.R.mipmap</code>
     * @return internal <code>Drawable</code> mipmap
     */
    @SuppressWarnings("deprecation")
    public static Drawable getMipmap(@NonNull String resName) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getDrawable(getResourceId(MIPMAP, resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick mipmap drawable resources from <code>com.android.internal.R.mipmap</code>
     * @return internal <code>Drawable</code> mipmap
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static Drawable getMipmap(@NonNull String resName, Resources.Theme theme) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getDrawable(getResourceId(MIPMAP, resName), theme);
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick RAW resources from <code>com.android.internal.R.raw</code>
     * @return internal <code>InputStream</code> raw
     */
    public static InputStream getRaw(@NonNull String resName) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().openRawResource(getResourceId(RAW, resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick <code>Integer</code> array resources from <code>com.android.internal.R.array</code>
     * @return internal <code>Integer</code> array
     */
    public static int[] getIntArray(@NonNull String resName) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getIntArray(getResourceId(ARRAY, resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick <code>String</code> array resources from <code>com.android.internal.R.array</code>
     * @param resName for example = <code>emailAddressTypes</code>
     * @return internal <code>String</code> array
     */
    public static String[] getStringArray(@NonNull String resName) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getStringArray(getResourceId(ARRAY, resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick string resources from <code>com.android.internal.R.string</code>
     * @param resName for example = <code>accept, cancel, upload_file, find_on_page</code>
     * @return internal <code>String</code>
     */
    public static String getString(@NonNull String resName) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getString(getResourceId(STRING, resName));
        } catch (ClassNotFoundException e) {return null;}
    }

    /**
     * Pick XML resources from <code>com.android.internal.R.xml</code>
     * @return internal <code>XmlResourceParser</code> XML
     */
    public static XmlResourceParser getXml(@NonNull String resName) throws ResourceNotFoundException {
        try {
            return Resources.getSystem().getXml(getResourceId(XML, resName));
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

    /**
     * @deprecated since we don't use <code>Context</code> and Java reflection to get the internal resources, so using this
     * <code>Builder</code> is redundant. Get them directly without storing to this class or {@link ResourcesHolder}
     */
    public static class Builder {

        final boolean saveToResourcesHolder;
        ResourcesHolder holder = new ResourcesHolder();

        /**
         * @param saveToResourcesHolder determine whether once we get the value also store it to
         *                              <code>ResourcesHolder</code> automatically. The key for every data stored to
         *                              <code>ResourcesHolder</code> always uses <code>resName</code> argument of the method.
         */
        public Builder(boolean saveToResourcesHolder){
            this.saveToResourcesHolder = saveToResourcesHolder;
        }

        public ResourcesHolder getResourcesHolder(){
            return holder;
        }

        /**
         * Replacement for {@link InternalAccessor#getAnimation(String)}
         */
        public XmlResourceParser getAnimation(@NonNull String resName) throws ResourceNotFoundException {
            XmlResourceParser anim = InternalAccessor.getAnimation(resName);
            if (saveToResourcesHolder)
                holder.put(resName, anim);
            return anim;
        }

        /**
         * Replacement for {@link InternalAccessor#getBoolean(String)}
         */
        public boolean getBoolean(@NonNull String resName) throws ResourceNotFoundException {
            boolean bol = InternalAccessor.getBoolean(resName);
            if (saveToResourcesHolder)
                holder.put(resName, bol);
            return bol;
        }

        /**
         * Replacement for {@link InternalAccessor#getColor(String)}
         */
        public int getColor(@NonNull String resName) throws ResourceNotFoundException {
            int color = InternalAccessor.getColor(resName);
            if (saveToResourcesHolder)
                holder.put(resName, color);
            return color;
        }

        /**
         * Replacement for {@link InternalAccessor#getDimension(String)}
         */
        public float getDimension(@NonNull String resName) throws ResourceNotFoundException {
            float dimens = InternalAccessor.getDimension(resName);
            if (saveToResourcesHolder)
                holder.put(resName, dimens);
            return dimens;
        }

        /**
         * Replacement for {@link InternalAccessor#getDrawable(String)}
         */
        public Drawable getDrawable(@NonNull String resName) throws ResourceNotFoundException {
            Drawable drawable = InternalAccessor.getDrawable(resName);
            if (saveToResourcesHolder)
                holder.put(resName, drawable);
            return drawable;
        }

        /**
         * Replacement for {@link InternalAccessor#getFraction(String, int, int)}
         */
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public float getFraction(@NonNull String resName, int base, int pbase) throws ResourceNotFoundException {
            float fract = InternalAccessor.getFraction(resName, base, pbase);
            if (saveToResourcesHolder)
                holder.put(resName, fract);
            return fract;
        }

        /**
         * Replacement for {@link InternalAccessor#getInteger(String)}
         */
        public int getInteger(@NonNull String resName) throws ResourceNotFoundException {
            int integer = InternalAccessor.getInteger(resName);
            if (saveToResourcesHolder)
                holder.put(resName, integer);
            return integer;
        }

        /**
         * Replacement for {@link InternalAccessor#getLayout(String)}
         */
        public XmlResourceParser getLayout(@NonNull String resName) throws ResourceNotFoundException {
            XmlResourceParser layout = InternalAccessor.getLayout(resName);
            if (saveToResourcesHolder)
                holder.put(resName, layout);
            return layout;
        }

        /**
         * Replacement for {@link InternalAccessor#getMenu(String)}
         */
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public XmlResourceParser getMenu(@NonNull String resName) throws ResourceNotFoundException {
            XmlResourceParser menu = InternalAccessor.getMenu(resName);
            if (saveToResourcesHolder)
                holder.put(resName, menu);
            return menu;
        }

        /**
         * Replacement for {@link InternalAccessor#getMipmap(String)}
         */
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        public Drawable getMipmap(@NonNull String resName) throws ResourceNotFoundException {
            Drawable mipmap = InternalAccessor.getMipmap(resName);
            if (saveToResourcesHolder)
                holder.put(resName, mipmap);
            return mipmap;
        }

        /**
         * Replacement for {@link InternalAccessor#getRaw(String)}
         */
        public InputStream getRaw(@NonNull String resName) throws ResourceNotFoundException {
            InputStream stream = InternalAccessor.getRaw(resName);
            if (saveToResourcesHolder)
                holder.put(resName, stream);
            return stream;
        }

        /**
         * Replacement for {@link InternalAccessor#getIntArray(String)}
         */
        public int[] getIntArray(@NonNull String resName) throws ResourceNotFoundException {
            int[] ints = InternalAccessor.getIntArray(resName);
            if (saveToResourcesHolder)
                holder.put(resName, ints);
            return ints;
        }

        /**
         * Replacement for {@link InternalAccessor#getStringArray(String)}
         */
        public String[] getStringArray(@NonNull String resName) throws ResourceNotFoundException {
            String[] strings = InternalAccessor.getStringArray(resName);
            if (saveToResourcesHolder)
                holder.put(resName, strings);
            return strings;
        }

        /**
         * Replacement for {@link InternalAccessor#getString(String)}
         */
        public String getString(@NonNull String resName) throws ResourceNotFoundException {
            String string = InternalAccessor.getString(resName);
            if (saveToResourcesHolder)
                holder.put(resName, string);
            return string;
        }

        /**
         * Replacement for {@link InternalAccessor#getXml(String)}
         */
        public XmlResourceParser getXml(@NonNull String resName) throws ResourceNotFoundException {
            XmlResourceParser xml = InternalAccessor.getXml(resName);
            if (saveToResourcesHolder)
                holder.put(resName, xml);
            return xml;
        }
    }
}
