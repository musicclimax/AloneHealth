package com.example.caucse.alonehealth;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;

public class DifferenceImageTest extends AppCompatActivity
        implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "opencv";
    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat matInversion;
    private Mat matGray;
    private Mat matInput;
    //딜레이 카운트
    private int count = 0;
    //쓰레드 핸들러
    Handler mHandler = null;
    //표본 Mat
    private Mat matStartSample;
    private Mat matEndSample;
    private Mat matDifference = new Mat();
    int max_difference = 0;
    // 테스트 View
    private TextView stateTextView;
    private TextView imageSwitch;
    private TextView logSwitch;
    private TextView logTextView;
    private TextView countTextVeiw;
    private Button startButton;
    // 테스트 state
    private final static int INIT_STATE = -1;
    private final static int START_STATE = 0;
    private final static int SAMPLING_STATE = 1;
    private final static int END_STATE = 2;
    private final static int READY_STATE = 3;
    private final static int COUNT_STATE = 4;
    private final static int FINAL_STATE = 5;
    int testState;
    // 테스트 COUNT 시간
    private final static int PREPARE_COUNT = 3;
    private final static int SAMPLING_COUNT = 5;
    // 테스트 결과 image state
    private int testResultState;
    private final static int START_RESULT = 0;
    private final static int END_RESULT = 1;
    private final static int DIFF_RESULT = 2;
    private String log = new String();

    // COUNTING 테스트 STATE
    private int countState;
    private final static int START_COUNTING = 0;
    private final static int END_COUNTING = 1;

    //Thread <-> Frame Test
    private int NumberOfThread = 0;
    private int NumberOfFrame = 0;
    private long countEnd = 0;
    private long msgEnd = 0;
    //Circular Queue
    MatCirCularQueue frameBuffer;




    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);
    public native void InvertMat(long matAddrInput, long matAddrResult);
    public native int CountWhitePixels(long matAddrInput);
    public native int CountWhitePixelsInOneRow(long matAddrInput, int indexOfStart, int indexOfEnd);
    public native void CreateDifferenceImage(long matAddrSource, long matAddrTarget, long matAddrResult);


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

        /** Initialize */
        testState = INIT_STATE;
        testResultState = START_STATE;
        countState = START_COUNTING;
        frameBuffer = new MatCirCularQueue();
        //View Instance 생성
        imageSwitch = (TextView)findViewById(R.id.switch_image);
        stateTextView = (TextView)findViewById(R.id.state_text);
        logSwitch = (TextView)findViewById(R.id.switch_log);
        countTextVeiw = (TextView)findViewById(R.id.count_text);
        logTextView = (TextView)findViewById(R.id.log_text);
        startButton = (Button)findViewById(R.id.start_test_button);

        //테스트 시작버튼 클릭 리스너
        startButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //테스트 STATE 변경
                if(testState == INIT_STATE) {
                    testState = START_STATE;
                    stateTextView.setText("시작 단계");
                    //버튼 숨기기
                    startButton.setVisibility(View.GONE);
                    //카운트 보이기
                    countTextVeiw.setVisibility(View.VISIBLE);
                    countTextVeiw.setText(String.valueOf(PREPARE_COUNT));
                    //타이머 쓰레드 시작
                    TimerThread timerThread = new TimerThread(PREPARE_COUNT);
                    timerThread.setDaemon(true);
                    timerThread.start();
                }
                else if(testState == END_STATE){
                    testState = READY_STATE;
                    stateTextView.setText("카운트 단계");
                    //버튼 숨기기
                    startButton.setVisibility(View.INVISIBLE);
                    logTextView.setVisibility(View.INVISIBLE);
                    logSwitch.setVisibility(View.INVISIBLE);
                    imageSwitch.setVisibility(View.INVISIBLE);
                    countTextVeiw.setVisibility(View.VISIBLE);
                    countTextVeiw.setText("");
                    TimerThread timerThread = new TimerThread(PREPARE_COUNT);
                    timerThread.start();
                }
            }
        });

        //이미지 전환 버튼 클릭 리스너
        imageSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //테스트 RESULT STATE 변경
                if(testResultState == START_RESULT) {
                    testResultState = END_RESULT;
                    stateTextView.setText("끝 자세 표본");
                }
                else if(testResultState == END_RESULT) {
                    testResultState = DIFF_RESULT;
                    stateTextView.setText("표본 차영상");
                }
                else if(testResultState == DIFF_RESULT){
                    testResultState = START_RESULT;
                    stateTextView.setText("시작 자세 표본");
                }
            }
        });

        //로그 보기 버튼 클릭 리스너
        logSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(logTextView.getVisibility() == View.VISIBLE)
                    logTextView.setVisibility(View.INVISIBLE);
                else
                    logTextView.setVisibility(View.VISIBLE);
            }
        });

        //UI처리 쓰레드 핸들러
        mHandler = new Handler(){
            public void handleMessage(Message msg){

                switch(testState){
                    case INIT_STATE:
                        break;
                    case START_STATE:
                        if(msg.what != -1)
                            countTextVeiw.setText(String.valueOf(msg.what));
                        else {
                            testState = SAMPLING_STATE;
                            stateTextView.setText("표본 추출 단계");
                            countTextVeiw.setText(String.format("표본 추출중 %d",SAMPLING_COUNT));
                            TimerThread timerThread = new TimerThread(SAMPLING_COUNT);
                            timerThread.setDaemon(true);
                            timerThread.start();
                        }
                        break;
                    case SAMPLING_STATE:
                        if(msg.what != -1) {
                            countTextVeiw.setText(String.format("표본 추출중 %d", msg.what));
                        }
                        else{
                            stateTextView.setText("표본 추출 완료");
                            countTextVeiw.setVisibility(View.GONE);
                            imageSwitch.setVisibility(View.VISIBLE);
                            logSwitch.setVisibility(View.VISIBLE);
                            logTextView.setVisibility(View.VISIBLE);
                            logTextView.setText(log + String.format("MaxDifference : %d",max_difference));
                            startButton.setVisibility(View.VISIBLE);
                            testState=END_STATE;
                        }
                        break;
                    case END_STATE:
                        break;
                    case READY_STATE:
                        if(msg.what != -1){
                            countTextVeiw.setText(""+msg.what);
                        }
                        else{
                            testState = COUNT_STATE;
                            countTextVeiw.setText("START");
                            mHandler.sendEmptyMessage(0);
                        }
                        break;
                    case COUNT_STATE:
                        stateTextView.setText("MAX : " + (int)(max_difference/5) + " Difference : " + msg.what + " STATE : " + countState);
                        if(count < 5){
                            countTextVeiw.setText(""+count);
                            msgEnd = System.currentTimeMillis();
                            Log.d(TAG, String.format("--------------------\ncount <-> msg intervasl : %d",msgEnd - countEnd));
                        }
                        else{
                            Log.d(TAG, String.format("--------------------\nTest END\nThread : %d\nFrame : %d",NumberOfThread,NumberOfFrame));
                            testState = FINAL_STATE;
                            countTextVeiw.setText("THE END");
                        }
                        break;
                    case FINAL_STATE:
                        break;
                }

            }
        };
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

        switch(testState){
            case INIT_STATE:
                break;
            case START_STATE:
                break;
            case SAMPLING_STATE:
                if(matStartSample == null){
                    matStartSample = matGray.clone();
                }
                else{
                    SampingThread sampingThread = new SampingThread();
                    sampingThread.start();
                }
                break;
            case END_STATE:
                if(testResultState == START_RESULT)
                    return matStartSample;
                else if(testResultState == END_RESULT)
                    return matEndSample;
                else
                    return matDifference;
            case COUNT_STATE:
                NumberOfFrame ++;
                frameBuffer.Enqueue(matGray);
                CountingThread countingThread = new CountingThread();
                countingThread.start();
                break;
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

        AlertDialog.Builder builder = new AlertDialog.Builder( DifferenceImageTest.this);
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
        private int count;
        TimerThread(int count){
            this.count = count;
        }
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
            mHandler.sendEmptyMessage(-1);
        }
    }
    public class SampingThread extends Thread{
        public synchronized void run(){
            if(testState == SAMPLING_STATE) {
                Mat matDiff = new Mat(matGray.rows(), matGray.cols(), matGray.type());
                CreateDifferenceImage(matGray.getNativeObjAddr(),matStartSample.getNativeObjAddr(),matDiff.getNativeObjAddr());
                int difference = CountWhitePixels(matDiff.getNativeObjAddr());
                log += String.format("Difference : %d ",difference);

                if (difference > max_difference) {
                    max_difference = difference;
                    matDifference = matDiff.clone();
                    matEndSample = matGray.clone();
                }
            }
        }
    }

    public class CountingThread extends Thread{
        public void run(){
            long start = System.currentTimeMillis();
            Log.d(TAG, String.format("Thread %d : start",this.getId()));
            if(testState == COUNT_STATE) {
                countUsingDiff();
            }
            long end = System.currentTimeMillis();
            NumberOfThread++;
            Log.d(TAG, String.format("Thread %d : end\n Thread Time : %d",this.getId(),end-start));
        }
    }
    public void countUsingDiff(){
        Mat matFrame = frameBuffer.Dequeue();
        Mat matStartDiff= new Mat(matFrame.rows(), matFrame.cols(), matFrame.type());
        Mat matEndDiff= new Mat(matFrame.rows(), matFrame.cols(), matFrame.type());
        long start = System.currentTimeMillis();
        CreateDifferenceImage(matFrame.getNativeObjAddr(), matEndSample.getNativeObjAddr(), matEndDiff.getNativeObjAddr());
        CreateDifferenceImage(matFrame.getNativeObjAddr(), matStartSample.getNativeObjAddr(), matStartDiff.getNativeObjAddr());
        long end = System.currentTimeMillis();
        Log.d(TAG,String.format("CreateDifferenceImage time : %d",end - start));
        start = System.currentTimeMillis();
        int differenceWithStart = sumOfWhitePixels(matStartDiff,8);
        int differenceWithEnd = sumOfWhitePixels(matEndDiff,8);
        end = System.currentTimeMillis();
        Log.d(TAG,String.format("CountWhitePixels time : %d",end-start));
        counting(differenceWithStart,differenceWithEnd);
    }
    private synchronized void counting(int differenceWithStart, int differenceWithEnd){
        if(countState == START_COUNTING){
            if(differenceWithEnd < max_difference / 5){
                countState = END_COUNTING;
            }

        }
        else if(countState == END_COUNTING){
            if(differenceWithStart < max_difference / 5){
                count++;
                countState = START_COUNTING;
                mHandler.sendEmptyMessage(differenceWithStart);
                countEnd = System.currentTimeMillis();
            }
        }
    }
    class CountPixelThread extends Thread{
        int indexOfStart;
        int indexOfEnd;
        long matInput;
        int ans;
        CountPixelThread(long matInput, int start, int end){ this.matInput = matInput; this.indexOfStart = start; this.indexOfEnd = end; this.ans = 0;}

        @Override
        public void run() {
            this.ans = CountWhitePixelsInOneRow(matInput,indexOfStart,indexOfEnd);
        }

    }
    private int sumOfWhitePixels(Mat mSource, int numberOfThread){
        int sum = 0;
        CountPixelThread countPixelThread[] = new CountPixelThread[numberOfThread];
        for(int i = 0; i < numberOfThread; i ++){
            countPixelThread[i] = new CountPixelThread(mSource.getNativeObjAddr(),(i*mSource.rows())/numberOfThread,((i+1)*mSource.rows())/numberOfThread);
            countPixelThread[i].start();
        }
        for(int i = 0; i < numberOfThread; i++){
            try {
                countPixelThread[i].join();
                sum += countPixelThread[i].ans;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        return sum;
    }


}
