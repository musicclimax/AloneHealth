package com.example.caucse.alonehealth;


import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.CalendarMode;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.format.TitleFormatter;

import org.w3c.dom.Text;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;

import decorators.EventDecorator;
import decorators.OneDayDecorator;
import decorators.SaturdayDecorator;
import decorators.SundayDecorator;

public class CalendarManager extends AppCompatActivity {

    String time,kcal,menu;
    private final OneDayDecorator oneDayDecorator = new OneDayDecorator();
    Cursor cursor;
    MaterialCalendarView materialCalendarView;
    ListView listView;
    ExerciseListViewAdapter adapter;
    Button addExerciseButton;
    AddExerciseDialog addExerciseDialog;
    TextView currentDateTextView;
    Date selectedDate;
    SimpleDateFormat simpleDateFormat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);
        /**초기 세팅*/
        simpleDateFormat = new SimpleDateFormat("yyyy. MM. dd");
        //schedule list view adapter
        adapter = new ExerciseListViewAdapter();
        listView = (ListView)findViewById(R.id.listView);
        listView.setAdapter(adapter);

        /**운동 추가 다이얼로그 창 설정*/
        addExerciseDialog = new AddExerciseDialog(this,adapter);
        addExerciseDialog.getWindow().setGravity(Gravity.BOTTOM);

        /////////////////////////////////////////////////////////////////////
        /**운동 추가 버튼**/
        addExerciseButton = findViewById(R.id.add_exercise_button);


        /**날짜 최신화*/
        currentDateTextView = (TextView)findViewById(R.id.currentDate);
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        addExerciseDialog.currentDate = new Date(now);
        selectedDate = new Date(now);
        currentDateTextView.setText(simpleDateFormat.format(date));
        adapter.setListViewItemList(SQLiteManager.sqLiteManager.selectScheduleFromDate(simpleDateFormat.format(date)));
        /////////////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////////////////////
        materialCalendarView = (MaterialCalendarView)findViewById(R.id.calendarView);
        materialCalendarView.state().edit()
                .setFirstDayOfWeek(Calendar.SUNDAY)
                .setMinimumDate(CalendarDay.from(2017, 0, 1)) // 달력의 시작
                .setMaximumDate(CalendarDay.from(2030, 11, 31)) // 달력의 끝
                .setCalendarDisplayMode(CalendarMode.MONTHS)
                .commit();
        TitleFormatter titleFormatter = new TitleFormatter() {

            @Override
            public CharSequence format(CalendarDay day) {
                String format = new String(""+day.getYear()+"년 "+(day.getMonth()+1)+"월  ");
                return format;
            }
        };
        materialCalendarView.setTitleFormatter(titleFormatter);
        materialCalendarView.setTileSizeDp(44);
        materialCalendarView.setArrowColor(Color.WHITE);
        materialCalendarView.addDecorators(
                new SundayDecorator(),
                new SaturdayDecorator(),
                oneDayDecorator);

        String[] result = {"2017,03,18","2017,04,18","2017,05,18","2017,06,18"};

        new ApiSimulator(result).executeOnExecutor(Executors.newSingleThreadExecutor());

        // 날짜 클릭 이벤트 리스너
        materialCalendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                int Year = date.getYear();
                int Month = date.getMonth() + 1;
                int Day = date.getDay();

                Log.i("Year test", Year + "");
                Log.i("Month test", Month + "");
                Log.i("Day test", Day + "");

                String shot_Day = Year + "." + Month + "." + Day;

                Log.i("shot_Day test", shot_Day + "");
                materialCalendarView.clearSelection();

                oneDayDecorator.setDate(date.getDate());
                selectedDate = date.getDate();
                materialCalendarView.removeDecorator(oneDayDecorator);
                materialCalendarView.addDecorator(oneDayDecorator);
                currentDateTextView.setText(simpleDateFormat.format(date.getDate()));
                adapter.setListViewItemList(SQLiteManager.sqLiteManager.selectScheduleFromDate(simpleDateFormat.format(date.getDate())));
                adapter.notifyDataSetChanged();
                Toast.makeText(getApplicationContext(), shot_Day , Toast.LENGTH_SHORT).show();
            }
        });
        /////////////////////////////////////////////////////////////////////

        //버튼 클릭 이미지 변경
        addExerciseButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if(action == MotionEvent.ACTION_DOWN){
                    addExerciseButton.setBackgroundResource(R.drawable.addpressedbutton128);
                }
                else if(action == MotionEvent.ACTION_UP){
                    addExerciseButton.setBackgroundResource(R.drawable.addbutton128);
                    addExerciseDialog.setSelectedDate(selectedDate);
                    addExerciseDialog.show();
                }
                return true;
            }
        });
        /////////////////////////////////////////////////////////////////////

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                adapter.delete(i);
                adapter.notifyDataSetChanged();
                Toast.makeText(getApplicationContext(), "i = " + i +"\n l = "+l, Toast.LENGTH_SHORT).show();
            }
        });



    }

    private class ApiSimulator extends AsyncTask<Void, Void, List<CalendarDay>> {

        String[] Time_Result;

        ApiSimulator(String[] Time_Result){
            this.Time_Result = Time_Result;
        }

        @Override
        protected List<CalendarDay> doInBackground(@NonNull Void... voids) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Calendar calendar = Calendar.getInstance();
            ArrayList<CalendarDay> dates = new ArrayList<>();

            /*특정날짜 달력에 점표시해주는곳*/
            /*월은 0이 1월 년,일은 그대로*/
            //string 문자열인 Time_Result 을 받아와서 ,를 기준으로짜르고 string을 int 로 변환
            for(int i = 0 ; i < Time_Result.length ; i ++){
                CalendarDay day = CalendarDay.from(calendar);
                String[] time = Time_Result[i].split(",");
                int year = Integer.parseInt(time[0]);
                int month = Integer.parseInt(time[1]);
                int dayy = Integer.parseInt(time[2]);

                dates.add(day);
                calendar.set(year,month-1,dayy);
            }



            return dates;
        }

        @Override
        protected void onPostExecute(@NonNull List<CalendarDay> calendarDays) {
            super.onPostExecute(calendarDays);

            if (isFinishing()) {
                return;
            }

            materialCalendarView.addDecorator(new EventDecorator(Color.GREEN, calendarDays,CalendarManager.this));
        }
    }
}