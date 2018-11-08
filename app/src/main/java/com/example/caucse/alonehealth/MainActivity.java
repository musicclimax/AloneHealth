package com.example.caucse.alonehealth;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.PaintDrawable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity{

    int bot_state = 0;

    //터치좌표
    private float x;
    private float y;
    private static final int MAX_CLICK_DISTANCE = 80;
    //드래그중임을 확인하는 boolean 변수
    //드래그중 = true , 드래그아닐시 = false;
    boolean onDrag = false;

    //날짜 표시 포맷
    Date today = new Date();
    SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy. MM. dd");
    SimpleDateFormat DateFormat_Day = new SimpleDateFormat("E", Locale.KOREAN);

    //////////하단 첫화면 레이아웃//////////////
    DateAdapter at; //어댑터
    CustomViewPager vp;   //뷰페이저
    /////////////////////////////////////////

    //////////하단 캘린더 레이아웃//////////////
    TextView textview_DATE_Cal;
    TextView textview_DAY_Cal;

    ListView cal_listView;
    ExerciseListViewAdapter cal_adapter;

    int preSelectPos;   //이전 포지션 위치
    //////////////////////////////////////////

    ////////////////운동을 시작하기 위해 필요//////////////////
    Button startbutton; //운동 시작 버튼
    String selectedDate;  //선택된 날짜
    String selectedItemId;  //리스트에서 선택된 아이디값
    String selectedEN;              //운동 이름
    int selectedSet;                //운동의 세트수
    int selectednumber;             //세트 횟수
    ////////////////////////////////////////////////////////

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar)findViewById(R.id.myToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        //시작 버튼 셋팅
        startbutton = (Button)findViewById(R.id.startbutton);
        startbutton.setVisibility(View.INVISIBLE);  //초기에 보이지 않게

        ///////////////하단 첫화면 초기 셋팅//////////////////
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
        vp.setPaggingFlg(true);
        /////////////////////////////////////////////////////

        ///////////////하단 캘린더 초기 셋팅//////////////////
        // list view adapter
        cal_adapter = new ExerciseListViewAdapter();
        //리스트뷰 셋팅은 뷰 생성 후
        ////////////////////////////////////////////////////

        /*일정관리 버튼 누를시
         *일정관리페이지로 이동*/
        toolbar.findViewById(R.id.Schedule_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),
                        CalendarManager.class);
                startActivity(intent);
            }
        });

        /*홈 버튼 누를시
         *첫화면으로 이동*/
        toolbar.findViewById(R.id.home_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(),
                        MainActivity.class);
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

                            float touch_distance = distance(x,y,postx,posty);
                            if (bot_state == 0
                                    && touch_distance < MAX_CLICK_DISTANCE)  //하단 화면의 상태가 첫화면일때
                            {
                            //뷰페이저 페이징 터치리스너 해제
                            vp.setPaggingFlg(false);

                            //DB 날짜 데이터 포맷
                            SimpleDateFormat DB_SDF = new SimpleDateFormat("yyyy. MM. dd");
                            String date_str = at.DateList.get(vp.getCurrentItem()).date_str;    //현재날짜
                            selectedDate = date_str;    //선택된 날짜 셋팅
                            cal_adapter.setListViewItemList(SQLiteManager.sqLiteManager.selectScheduleFromDate(date_str));

                            if(cal_adapter.getCount() == 0)
                            {
                                //DB에 운동목록이 없을때
                                change_bot_view(0);
                                //운동 추가 뷰로 이동
                            }
                            else
                            {
                                change_bot_view(1);
                                //날짜 동기화
                                TextView tv = findViewById(R.id.dateView_Cal);
                                tv.setText(at.DateList.get(vp.getCurrentItem()).date_str);

                                //리스트뷰에 DB운동 리스트 추가
                                cal_listView = (ListView)findViewById(R.id.listView_Cal);
                                cal_listView.setAdapter(cal_adapter);

                                //이전 선택위치 초기화 = 리스트 크기+1 (초기에 겹치지 않게)
                                preSelectPos = -1;
                                ///////////리스트뷰 클릭 리스너///////////
                                cal_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                        {
                                            cal_listView.refreshDrawableState();
                                            if(preSelectPos != i)
                                            {
                                                preSelectPos = i;
                                                startbutton.setVisibility(View.VISIBLE);
                                                for(int iCnt = 0; iCnt< cal_listView.getAdapter().getCount();iCnt++)
                                                {
                                                    if(iCnt == i)
                                                    {
                                                        getViewByPosition(iCnt,cal_listView).setBackgroundColor(Color.GREEN);
                                                        //선택된 날짜 및 아이디값 셋팅
                                                        ScheduleData temp = (ScheduleData)adapterView.getItemAtPosition(i);
                                                        selectedItemId = temp.getId();

                                                        selectedEN = temp.getExercise_id();
                                                        selectedSet = temp.getSet();
                                                        selectednumber = temp.getNumber();
                                                    }
                                                    else
                                                    {
                                                        getViewByPosition(iCnt,cal_listView).setBackgroundColor(Color.TRANSPARENT);
                                                    }
                                                }
                                            }
                                            else if(preSelectPos == i)
                                            {
                                                getViewByPosition(i,cal_listView).setBackgroundColor(Color.TRANSPARENT);
                                                preSelectPos = -1;
                                                startbutton.setVisibility(View.INVISIBLE);
                                            }
                                        }
                                    }
                                });
                            }
                            /*
                            if()//해당날짜의 리스트가 존재하지 않을때
                            else()//해당날짜의 리스타가 존재할때
                            */
                            } else if (bot_state == 0
                                    && touch_distance > MAX_CLICK_DISTANCE
                                    && x > postx) {
                            /*
                            왼쪽으로 슬라이드 했을때 리스트에 요소를 항상 5개로 유지 하기 위해
                            마지막 리스트에 요소 추가, 처음 리스트에 요소 제거
                             */
                                at.addToList(true);
                                at.CalWhatday(at.DateList.size()-1, true);

                                at.deletetoList(false);

                            } else if (bot_state == 0
                                    && touch_distance > MAX_CLICK_DISTANCE
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

        ///////////시작 버튼 터치 리스너///////////
        startbutton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if(action == MotionEvent.ACTION_DOWN){
                    startbutton.setBackgroundResource(R.drawable.startbutton1);
                }
                else if(action == MotionEvent.ACTION_UP){
                    startbutton.setBackgroundResource(R.drawable.startbutton);

                    /**
                     * 운동시작화면으로 이동 로직 추가
                     */
                    //운동 실행결과 갱신 - 임시
                    if(SQLiteManager.sqLiteManager.updateScheduleData(new ScheduleData(selectedItemId,selectedDate,selectedItemId,selectedSet,selectednumber,1))) {
                        //Toast.makeText(getContext(), "성공적으로 수정되었습니다..", Toast.LENGTH_SHORT).show();
                        cal_adapter.setListViewItemList(SQLiteManager.sqLiteManager.selectScheduleFromDate(selectedDate));
                        cal_adapter.notifyDataSetChanged();
                    }
                }
                return true;
            }
        });
    }

    //리스트뷰에서 포지션위치의 자식을 받아오는 함수
    public View getViewByPosition(int position, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (position < firstListItemPosition || position > lastListItemPosition ) {
            return listView.getAdapter().getView(position,listView.getChildAt(position), listView);
        } else {
            final int childIndex = position - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }
    private static float distance(float x1, float y1, float x2, float y2)
    {
        float dx = x1-x2;
        float dy = y1-y2;
        float distanceInPx = (float)Math.sqrt(dx*dx + dy*dy);
        return distanceInPx;
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

}