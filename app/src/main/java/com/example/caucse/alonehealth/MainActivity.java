package com.example.caucse.alonehealth;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.myToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

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
        LinearLayout ll = (LinearLayout)findViewById(R.id.MAIN_LL_BOT);
        ll.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    //터치 케이스
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_UP:
                        /*
                        if()//해당날짜의 리스트가 존재하지 않을때
                            else()//해당날짜의 리스타가 존재할때

                        */

                        change_bot_view(1);
                }
                return true;
            }
        });

        //Bot 첫화면 설정
        change_bot_view(0);
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
            case 0 :    //첫화면 뷰
                view = inflater.inflate(R.layout.main_bot_fir, frame, false);
                break;
            case 1 :    //일정추가 뷰
                view = inflater.inflate(R.layout.main_bot_add, frame, false);
                break;
            case 2 :    //운동캘린더 뷰
                view = inflater.inflate(R.layout.main_bot_cal, frame, false);
        }

        //BOT 프레임레이아웃 ID값 비교함수 추가예정

        //프레임레이아웃에 뷰 추가
        if(view != null) {
            frame.addView(view);
        }
    }

    public native String stringFromJNI();
}