package com.example.caucse.alonehealth;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Adapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity{

    int bot_state = 0;

    //터치좌표
    private float x;
    private float y;

    //드래그중임을 확인하는 boolean 변수
    //드래그중 = true , 드래그아닐시 = false;
    boolean onDrag = false;

    //날짜 표시 포맷
    Date today = new Date();
    SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy-MM-dd");
    SimpleDateFormat DateFormat_Day = new SimpleDateFormat("E", Locale.KOREAN);

    //첫번째 하단 레이아웃
    //TextView textview_DATE_Fir;
    //TextView textview_DAY_Fir;

    DateAdapter at; //어댑터
    CustomViewPager vp;   //뷰페이저

    //캘린더 하단 레이아웃
    TextView textview_DATE_Cal;
    TextView textview_DAY_Cal;



    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.myToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //뷰페이저 및 어댑터 셋팅
        vp = (CustomViewPager) findViewById(R.id.pager);
        at = new DateAdapter(this);
        vp.setAdapter(at);

        /////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////뷰페이저 체인지리스너//////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////
        vp.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {
                if(position != 2)
                {
                    vp.setCurrentItem(2);
                }
            }
            @Override
            public void onPageSelected(int position)
            {
                if(position != 2)
                {
                    vp.setCurrentItem(2);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {

            }
        });
        /////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////

        vp.setCurrentItem(2);   //첫페이지는 가운데 페이지
        /*일정관리 버튼 누를시
         *일정관리페이지(Test)로 이동*/
        toolbar.findViewById(R.id.Schedule_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),
                        CalendarManager.class);
                startActivity(intent);
            }
        });
        //바텀레이아웃 터치이벤트


        vp.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction())
                {
                    case MotionEvent.ACTION_MOVE:
                        break;
                    case MotionEvent.ACTION_DOWN:
                        //처음에 한번 체크 하고 그후 드래그

                        if(onDrag == false) {
                            x = event.getX();
                            y = event.getY();
                            onDrag = true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        //터치가 떼어졌을때
                        //드래그중일때
                        if(onDrag) {
                            float postx = event.getX();
                            float posty = event.getY();

                            if (bot_state == 0
                                    && x == postx
                                    && y == posty)  //하단 화면의 상태가 첫화면일때
                            {
                            /*
                            if()//해당날짜의 리스트가 존재하지 않을때
                            else()//해당날짜의 리스타가 존재할때
                            */
                                vp.setPaggingFlg(false);

                                change_bot_view(1);
                                TextView tv = findViewById(R.id.dateView_Cal);
                                tv.setText(at.DateList.get(vp.getCurrentItem()).date_str);

                                ///////////////////DB에서 리스트데이터를 가져오는 부분 추가///////////////////


                                //////////////////////////////////////////////////////////////////////////
                            } else if (bot_state == 0
                                    && x > postx) {
                            /*
                            왼쪽으로 슬라이드 했을때 리스트에 요소를 항상 5개로 유지 하기 위해
                            마지막 리스트에 요소 추가, 처음 리스트에 요소 제거
                             */
                                at.addToList(true);
                                at.CalWhatday(at.DateList.size()-1, true);

                                at.deletetoList(false);

                            } else if (bot_state == 0
                                    && x < postx) {
                            /*
                            오른쪽으로 슬라이드 했을때 리스트에 요소를 항상 5개로 유지 하기 위해
                            처음 리스트에 요소 추가, 마지막 리스트에 요소 제거
                             */
                                at.addToList(false);
                                at.CalWhatday(0, false);

                                at.deletetoList(true);
                            }
                        }
                        //드래그 해제
                        onDrag = false;
                        break;
                }

                return false;
                }
                }
        );

        FrameLayout ll = (FrameLayout)findViewById(R.id.MAIN_FL_BOT);
        ll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    //터치 케이스
                    case MotionEvent.ACTION_UP:
                        if (bot_state == 1)
                        {
                            //화면 터치시 운동 일정관리페이지로 이동
                            Intent intent = new Intent(getApplicationContext(),
                                    CalendarManager.class);
                            startActivity(intent);
                        }
                }
                return true;
            }
        });
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    private void change_bot_view(int index)
    {
        //레이아웃인플레터 초기화
        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        FrameLayout frame = (FrameLayout)findViewById(R.id.MAIN_FL_BOT);

        if(frame.getChildCount() > 0)
        {
            // 프레임레이아웃에서 뷰 삭제
            frame.removeViewAt(0);
        }

        View view = null;

        switch (index){
            case 0 :    //일정추가 뷰
                view = inflater.inflate(R.layout.main_bot_add, frame, false);

                bot_state = 1;
                break;
            case 1 :    //운동캘린더 뷰
                view = inflater.inflate(R.layout.main_bot_cal, frame, false);
                bot_state = 2;
                break;
        }

        //BOT 프레임레이아웃 ID값 비교함수 추가예정

        //프레임레이아웃에 뷰 추가
        if(view != null) {
            frame.addView(view);
        }
    }

    public native String stringFromJNI();
}