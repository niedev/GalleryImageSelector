/*
 * Copyright 2016 Luca Martino.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copyFile of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gallery.imageselector;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The GalleryImageSelector is a tool for pick an image from gallery, crop it and save it in an internal file that can be accessed from
 * this object (GalleryImageSelector.getSavedImage.
 * The library is highly optimized (fixes a lot of bugs including the rotation bug) and work with a variety of different galleries,
 * even when combined (you can use a gallery for pick and another for crop without problems).
 * <br /><br /><br />
 * When GalleryImageSelector is created, when the user pick an image and when he crop it the results of those operations will be sent
 * via onActivityResult of the activity or the fragment passed to GalleryImageSelector in its constructor.
 * So you will have to override onActivityResult on the activity or on the fragment and inside of it call the method onActivityResult
 * of GalleryImageSelector and pass to it the results received from onActivityResult, for more details keep reading.
 * <br /><br /><br />
 * To use the library follow these passages:
 * <br /><br />
 * - Insert this code inside <application> in the manifest of your app, and fill authorities with a unique name of your choice
 *(if your app has the same authority name of another intalled on a phone your app will not be installed) like "com.hello.world":
 * <pre>{@code
 *         <provider
 *             android:name="androidx.core.content.FileProvider"
 *             android:authorities="custom autority name"
 *             android:exported="false"
 *             android:grantUriPermissions="true">
 *             <meta-data
 *                 android:name="android.support.FILE_PROVIDER_PATHS"
 *                 android:resource="@xml/filepaths" />
 *         </provider>
 * }</pre>
 *
 * - create an xml folder, if you not have one, in the res folder and insert a file named filepath.xml with this code:
 * <pre>{@code
 *        <?xml version="1.0" encoding="utf-8"?>
 *        <paths>
 *            <cache-path path="temporary_images/" name="temporaryUserImage" />
 *        </paths>
 * }</pre>
 * - Create an ImageView that will contain the image.
 * <br /><br />
 * - Create a GalleryImageSelector object as attribute of the Object that will override onActivityResult (the Fragment or the Activity),
 * pick the image view with findViewById and pass it to the constructor of GalleryImageSelector,
 * in the constructor also insert the current activity, and if you are using a Fragment and you want to override onActivityResult in that fragment
 * pass the fragment in addition to the activity (you have to pass the activity anyway) instead if you want to override onActivityResult in the
 * activity or you are not using a Fragment pass the activity and null for the fragment argument.<br />
 * The next argument of the constructor is the resourceId of the default image that GalleryImageSelector should use es. R.drawable.user_icon, it
 * should be the same of the ImageView.<br />
 * The last Argument is the authority name that you chosen in the manifest (they must be equal).
 * <br /><br />
 * - Override onActivityResult in the activity that was passed or in the fragment if it is not null and here call (using the attribute of type GalleryImageSelector created before) galleryImageSelector.onActivityResult
 * passing the arguments of the onActivityResult overwritten and true or false respectively if you want to save the selected image or not (you can save it later with saveContent()).
 * <br /><br /><br />
 * For saving the image you can mark the last parameter of galleryImageSelector.onActivityResult with true, in this case the image will be
 * saved automatically at the end of the crop, if you want instead to save the image in another moment (maybe after an error check for other data)
 * you can mark the save parameter of onActivityResult false and use galleryImageSelector.saveImage() and UserImageContainer will save the last cropped image.
 * The saved image can be accessed with "new File(context.getFilesDir(), "user_image");" or with the static method GalleryImageSelector.getSavedImage(context).
 */
public class GalleryImageSelector {
    private final String DEFAULT_IMAGE = "default";
    private final String CUSTOM_IMAGE = "custom";
    private static final String TEMP_PHOTO_DIRECTORY = "temporary_images";
    private static final String TEMP_PICKED_PHOTO_FILE = "temporary_picked_holder.jpg";
    private static final String TEMP_CROPPED_PHOTO_FILE = "temporary_cropped_holder.jpg";
    private static int PICK_IMAGE = 1;
    private static int CROP_IMAGE = 2;
    private ImageView imageView;
    private Bitmap image;
    private Activity activity;
    private Fragment fragment;
    private String authority;

    /**
     * In this constructor you have to pass the ImageView that will contain the image, the current activity, and if you are using a Fragment and you want to override onActivityResult in that fragment
     * pass the fragment in addition to the activity (you have to pass the activity anyway) instead if you want to override onActivityResult in the
     * activity or you are not using a Fragment pass the activity and null for the fragment argument.<br />
     * The next argument of the constructor is the resourceId of the default image that GalleryImageSelector should use es. R.drawable.user_icon, it
     * should be the same of the ImageView.<br />
     * The last Argument is the authority name that you chosen in the manifest (they must be equal).
     *
     * @param image             the ImageView that will contain the image selected and cropped.
     * @param activity          the current activity, if fragment is != null this onActivityResult will be called on the Fragment, not in the Activity.
     * @param fragment          the fragment that override onActivityResult, it can be null, in that case the Activity will have to override onActivityResult.
     * @param defaultImageResId resourceId of the default image that GalleryImageSelector should use es. R.drawable.user_icon, it
     *                          should be the same of the ImageView.
     * @param authority         the authority name that you chosen in the manifest (see the tutorial in the documentation of the class)
     */
    public GalleryImageSelector(ImageView image, @NonNull final Activity activity, @Nullable final Fragment fragment, int defaultImageResId, String authority) {
        this.imageView = image;
        this.activity = activity;
        this.fragment = fragment;
        this.authority = authority;

        //user image initialization
        Bitmap imageBitmap = getBitmapFromFile(new File(activity.getFilesDir(), "user_image"));
        if (imageBitmap != null) {
            // get the user image and set it as the image
            RoundedBitmapDrawable circlularImage = RoundedBitmapDrawableFactory.create(activity.getResources(), imageBitmap);
            circlularImage.setCircular(true);
            imageView.setImageDrawable(circlularImage);
            imageView.setTag(CUSTOM_IMAGE);
            this.image = circlularImage.getBitmap();
        } else {
            imageView.setImageResource(defaultImageResId); //insert the default image in drawable and set it here in imageView
            imageView.setTag(DEFAULT_IMAGE);
        }
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] items;
                if (imageView.getTag() == CUSTOM_IMAGE) {
                    items = new String[2];
                    items[0] = "Select from gallery";
                    items[1] = "Remove";
                } else {
                    items = new String[1];
                    items[0] = "Select from gallery";
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setCancelable(true);
                builder.setTitle("user image");
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            Intent pickIntent = new Intent("android.intent.action.PICK");
                            pickIntent.setType("image/*");

                            if (fragment != null) {
                                fragment.startActivityForResult(pickIntent, PICK_IMAGE);
                            } else {
                                activity.startActivityForResult(pickIntent, PICK_IMAGE);
                            }
                        } else {
                            // insert the default image in the imageView
                            imageView.setImageResource(defaultImageResId);
                            imageView.setTag(DEFAULT_IMAGE);
                            GalleryImageSelector.this.image = null;
                            // delete the previous saved image
                            File file = new File(activity.getFilesDir(), "user_image");
                            file.delete();
                        }
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    /**
     * In the overwritten onActivityResult (in the Fragment or in the Activity) you have to call this method an pass to it all the arguments
     * received from the overwritten method, plus you have to set if the image will be saved in this method or not (if you want to save it later
     * with saveImage() or you not want to save the image at all, in this case when the ImageView will be recreated it will show the default image).
     * <br /><br />
     * This method will be called when the image is selected and when the image si cropped, in the last case this method will insert the cropped
     * image in the ImageView passed in the constructor, and if saveImage is true it will also save the image.
     *
     * @param requestCode
     * @param resultCode
     * @param data
     * @param saveImage   if you want to save the image in this method or not
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data, boolean saveImage) {
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            //copyFile the result into cache
            copyImageUriIntoFile(data.getData(), getTempPickedFile());
            //start crop
            Intent intent = new Intent("com.android.camera.action.CROP");
            intent.setDataAndTypeAndNormalize(getTempPickedUri(), "image/*");
            intent.putExtra("crop", "true");
            intent.putExtra("aspectX", 1);
            intent.putExtra("aspectY", 1);
            intent.putExtra("outputX", imageView.getWidth());
            intent.putExtra("outputY", imageView.getHeight());
            intent.putExtra("scaleUpIfNeeded", true);
            intent.putExtra("noFaceDetection", true);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra("output", getTempCroppedUri());
            intent.setClipData(ClipData.newRawUri("output", getTempCroppedUri()));
            if (fragment != null) {
                fragment.startActivityForResult(intent, CROP_IMAGE);
            } else {
                activity.startActivityForResult(intent, CROP_IMAGE);
            }

        } else if (requestCode == CROP_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Bitmap image = null;
            File tempFile = getTempCroppedFile();

            if (tempFile != null && tempFile.exists()) {
                String path = tempFile.getAbsolutePath();

                image = BitmapFactory.decodeFile(path);
                tempFile.delete();
            }

            if (image == null && data.getData() != null) {  //nel caso non sia stata salvata nel file
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = activity.getContentResolver().query(data.getData(), filePathColumn, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String picturePath = cursor.getString(columnIndex);
                    cursor.close();
                    // withdrawal of the selected image
                    image = BitmapFactory.decodeFile(picturePath);
                    // to prevent rotation bug
                    try {
                        image = modifyOrientation(image, picturePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (image != null) {
                // insertion of the selected image in the imageView
                RoundedBitmapDrawable circlularImage = RoundedBitmapDrawableFactory.create(activity.getResources(), image);
                circlularImage.setCircular(true);
                imageView.setImageDrawable(circlularImage);
                imageView.setTag(CUSTOM_IMAGE);
                this.image = image;
                if (saveImage) {
                    // saving the selected image
                    saveBitmapToFile(new File(activity.getFilesDir(), "user_image"), image);
                }
            } else {
                Toast.makeText(activity, "Error during image selection, use another gallery to select it", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * This method will save the last image selected and cropped
     */
    public void saveImage() {
        if (image != null) {
            saveBitmapToFile(new File(activity.getFilesDir(), "user_image"), image);
        }
    }

    /**
     * This static method will return the last image saved
     *
     * @param context context used to getFilesDir()
     * @return the last image saved
     */
    public static Bitmap getSavedImage(Context context) {
        return getBitmapFromFile(new File(context.getFilesDir(), "user_image"));
    }

    private static synchronized Bitmap getBitmapFromFile(File file) {
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.getPath());
        } else {
            return null;
        }
    }

    private static synchronized void saveBitmapToFile(File file, Bitmap image) {
        OutputStream outStream = null;
        try {
            outStream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copyImageUriIntoFile(Uri sourceUri, File destinationFile) {
        InputStream inputStream = null;
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            inputStream = activity.getContentResolver().openInputStream(sourceUri);
            if (inputStream != null) {
                bis = new BufferedInputStream(inputStream);
                bos = new BufferedOutputStream(new FileOutputStream(destinationFile, false));
                byte[] buf = new byte[1024];
                while (bis.read(buf) != -1) {
                    bos.write(buf);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static Bitmap modifyOrientation(Bitmap bitmap, String image_absolute_path) throws IOException {
        ExifInterface ei = new ExifInterface(image_absolute_path);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotate(bitmap, 90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotate(bitmap, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotate(bitmap, 270);

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return flip(bitmap, true, false);

            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return flip(bitmap, false, true);

            default:
                return bitmap;
        }
    }

    private static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private Uri getTempPickedUri() {
        return FileProvider.getUriForFile(activity, authority, getTempPickedFile());
    }

    private Uri getTempCroppedUri() {
        return FileProvider.getUriForFile(activity, authority, getTempCroppedFile());
    }

    private File getTempPickedFile() {
        activity.getCacheDir().mkdirs();
        File directory = new File(activity.getCacheDir(), TEMP_PHOTO_DIRECTORY);
        directory.mkdirs();
        File file = new File(directory, TEMP_PICKED_PHOTO_FILE);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
            }
        }
        return file.getAbsoluteFile();
    }

    private File getTempCroppedFile() {
        activity.getCacheDir().mkdirs();
        File directory = new File(activity.getCacheDir(), TEMP_PHOTO_DIRECTORY);
        directory.mkdirs();
        File file = new File(directory, TEMP_CROPPED_PHOTO_FILE);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
            }
        }
        return file.getAbsoluteFile();
    }
}
