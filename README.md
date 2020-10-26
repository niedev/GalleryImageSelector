### GalleryImageSelector
The GalleryImageSelector is a library, created originally for <a href="https://github.com/niedev/RTranslator" target="_blank" rel="noopener noreferrer">RTranslator</a>, that allows you to pick an image from gallery, crop it and save it in an internal file that can be accessed from the GalleryImageSelector class (via the static method GalleryImageSelector.getSavedImage).<br /><br />
The library is highly optimized (fixes a lot of bugs including the rotation bug) and can work with a variety of different galleries,
even when combined (you can use a gallery for pick and another for crop without problems). If you have problem with a specific gallery app please report it in Issues (I cannot guarantee that I will be able to fix the problem but I can try if I have time).<br />

For see an example app check <a href="https://github.com/niedev/GalleryImageSelectorExample" target="_blank" rel="noopener noreferrer">BluetoothCommunicatorExample</a> or <a href="https://github.com/niedev/RTranslator" target="_blank" rel="noopener noreferrer">RTranslator</a><br /><br />

#### Tutorial
For use the library in a project you have to add jitpack.io to your root build.gradle (project):
```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
Then add the last version of BluetoothCommunicator to your app build.gradle
```
dependencies {
        implementation 'com.github.niedev:GalleryImageSelector:1.0.7'
}
```

<br />When GalleryImageSelector is created, when the user pick an image and when he crop it the results of those operations will be sent
via onActivityResult of the activity or the fragment passed to GalleryImageSelector in its constructor.
So you will have to override onActivityResult on the activity or on the fragment and inside of it call the method onActivityResult
of GalleryImageSelector and pass to it the results received from onActivityResult, for more details keep reading.
<br /><br />
To use the library follow these passages:
- Insert this code inside <application> in the manifest of your app, and fill authorities with a unique name of your choice
(if your app has the same authority name of another intalled on a phone your app will not be installed) like "com.hello.world":
```
  <provider
      android:name="androidx.core.content.FileProvider"
      android:authorities="custom autority name"
      android:exported="false"
      android:grantUriPermissions="true">
      <meta-data
          android:name="android.support.FILE_PROVIDER_PATHS"
          android:resource="@xml/filepaths" />
  </provider>
```

- create an xml folder (if you not have one) in the res folder and insert a file named filepath.xml with this code:
```
  <?xml version="1.0" encoding="utf-8"?>
  <paths>
     <cache-path path="temporary_images/" name="temporaryUserImage" />
  </paths>
```
- Create an ImageView that will contain the image.

- Create a GalleryImageSelector object as attribute of the Object that will override onActivityResult (the Fragment or the Activity),
pick the image view with findViewById and pass it to the constructor of GalleryImageSelector,
in the constructor also insert the current activity, and if you are using a Fragment and you want to override onActivityResult in that fragment
pass the fragment in addition to the activity (you have to pass the activity anyway) instead if you want to override onActivityResult in the
activity or you are not using a Fragment pass the activity and null for the fragment argument.<br />
The next argument of the constructor is the resourceId of the default image that GalleryImageSelector should use es. R.drawable.user_icon, it
should be the same of the ImageView.<br />
The last Argument is the authority name that you chosen in the manifest (they must be equal).

- Override onActivityResult in the activity that was passed or in the fragment if it is not null and here call (using the attribute of type GalleryImageSelector created before) galleryImageSelector.onActivityResult
passing the arguments of the onActivityResult overwritten and true or false respectively if you want to save the selected image or not (you can save it later with saveContent()).
<br /><br />

For saving the image you can mark the last parameter of galleryImageSelector.onActivityResult with true, in this case the image will be
saved automatically at the end of the crop, if you want instead to save the image in another moment (maybe after an error check for other data)
you can mark the save parameter of onActivityResult false and use galleryImageSelector.saveImage() and UserImageContainer will save the last cropped image.
The saved image can be accessed with "new File(context.getFilesDir(), "user_image");" or with the static method GalleryImageSelector.getSavedImage(context).
