package com.example.caucse.alonehealth;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;

import android.support.v7.app.AlertDialog;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import static org.opencv.core.Core.flip;


public class PictureSamplingTest extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "opencv";
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat matInversion;
    private Mat matGray;
    private Mat matInput;
    //테스트 시작 버튼
    private Button startTestButton;
    //테스트 로그 텍스트뷰
    private TextView testStateTextView;
    //딜레이 카운트
    private int count = 7;
    //쓰레드 핸들러
    Handler mHandler = null;
    //타이머 쓰레드
    TimerThread timerThread;
    //표본 Mat
    private Mat mStartSample;
    private Mat mEndSample;
    int min_distance = Integer.MAX_VALUE;
    // 테스트 state
    boolean stateInit = true;
    boolean stateTestStart = false;
    boolean stateSamplingStart = false;
    boolean stateTestEnd = false;
    //테스트 결과 view
    private Mat startResultImage;
    private Mat endResultImage;
    private View startResultView;
    private View endResultView;

    int jw = 0;

    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);
    public native void InvertMat(long matAddrInput, long matAddrResult);


    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native-lib");
    }



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_sampling);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //퍼미션 상태 확인
            if (!hasPermissions(PERMISSIONS)) {

                //퍼미션 허가 안되어있다면 사용자에게 요청
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }
        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.sampling_camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(1); // front-camera(1),  back-camera(0)
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);


        //UI처리 쓰레드 핸들러
        mHandler = new Handler(){
          public void handleMessage(Message msg){
              if(stateSamplingStart&&!stateTestStart&&!stateTestEnd){
                  if(msg.what != 0){
                      testStateTextView.setText("표본 추출중 " + count);
                  }
                  else{
                      stateSamplingStart = false;
                      testStateTextView.setText("표본 추출 완료" + compareFeature(mEndSample,mEndSample) + " , " + compareFeature(mStartSample,mStartSample) + " , " + compareFeature(mStartSample,mEndSample));
                      testStateTextView.setTextSize(20);
                      stateTestEnd = true;
                  }
              }
              else if(!stateSamplingStart&&!stateTestStart&&!stateTestEnd&&stateInit){
                  if(msg.what != 0)
                      testStateTextView.setText(String.valueOf(msg.what));
                  else {
                      stateTestStart = true;
                      stateInit = false;
                  }
              }


          }
        };
        //테스트 시작 버튼 리스너
        startTestButton = (Button)findViewById(R.id.start_test_button);
        testStateTextView = (TextView)findViewById(R.id.state_test_text);
        startTestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTestButton.setVisibility(View.INVISIBLE);
                testStateTextView.setVisibility(View.VISIBLE);
                timerThread = new TimerThread();
                timerThread.start();
            }
        });

        //테스트 결과 view
        startResultView = (ImageView)findViewById(R.id.test_result_start);
        endResultView = (ImageView)findViewById(R.id.test_result_end);

        testStateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap1 = Bitmap.createBitmap(startResultImage.width(),startResultImage.height(),null);
                Utils.matToBitmap(startResultImage,bitmap1);
                BitmapDrawable ob = new BitmapDrawable(getResources(),bitmap1);
                startResultView.setBackgroundDrawable(ob);
                Bitmap  bitmap2 = Bitmap.createBitmap(startResultImage.width(),startResultImage.height(),null);
                Utils.matToBitmap(endResultImage,bitmap2);
                ob = new BitmapDrawable(getResources(),bitmap2);
                endResultView.setBackgroundDrawable(ob);
                startResultView.setVisibility(View.VISIBLE);
                endResultView.setVisibility(View.VISIBLE);
                testStateTextView.setVisibility(View.INVISIBLE);

            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    @Override
    public void onPause()
    {
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        super.onPause();
    }

    public void onDestroy() {
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        super.onDestroy();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matInput = inputFrame.rgba();
        if(matInversion == null)
            matInversion = new Mat(matInput.rows(),matInput.cols(),matInput.type());
        InvertMat(matInput.getNativeObjAddr(),matInversion.getNativeObjAddr());
        if(matGray == null)
            matGray = new Mat(matInput.rows(), matInput.cols(), matInput.type());
        ConvertRGBtoGray(matInversion.getNativeObjAddr(), matGray.getNativeObjAddr());

        // 테스트 시작 단계
        if(stateTestStart){
            mStartSample = extractDescriptor(matGray);
            startResultImage = matInversion.clone();
            stateTestStart = false;
            stateSamplingStart = true;

            /**테스트 시작*/
            count = 5;
            timerThread = new TimerThread();
            timerThread.start();
        }
        else if(stateSamplingStart){
            CompareThread compareThread = new CompareThread();
            compareThread.start();
        }
        return matInversion;
    }



    //여기서부턴 퍼미션 관련 메소드
    static final int PERMISSIONS_REQUEST_CODE = 1000;
    String[] PERMISSIONS  = {"android.permission.CAMERA"};


    private boolean hasPermissions(String[] permissions) {
        int result;

        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
        for (String perms : permissions){

            result = ContextCompat.checkSelfPermission(this, perms);

            if (result == PackageManager.PERMISSION_DENIED){
                //허가 안된 퍼미션 발견
                return false;
            }
        }

        //모든 퍼미션이 허가되었음
        return true;
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){

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

        AlertDialog.Builder builder = new AlertDialog.Builder( PictureSamplingTest.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id){
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

    public class TimerThread extends Thread{
        public void run(){
            while(count > 0){
                mHandler.sendEmptyMessage(count);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count --;
            }
            mHandler.sendEmptyMessage(count);
        }
    }
    public class CompareThread extends Thread{
        public void run(){
            if(!stateTestEnd&&stateSamplingStart) {
                Mat matCandidate = extractDescriptor(matGray);
                int distance;
                /**표본추출 시작*/
                distance = compareFeature(mStartSample, matCandidate);
                Log.d("min : ",String.valueOf(min_distance));
                if (distance < min_distance) {
                    min_distance = distance;
                    mEndSample = matCandidate;
                    endResultImage = matInversion.clone();

                }
            }
        }
    }
    //ORB Feature 추출
    public Mat extractDescriptor(Mat mSource){
        Mat mResult = new Mat();
        MatOfKeyPoint keyPoint = new MatOfKeyPoint();
        FeatureDetector detector = FeatureDetector.create(FeatureDetector.ORB);
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        detector.detect(mSource,keyPoint);
        extractor.compute(mSource,keyPoint,mResult);
        return mResult;
    }
    public static int compareFeature(Mat mSource, Mat mTarget) {
        int retVal = 0;

        // Definition of descriptor matcher
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);

        // Match points of two images
        MatOfDMatch matches = new MatOfDMatch();
        //  System.out.println("Type of Image1= " + descriptors1.type() + ", Type of Image2= " + descriptors2.type());
        //  System.out.println("Cols of Image1= " + descriptors1.cols() + ", Cols of Image2= " + descriptors2.cols());

        // Avoid to assertion failed
        // Assertion failed (type == src2.type() && src1.cols == src2.cols && (type == CV_32F || type == CV_8U)
        if (mTarget.cols() == mSource.cols()) {
            matcher.match(mTarget, mSource ,matches);

            // Check matches of key points
            DMatch[] match = matches.toArray();

            /*
            double max_dist = 0; double min_dist = 100;

            for (int i = 0; i < mSource.rows(); i++) {
                double dist = match[i].distance;
                if( dist < min_dist ) min_dist = dist;
                if( dist > max_dist ) max_dist = dist;
            }
            System.out.println("max_dist=" + max_dist + ", min_dist=" + min_dist);
            */

            // Extract good images (distances are under 10)
            for (int i = 0; i < mSource.rows(); i++) {
                if (match[i].distance <= 10) {
                    retVal++;
                }
            }
        }

        return retVal;
    }


}