
package com.reactlibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
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

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RNCustomCropModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private static final String TAG = "RNCustomCropModule";

    public RNCustomCropModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "CustomCropManager";
    }

    @ReactMethod
    public void crop(final ReadableMap points, final String base64Image, final Callback successCallBack) {
        try {
            Toast.makeText(reactContext, "should crop now", Toast.LENGTH_LONG).show();
            Thread thread = new Thread(new Runnable(){
                @Override
                public void run(){
                    WritableMap map = Arguments.createMap();

                    map.putString("image", getCroppedImage(points, base64Image));
                    successCallBack.invoke(null, map);
                }
            });
            thread.start();

        } catch (Exception e) {
            Toast.makeText(reactContext, "Unable to crop the image" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @ReactMethod
    public void findImageCorners(final ReadableMap points, final String base64Image, final Callback successCallBack) {
        try {
            Toast.makeText(reactContext, "should crop now", Toast.LENGTH_LONG).show();
            if (!OpenCVLoader.initDebug()) {
                Log.e(TAG, "  OpenCVLoader.initDebug(), not working.");
            } else {
                Thread thread = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        WritableMap map = Arguments.createMap();
                        map.putString("image", getCornerCoordinates(points, base64Image));
                        successCallBack.invoke(null, map);
                    }
                });
                thread.start();
                Log.d(TAG, "  OpenCVLoader.initDebug(), working.");
            }

        } catch (Exception e) {
            e.printStackTrace();
//            Log.e(TAG, "Unable to crop the image" + e.getMessage());
            Toast.makeText(reactContext, "Unable to crop the image" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private String getCornerCoordinates(ReadableMap points, String base64Image){

        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
        float[] coords = new float[]{
                (float) points.getMap("topLeft").getDouble("x") - 20, (float) points.getMap("topLeft").getDouble("y") - 20,
                (float) points.getMap("topRight").getDouble("x") + 20, (float) points.getMap("topRight").getDouble("y") - 20,
                (float) points.getMap("bottomRight").getDouble("x") + 20, (float) points.getMap("bottomRight").getDouble("y") + 20,
                (float) points.getMap("bottomLeft").getDouble("x") - 20, (float) points.getMap("bottomLeft").getDouble("y") + 20
        };

        Bitmap srcBitmap = Bitmap.createBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length), (int)coords[0], (int)coords[1], (int)(coords[2] - coords[0]), (int)(coords[5] - coords[1]));
        Mat edgedMat = getEdgedImage(srcBitmap);

        ArrayList<MatOfPoint> contours = findContours(edgedMat);










        Bitmap resultBitmap = Bitmap.createBitmap(edgedMat.cols(), edgedMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edgedMat, resultBitmap);

        edgedMat.release();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Mat getEdgedImage(Bitmap srcBitmap){
        Mat rgba = new Mat();
        Utils.bitmapToMat(srcBitmap, rgba);

        Mat edges = new Mat(rgba.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_RGB2GRAY, 4);
        Mat dilatedImage = new Mat();
        Imgproc.dilate(edges, dilatedImage, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7)));
        Mat bgImage = new Mat();
        Imgproc.medianBlur(dilatedImage, bgImage, 21);
        Mat diffImage = new Mat();
        Core.absdiff(edges, bgImage, diffImage);
        Core.subtract(new Mat(diffImage.rows(), diffImage.cols(), CvType.CV_8UC1, new Scalar(255, 255, 255, 255)), diffImage, diffImage);
        Core.normalize(diffImage, edges, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);








        /*long now = System.currentTimeMillis();
        Mat sobelX = new Mat();
        Mat sobelY = new Mat();
        Imgproc.Sobel(edges, sobelX, CvType.CV_16S, 1, 0);
        Imgproc.Sobel(edges, sobelY, CvType.CV_16S, 0, 1);
        Mat absX = new Mat();
        Mat absY = new Mat();
        Core.convertScaleAbs(sobelX, absX);
        Core.convertScaleAbs(sobelY, absY);
        Core.addWeighted(absX, 0.5, absY, 0.5, 0, edges);
        Log.v(TAG, "getEdge time:" + (System.currentTimeMillis() - now));

        Imgproc.threshold(edges, edges, 127, 255, Imgproc.THRESH_BINARY);*/













        Imgproc.GaussianBlur(edges, edges, new Size(5,5), 0);
//        Imgproc.medianBlur(edges, edges, 3);

        MatOfDouble mu = new MatOfDouble();
        MatOfDouble sigma = new MatOfDouble();
        Core.meanStdDev(edges, mu, sigma);
        double median = mu.get(0,0)[0];

        int lower = (int)Math.max(0, (1.0 - 0.33) * median);
        int upper = (int)Math.min(255, (1.0 + 0.33) * median);


        Imgproc.adaptiveThreshold(edges, edges,upper,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY,31,9);


//        Imgproc.Canny(edges, edges, lower, upper);


//        double threshold = Imgproc.threshold(edges, edges, lower, upper, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

//        Log.d(TAG,"upper limit is :"+ upper + " Lower limit is :"+ lower + " threshold :"+ threshold);
//        Imgproc.Canny(edges, edges, threshold * 2 / 5, threshold);

//        Imgproc.dilate(edges, edges, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)));
//        Imgproc.erode(edges, edges, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));



//        ArrayList<MatOfPoint> contours = new ArrayList<>();
//        Mat hierarchy = new Mat();
//        Imgproc.findContours(edges, contours, hierarchy,1,1);
//        Imgproc.drawContours(rgba, contours, -1, new Scalar(0,255,0), 5);






//        rgba.release();
        dilatedImage.release();
        bgImage.release();
        diffImage.release();

        return edges;
    }

    private ArrayList<MatOfPoint> findContours(Mat src) {


        double ratio = src.size().height / 500;
        int height = Double.valueOf(src.size().height / ratio).intValue();
        int width = Double.valueOf(src.size().width / ratio).intValue();
        Size size = new Size(width,height);


        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(src, contours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

       /* if (hierarchy.size().height > 0 && hierarchy.size().width > 0) {
            for (int idx = 0; idx >= 0; idx = (int) hierarchy.get(0, idx)[0]) {
                Rect rect = Imgproc.boundingRect(contours.get(idx));
                Imgproc.rectangle(src, new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(255, 0, 0), 20);
            }
        }*/

        hierarchy.release();

        Collections.sort(contours, new Comparator<MatOfPoint>() {

            @Override
            public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                return Double.valueOf(Imgproc.contourArea(rhs)).compareTo(Imgproc.contourArea(lhs));
            }
        });

        for ( MatOfPoint c: contours ) {
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double peri = Imgproc.arcLength(c2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);

            Point[] points1 = approx.toArray();

            // select biggest 4 angles polygon
            if (points1.length == 4) {
                Point[] foundPoints = sortPoints(points1);
                Imgproc.circle(src, foundPoints[0], 50, new Scalar(230,0,0));
                Imgproc.circle(src, foundPoints[1], 50, new Scalar(230,255,0));
                Imgproc.circle(src, foundPoints[2], 50, new Scalar(230,0,255));
                Imgproc.circle(src, foundPoints[3], 50, new Scalar(0,255,0));
                Log.d(TAG, "p1: "+ foundPoints[0].toString() +" p2: "+ foundPoints[1].toString() + "p3: "+ foundPoints[2].toString() +" p4: "+ foundPoints[3].toString());
                if (insideArea(foundPoints, size)) {
                    Log.d(TAG, "some point found");

                    Log.d(TAG, "p1: "+ foundPoints[0].toString() +" p2: "+ foundPoints[1].toString() + "p3: "+ foundPoints[2].toString() +" p4: "+ foundPoints[3].toString());
                }
            }
        }


/*        List<MatOfPoint> matOfPoints = new ArrayList<>();
        Mat drawing = new Mat(src.size(), CvType.CV_8UC3);
        for ( MatOfPoint c: contours ) {

            MatOfInt hull = new MatOfInt();
            Imgproc.convexHull(c, hull);

            MatOfPoint mopOut = new MatOfPoint();
            mopOut.create((int)hull.size().height,1,CvType.CV_32SC2);

            for(int i = 0; i < hull.size().height ; i++)
            {
                int index = (int)hull.get(i, 0)[0];
                double[] point = new double[] {
                        c.get(index, 0)[0], c.get(index, 0)[1]
                };
                mopOut.put(i, 0, point);
            }
            matOfPoints.add(mopOut);
        }

        Imgproc.polylines( src, matOfPoints, true, new Scalar(0,0,255), 2 );

//        polylines( src, ConvexHullPoints, true, Scalar(0,0,255), 2 );*/



        return contours;
    }

    private Point[] sortPoints( Point[] src ) {

        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));

        Point[] result = { null , null , null , null };

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);
            }
        };

        Comparator<Point> diffComparator = new Comparator<Point>() {

            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);
            }
        };

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator);

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator);

        // top-right corner = minimal diference
        result[1] = Collections.min(srcPoints, diffComparator);

        // bottom-left corner = maximal diference
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }

    private boolean insideArea(Point[] rp, Size size) {

        int width = Double.valueOf(size.width).intValue();
        int height = Double.valueOf(size.height).intValue();
        int baseMeasure = height/4;

        int bottomPos = height-baseMeasure;
        int topPos = baseMeasure;
        int leftPos = width/2-baseMeasure;
        int rightPos = width/2+baseMeasure;

        return (
                rp[0].x <= leftPos && rp[0].y <= topPos
                        && rp[1].x >= rightPos && rp[1].y <= topPos
                        && rp[2].x >= rightPos && rp[2].y >= bottomPos
                        && rp[3].x <= leftPos && rp[3].y >= bottomPos

        );
    }

    private String getCroppedImage(ReadableMap points, String base64Image) {

        byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
        Bitmap srcBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);


        //target size
        int bitmapWidth = srcBitmap.getWidth();
        int bitmapHeight = (int)(srcBitmap.getWidth()/1.586);

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
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }
}