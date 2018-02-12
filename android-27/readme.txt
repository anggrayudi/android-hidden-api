framework.jar file was combined with android.jar from /<SDK location>/platforms/android-27, where 27 is current API level.

I'm not sure that it will work as intended. If it does not work, please report to us.

Please note: android.jar includes Framework hidden-apis, Telephony internal apis, and Common apis. A lot of system apis needs system permissions. 

We also provide the Services jar, which contains the com.android.server. * Classes, which you can import via provided. However, these APIs can only be used in SystemServer, other applications throw NoClassDefFoundError and other exceptions.

========== BACKUP ==========

Original android.jar can be downloaded here
https://dl.google.com/android/repository/platform-27_r12.zip

Extract the archive and find android.jar
