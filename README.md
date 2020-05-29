# Android Hidden APIs

**Android Hidden APIs** are classes, methods and resources that Google hides from you because of stability reason. These features are hidden because they may be changed on next API version.

The internal APIs are located in package `com.android.internal` and available in the `framework.jar`, while the hidden APIs are located in the `android.jar` file with `@hide` javadoc attribute. Now you know the difference. But I will refer to both as hidden APIs.

This repo contains custom `android.jar` which you can use to develop your app. However, if you urgently need to create your own `android.jar`, I also share you the Krabby Patty secret recipe here: [Create Your Own Android Hidden APIs](https://medium.com/@hardiannicko/create-your-own-android-hidden-apis-fa3cca02d345).

## Resources Helper [ ![Download](https://api.bintray.com/packages/anggrayudi/maven/android-hidden-api/images/download.svg)](https://bintray.com/anggrayudi/maven/android-hidden-api/_latestVersion)

If you plan to use only Android internal resources rather than internal classes or methods,
do:

````gradle
dependencies {
    implementation 'com.anggrayudi:android-hidden-api:28.1'
}
````

**Note:** If you encounter error `Failed to resolve com.anggrayudi:android-hidden-api:x.x`, then add the following config:

````gradle
repositories {
    maven { url 'https://dl.bintray.com/anggrayudi/maven/' }
}
````

Here's some example of accessing internal resources:
​    
```java
import com.anggrayudi.hiddenapi.Res;

    String accept = InternalAccessor.getString(Res.string.accept);
    float sbar_height = InternalAccessor.getDimension(Res.dimen.status_bar_height);
    int notif_color = InternalAccessor.getColor("config_defaultNotificationColor");
```

If you also want to include the internal classes or methods, do the following:

1. Go to `<SDK location>/platforms/`.
2. Copy, paste and replace the downloaded hidden API file into this directory, e.g. `android-29/android.jar`.
3. Change `compileSdkVersion` and `targetSdkVersion` to 29 (for example).
4. Finally, rebuild your project.

Note: Higher `compileSdkVersion` and `targetSdkVersion` will be better.

## License

    Copyright 2015-2020 Anggrayudi Hardiannicko A.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


[1]: https://devmaze.wordpress.com/2011/01/18/using-com-android-internal-part-1-introduction
[2]: https://github.com/anggrayudi/android-hidden-api/issues/9
