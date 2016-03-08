# Android Hidden API
**Android Hidden API** is a modified jar file which combining `android.jar` from Android SDK with `framework.jar` from real device. This jar makes you able to use Android internal/hidden APIs in development.

**What is Android internal and hidden APIs?**
Internal API is located in `com.android.internal` package which available in the `framework.jar` file from real Android device, while hidden API is located in `android.jar` file with *@hide* javadoc attribute. Although the classes & methods are `public`, but you cannot access it. There are pretty methods and resources you can use from this package. I will assume this is one API and will refer to it as to hidden API. Learn more about hidden API [**here**][1].

**What's the advantage?**
You don't need to create new classes, constants, methods or resources which you want to use the similar function in your project. For example, if you want to create a new `String` resource that contains 'day' word. Then you create a new translation to other language, e.g. Arabic, Indonesian, even with all languages in this world. This is too exhausting and waste your time. Why don't use `com.android.internal.R.string.durationDayHour` which already available from Custom Android framework.jar?

#Usage
1. Close your Android Studio.
2. Go to `<SDK location>/platforms/`.
3. Copy, paste and replace the downloaded file into this directory, e.g. `android-21`. Note: I already make a backup that located in original folder.
4. Open Android Studio, change `compileSdkVersion` and `targetSdkVersion` to 21 (for example).
5. Finally, rebuild your project.

Note: Higher `compileSdkVersion` and `targetSdkVersion` version will be better.


**LIMITATION**

Currently, Android Hidden API is not available for Marshmallow 6.0, because of I can't find any people who has this device. If you have it, please upload `framework.jar` file from real device that located in `/system/framework/framework.jar` to me. I will make a new!


  [1]: https://devmaze.wordpress.com/2011/01/18/using-com-android-internal-part-1-introduction/
