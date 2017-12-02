sail trainer
============

These are Android apps for the preparation to different driving licences for
German pleasure crafts on water ways. Essentially these are six copies of the
same source code, that is able to train the correct responses to multiple
choice questions. The user gets asked until he is able to correctly answer the
question five consecutive times.

The SKS trainer is a bit different as the exam does require the examinee to
answer questions in free text.

This is the source code of the Android application. You can install the
applications in compiled form directly from the Google Play Store:

https://play.google.com/store/apps/details?id=eu.wimmerinformatik.sbfb
https://play.google.com/store/apps/details?id=eu.wimmerinformatik.sbfs
https://play.google.com/store/apps/details?id=eu.wimmerinformatik.sks
https://play.google.com/store/apps/details?id=eu.wimmerinformatik.src
https://play.google.com/store/apps/details?id=eu.wimmerinformatik.trainer
https://play.google.com/store/apps/details?id=eu.wimmerinformatik.lrc

I have published the code on GitHub because some people asked me if they can
play with it. (Yes, you can.)


Note on the SBFS trainer
------------------------

The trainer for SBFS has been moved to its on repository:

https://github.com/mawis/sbfs-trainer


Build how-to
------------

These apps are build using Maven. To build them you need the following tools
installed:

- Maven
- Java JDK
- Android SDK

Before being able to compile you also will have to generate a key that is used
for signing the Android apps. A self-signed key is absolutely suficient for
signing on Android. The key is mainly used by the device to check if you are
allowed to distribute updates for an application.

How to generate your key is described on
http://developer.android.com/tools/publishing/app-signing.html#signing-manually

(You only have to follow step one of this description. Signing of the
application will be done automatically by Maven.)

After you created your own key, you have to tell Maven where to find it and
what you used as the password for it. This is done by adding entries to the
Maven configuration typically found in ~/.m2/settings.xml (on Unix systems).
This file should contain something like this:

<?xml version='1.0'?>
<settings
    xmlns="http://maven.apache.org/SETTINGS/1.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                        http://maven.apache.org/xsd/settings-1.0.0.xsd">

    <profiles>
	<profile>
	    <activation>
		<activeByDefault>true</activeByDefault>
	    </activation>
	    <properties>
		<androidsign.keystore>${user.home}/your.keystore</androidsign.keystore>
		<androidsign.alias>android-release-key</androidsign.alias>               
		<androidsign.keypass>password of your key</androidsign.keypass>                
		<androidsign.storepass>password of your keystore</androidsign.storepass>
	    </properties>
	</profile>
    </profiles>
</settings>

After all these preparations are done you can change to the folder of the
application you want to compile (e.g. "cd SBFB-Trainer"). You start the
compilation with the following command:

mvn clean install -Dandroid.release=true

Afterwards you have the compiled app in the target sub-folder as
sbfbtrainer-1.5.3-signed-aligned.apk.

This APK is called aligned, but it isn't. Therefore it is a good idea to align
it before installing it on your device. This can be done using the zipalign
command:

zipalign -v 4 sbfbtrainer-1.5.3-signed-aligned.apk sbfbtrainer.apk

This results in the file sbfbtrainer.apk which now can be distributed.


Matthias Wimmer, 2014-12-31
