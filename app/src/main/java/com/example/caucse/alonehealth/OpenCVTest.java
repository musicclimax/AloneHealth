package com.example.caucse.alonehealth;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;

public class OpenCVTest extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    Button button, startbutton, endbutton;
    TextView testtext, resulttext;
    boolean flag = true;
    private static final String TAG = "opencv";
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat matInput;
    private Mat matResult;
    public static final ArrayList<Mat> testMat = new ArrayList<>();
    ArrayList<Mat> nowMat;
    MatOfKeyPoint keypoints1;
    Mat descriptors1;
    Mat outputImage;
    int ret = 0;
    int tempret = 0;
    int count = 0;
    boolean isStart = false;
    TextToSpeech tts;
    ORB orb;
    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.opencv_layout);
        testtext = (TextView) findViewById(R.id.testtext);
        resulttext = (TextView) findViewById(R.id.resulttext);

        /*new Thread(){
            @Override
            public void run() {
                super.run();

                if(testMat.size() == 2) {
                    //resulttext.setText("hiyo");
                    while (count < 10) {
                        try {
                            DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                            MatOfDMatch matches = new MatOfDMatch();
                            if (testMat.get(0).cols() == descriptors1.cols()) {
                                matcher.match(testMat.get(0), descriptors1, matches);

                                DMatch[] match = matches.toArray();
                                double max_dist = 0;
                                double min_dist = 100;

                                for (int i = 0; i < descriptors1.rows(); i++) {
                                    double dist = match[i].distance;
                                    if (dist < min_dist) min_dist = dist;
                                    if (dist > max_dist) max_dist = dist;
                                }

                                for (int i = 0; i < descriptors1.rows(); i++) {
                                    if (match[i].distance <= 50) {
                                        ret++;

                                        //resulttext.setText("rows : " + testMat.get(0).rows() + "cols : " + testMat.get(0).cols() + "sum : " + (testMat.get(0).rows() * testMat.get(0).cols()) + "result : " + ret);
                                    }
                                }
                                if (ret > 200) count++;
                                testtext.setText("ret: " + ret + "count : " + count);
                                ret = 0;
                            }
                            Thread.sleep(100);
                        }catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.start();*/

       /* tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR){
                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });*/



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //퍼미션 상태 확인
            if (!hasPermissions(PERMISSIONS)) {
                //퍼미션 허가 안되어있다면 사용자에게 요청
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }


        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        button = (Button) findViewById(R.id.button);
        startbutton = (Button) findViewById(R.id.startbutton);
        endbutton = (Button) findViewById(R.id.endbutton);

        matInput = inputFrame.rgba();
        matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());
        if (matResult == null) matResult.release();
        ConvertRGBtoGray(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());

        keypoints1 = new MatOfKeyPoint();
        descriptors1 = new Mat();
        outputImage = new Mat();

        orb = ORB.create();
        orb.detect(matResult, keypoints1);
        orb.compute(matResult, keypoints1, descriptors1);
        Features2d.drawKeypoints(matResult, keypoints1, outputImage, new Scalar(0, 255, 0), 0);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(testMat.size() == 2){

                    if(flag == true) {
                        while (count < 2) {
                            try {

                                DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                                MatOfDMatch matches = new MatOfDMatch();
                                if (testMat.get(0).cols() == descriptors1.cols()) {
                                    matcher.match(testMat.get(0), descriptors1, matches);

                                    DMatch[] match = matches.toArray();
                                    double max_dist = 0;
                                    double min_dist = 100;

                                    for (int i = 0; i < descriptors1.rows(); i++) {
                                        double dist = match[i].distance;
                                        if (dist < min_dist) min_dist = dist;
                                        if (dist > max_dist) max_dist = dist;
                                    }

                                    for (int i = 0; i < descriptors1.rows(); i++) {
                                        if (match[i].distance <= 50) {
                                            ret++;

                                            //resulttext.setText("rows : " + testMat.get(0).rows() + "cols : " + testMat.get(0).cols() + "sum : " + (testMat.get(0).rows() * testMat.get(0).cols()) + "result : " + ret);
                                        }
                                    }
                                    if (ret > 100) count++;
                                    else flag = false;
                                    resulttext.setText("ret: " + ret + "count : " + count + flag);
                                    ret = 0;

                                    Thread.sleep(100);
                                }

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if(flag == false){
                        while (count < 2) {
                            try {
                                DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                                MatOfDMatch matches = new MatOfDMatch();
                                if (testMat.get(1).cols() == descriptors1.cols()) {
                                    matcher.match(testMat.get(1), descriptors1, matches);

                                    DMatch[] match = matches.toArray();
                                    double max_dist = 0;
                                    double min_dist = 100;

                                    for (int i = 0; i < descriptors1.rows(); i++) {
                                        double dist = match[i].distance;
                                        if (dist < min_dist) min_dist = dist;
                                        if (dist > max_dist) max_dist = dist;
                                    }

                                    for (int i = 0; i < descriptors1.rows(); i++) {
                                        if (match[i].distance <= 50) {
                                            ret++;

                                            //resulttext.setText("rows : " + testMat.get(0).rows() + "cols : " + testMat.get(0).cols() + "sum : " + (testMat.get(0).rows() * testMat.get(0).cols()) + "result : " + ret);
                                        }
                                    }
                                    if (ret > 100) count++;
                                    else flag = true;
                                    resulttext.setText("ret: " + ret + "count : " + count+ flag);
                                    ret = 0;

                                    Thread.sleep(100);
                                }

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                count = 0;
            }
        });


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //testMat.add(descriptors1);

                if (testMat.size() == 2) {
                    //resulttext.setText("ok");
                    DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                    MatOfDMatch matches = new MatOfDMatch();
                    if (testMat.get(0).cols() == testMat.get(1).cols()) {
                        matcher.match(testMat.get(0), testMat.get(1), matches);

                        DMatch[] match = matches.toArray();
                        double max_dist = 0;
                        double min_dist = 100;

                        for (int i = 0; i < testMat.get(0).rows(); i++) {
                            double dist = match[i].distance;
                            if (dist < min_dist) min_dist = dist;
                            if (dist > max_dist) max_dist = dist;
                        }

                        for (int i = 0; i < testMat.get(0).rows(); i++) {
                            if (match[i].distance <= 50) {
                                ret++;

                                //resulttext.setText("rows : " + testMat.get(0).rows() + "cols : " + testMat.get(0).cols() + "sum : " + (testMat.get(0).rows() * testMat.get(0).cols()) + "result : " + ret);
                            }
                        }
                        if(ret > 200) count++;
                        testtext.setText("ret: " + ret + "count : " + count);
                        ret = 0;
                    }
                    testMat.clear();
                    //resulttext.setText("differenct : " + ret);
                } else if (testMat.size() > 2) {
                    testMat.clear();
                    ret = 0;
                    //resulttext.setText("clear");
                } else {
                    //resulttext.setText("aa");
                    ret = 0;
                }

            }
        });

        if(isStart){
            DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
            MatOfDMatch matches = new MatOfDMatch();

            tempret = 0;
            if (testMat.get(0).cols() == descriptors1.cols()) {

                matcher.match(testMat.get(0), descriptors1, matches);

                DMatch[] match = matches.toArray();
                double max_dist = 0;
                double min_dist = 100;

                for (int i = 0; i < testMat.get(0).rows(); i++) {
                    double dist = match[i].distance;
                    if (dist < min_dist) min_dist = dist;
                    if (dist > max_dist) max_dist = dist;
                }

                for (int i = 0; i < testMat.get(0).rows(); i++) {
                    if (match[i].distance <= 20) {
                        tempret++;
                    }
                }
                //resulttext.setText("result : " + tempret);
            }

        }


        startbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               /* tts.setPitch(1.0f);
                tts.setSpeechRate(1.0f);
                tts.speak("안녕하세요, 시작합니다", TextToSpeech.QUEUE_FLUSH, null);*/
                /*isStart = true;
                if(temp == 1){
                    //resulttext.setText(tempret);
                }*/
                testMat.add(descriptors1);
                resulttext.setText("first" + testMat.size());
            }

        });

        endbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*tts.setPitch(1.0f);
                tts.setSpeechRate(1.0f);
                tts.speak("안녕하세요, 시작합니다", TextToSpeech.QUEUE_FLUSH, null);*/
                /*isStart = true;
                if(temp == 1){
                    //resulttext.setText(tempret);
                }*/
                testMat.add(descriptors1);
                resulttext.setText("second" + testMat.size());
            }

        });
        return outputImage;
    }
    //여기서부턴 퍼미션 관련 메소드
    static final int PERMISSIONS_REQUEST_CODE = 1000;
    String[] PERMISSIONS = {"android.permission.CAMERA"};

    private boolean hasPermissions(String[] permissions) {
        int result;
        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions) {
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED) {
                //허가 안된 퍼미션 발견
                return false;
            }
        }
        //모든 퍼미션이 허가되었음
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraPermissionAccepted = grantResults[0]
                            == PackageManager.PERMISSION_GRANTED;

                    if (!cameraPermissionAccepted)
                        showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
                }
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(OpenCVTest.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                finish();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Toast.makeText(getBaseContext(), "resultCode : " + resultCode, Toast.LENGTH_SHORT).show();
    }

    private class TestThread extends Thread{
        public TestThread(){
        }
        @Override
        public void run(){
            resulttext.setText("wwwwwwwww");
            if(testMat.size() == 2) {
                resulttext.setText("hiyo");
                while (count < 10) {
                    try {
                        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
                        MatOfDMatch matches = new MatOfDMatch();
                        if (testMat.get(0).cols() == descriptors1.cols()) {
                            matcher.match(testMat.get(0), descriptors1, matches);

                            DMatch[] match = matches.toArray();
                            double max_dist = 0;
                            double min_dist = 100;

                            for (int i = 0; i < descriptors1.rows(); i++) {
                                double dist = match[i].distance;
                                if (dist < min_dist) min_dist = dist;
                                if (dist > max_dist) max_dist = dist;
                            }

                            for (int i = 0; i < descriptors1.rows(); i++) {
                                if (match[i].distance <= 50) {
                                    ret++;

                                    //resulttext.setText("rows : " + testMat.get(0).rows() + "cols : " + testMat.get(0).cols() + "sum : " + (testMat.get(0).rows() * testMat.get(0).cols()) + "result : " + ret);
                                }
                            }
                            if (ret > 200) count++;
                            testtext.setText("ret: " + ret + "count : " + count);
                            ret = 0;
                        }
                        Thread.sleep(1);
                    }catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}