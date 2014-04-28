Prerequisities:
JDK 1.6
Eclipse
Android SDK

add action bar http://developer.android.com/guide/topics/ui/actionbar.html
which leads to http://developer.android.com/tools/support-library/setup.html

Create emulator device:
Window - Android Virtual Device Manager, create new AVD.
properties: 
Google API 4.4.2, api level 19 (also tested 11 and 8)
CPU ARM
Storage SD Card 100MiB
Use Host GPU - may be quicker

If you need to run it below Android 3.0 (api level 11), you need to download this project:
https://github.com/kolavar/android-support-v4-preferencefragment
(it will complain about android-4, set it to android-8 (probably, this is the lowest api level for robotar (ioio)))
then remove libs/android-support-v4.jar and put there the same jar file from appcompat_v7 project (also in libs). otherwise you'll get error, that these two jars don't match.
(i'm not sure, where i got it, but for the record, it's on my computer in ws/robotar/appcompat_v7, i can package it and send :) )

Then Project Run as Android application.
It takes few minutes to start. 

If it's running, you need some song files.
You have to push it onto the device in Eclipse:
DDMS perspective - if it is running - on the left - Devices tab, select appropriate emulator and then on right - File explorer tab - push file onto a device Icon - in path /storage/sdcard/robotar/songs
(you have to create robotar/songs/ folders)

Song files may be prepared in RoboTarPC application or manually. It's only XML file. 
