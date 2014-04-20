Prerequisities:
JDK 1.6
Eclipse
Android SDK

add action bar http://developer.android.com/guide/topics/ui/actionbar.html
which leads to http://developer.android.com/tools/support-library/setup.html

Create emulator device:
Window - Android Virtual Device Manager, create new AVD.
properties: 
Google API 4.4.2, api level 19
CPU ARM
Storage SD Card 100MiB
Use Host GPU - may be quicker

Then Project Run as Android application.
It takes few minutes to start. 

If it's running, you need some song files.
You have to push it onto the device in Eclipse:
DDMS perspective - if it is running - on the left - Devices tab, select appropriate emulator and then on right - File explorer tab - push file onto a device Icon - in path /storage/sdcard/robotar/songs
(you have to create robotar/songs/ folders)

Song files may be prepared in RoboTarPC application or manually. It's only XML file. 
