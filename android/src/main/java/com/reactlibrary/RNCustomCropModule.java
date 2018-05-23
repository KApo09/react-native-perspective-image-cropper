
package com.reactlibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class RNCustomCropModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public RNCustomCropModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "CustomCropManager";
    }

    @ReactMethod
    public void crop(final ReadableMap points, final String uriString, final Callback successCallBack) {
        try {
//            Toast.makeText(reactContext, "should crop now", Toast.LENGTH_LONG).show();
            Thread thread = new Thread(new Runnable(){
                @Override
                public void run(){
                    WritableMap map = Arguments.createMap();

                    map.putString("image", getCroppedImage(points, uriString));
                    successCallBack.invoke(null, map);
                }
            });
            thread.start();

        } catch (Exception e) {
            Toast.makeText(reactContext, "Unable to crop the image" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getCroppedImage(ReadableMap points, String uriString) {
        try {
            Bitmap srcBitmap = MediaStore.Images.Media.getBitmap(this.reactContext.getContentResolver(), Uri.parse(uriString));
            if (srcBitmap == null) {
//                Log.e("RNCustomCrop", "Bitmap is null");
                return null;
            } else {
//                Log.e("RNCustomCrop", "Bitmap has a value");
                //target size
                int bitmapWidth = srcBitmap.getWidth();
                int bitmapHeight = (int)(srcBitmap.getHeight()/1.5);

                Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);

                float[] src = new float[]{
                        (float) points.getMap("topLeft").getDouble("x"), (float) points.getMap("topLeft").getDouble("y"),
                        (float) points.getMap("topRight").getDouble("x"), (float) points.getMap("topRight").getDouble("y"),
                        (float) points.getMap("bottomRight").getDouble("x"), (float) points.getMap("bottomRight").getDouble("y"),
                        (float) points.getMap("bottomLeft").getDouble("x"), (float) points.getMap("bottomLeft").getDouble("y")
                };
                float[] dsc = new float[]{
                        0, 0,
                        bitmapWidth, 0,
                        bitmapWidth, bitmapHeight,
                        0, bitmapHeight
                };

                Matrix matrix = new Matrix();
                matrix.setPolyToPoly(src, 0, dsc, 0, 4);
                canvas.drawBitmap(srcBitmap, matrix, new Paint(Paint.ANTI_ALIAS_FLAG));
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();

                File tempDir= Environment.getExternalStorageDirectory();
                tempDir=new File(tempDir.getAbsolutePath()+"/.tmp/");
                tempDir.mkdir();
                File tempFile = File.createTempFile("croppedImage", ".png", tempDir);

                //write the bytes in file
                FileOutputStream fos = new FileOutputStream(tempFile);
                fos.write(byteArray);
                fos.flush();
                fos.close();
                Log.e("uri", Uri.fromFile(tempFile).toString());

                return Uri.fromFile(tempFile).toString();
            }
        } catch (IOException e) {
            return null;
        }
    }
}