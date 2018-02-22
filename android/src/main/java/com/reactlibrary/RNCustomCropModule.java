
package com.reactlibrary;

import android.widget.Toast;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;

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
  public void crop(ReadableMap points, String base64Image, Callback successCallBack){
      try {
          Toast.makeText(reactContext, "should crop now", Toast.LENGTH_LONG).show();
          successCallBack.invoke(null, base64Image);
      } catch (Exception e) {
          Toast.makeText(reactContext, "Unable to crop the image", Toast.LENGTH_LONG).show();
      }
  }
}