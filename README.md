Vandy Vans
==========

Vandy Vans is an open-source Android project created by [Vanderbilt University](http://vanderbilt.edu) students through a student organization called [VandyApps](https://www.facebook.com/VandyMobile).

The purpose of the app is to provide a fast, native way for Vanderbilt students to access the van schedule as well as a live map of the vans' locations on their Android phones.

## Credits

Vandy Vans for Android was created by [Batman](http://uncyclopedia.wikia.com/wiki/Batman_\(person\)), inspired by the original [Vandy Vans for iOS](github.com/VandyApps/vandyvans-ios).

The graphic design for the project was done by [Fletcher Young](http://vandycommodore.deviantart.com/).

The API used in this project was developed by [Syncromatics](http://www.syncromatics.com/), who also developed the original [Vandy Vans web application](http://vandyvans.com/).

## Contact

You can submit any bug reports or feedback either directly through the app or by emailing [vandyvansapp@gmail.com](mailto:vandyvansapp@gmail.com).

## Importing Project Using IntelliJ IDEA

When importing the project into IntelliJ, navigate to PROJECT_HOME/GuideAndroid/, click on
pom.xml in the folder and click `import`. IDEA will setup all our dependencies automatically as specified in the 
pom.xml so that you don't have to worry about doing library project import, which is really really really HARD.


## Build Notes

* Maven 3.0.5
  - Please download the right version from http://maven.apache.org/download.cgi
  - version 3.1.1 does not work right now. The default version provided in the Ubuntu
    repo also does not work the last time I tried.
  - Follow the direction at the bottom of the page to change the default Maven
    version used. Check that it is working with `mvn -v`
  - This [page](https://code.google.com/p/maven-android-plugin/issues/detail?id=395) has
    some guide for OSX users.

* Android SDK
  - make sure `ANDROID_HOME` is set to the path to your SDK folder
  - for example, in `~/.bashrc` add

```bash
export ANDROID_HOME=/home/{path_to_the_folder}/sdk
export ANDROID_SDK_HOME=/home/{path_to_the_folder}/sdk
export ANDROID_SDK_ROOT=/home/{path_to_the_folder}/sdk
export ANDROID_SDK=/home/{path_to_the_folder}/sdk
```

  - ^^^Don't ask why. Android is always crazy.
  - install SDK version 19, 15, 7

* Install Google Play Services into your Maven repo
  - https://github.com/JakeWharton/gms-mvn-install
  - This step requires Maven 3.1.1, so switched your version temporarily.
  - run the script with 7 appended to indicate that version should be installed.

          ./gms-mvn-install.sh 7

* Create a new file `res/values/mapsapikey.xml` (which is automatically excluded in `.gitignore`) and put in this text:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="maps_api_key_v2">xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx</string>
</resources>
```

  - Get your own API key by following the instructions on [this page](https://developers.google.com/maps/documentation/android/start#getting_the_google_maps_android_api_v2)
    and place the key in here.
  - Please double check before your first commit to make sure that your API key is not
    included in the version control history.

## Build

You can build using Maven:

    $ mvn clean install

## Run

Deploy to an Android virtual device (AVD):

    $ mvn android:deploy

