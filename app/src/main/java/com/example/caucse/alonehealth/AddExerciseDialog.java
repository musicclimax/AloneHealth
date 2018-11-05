package com.example.caucse.alonehealth;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class AddExerciseDialog extends Dialog {
    ExerciseListViewAdapter exerciseListViewAdapter;
    boolean firstDragFlag = true;
    boolean dragFlag = false;
    float startYPosition = 0;
    float endYPosition = 0;
    int currentPosition = 0;
    ListView dialogListView;
    Button dialogAddButton;
    Button dialogCancelButton;
    EditText setEditText;
    EditText numberEditText;
    Date currentDate;
    Date selectedDate;
    public AddExerciseDialog(@NonNull Context context,ExerciseListViewAdapter adapter) {
        super(context);
        this.exerciseListViewAdapter = adapter;
    }

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //타이틀바 제거
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //다이얼로그의 배경을 투명 처리
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setContentView(R.layout.dialog_exerciselist);
        //운동 목록 불어오기
        DialogExerciseListViewAdapter dialogExerciseListViewAdapter = new DialogExerciseListViewAdapter();
        dialogListView =(ListView)findViewById(R.id.alertExerciselist);
        dialogListView.setVerticalScrollBarEnabled(false);
        dialogListView.setAdapter(dialogExerciseListViewAdapter);
        ArrayList<String> result = SQLiteManager.sqLiteManager.selectAllExerciseName();
        dialogExerciseListViewAdapter.addItem(result);

        //스크롤 이벤트 처리
        dialogListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_MOVE:
                        dragFlag = true;
                        if(firstDragFlag){
                            startYPosition = motionEvent.getY();
                            firstDragFlag = false;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        endYPosition = motionEvent.getY();
                        firstDragFlag = true;

                        if(dragFlag){
                            if((startYPosition != endYPosition) && (((startYPosition - endYPosition) > 10) || ((endYPosition - startYPosition) > 10))){
                                currentPosition = dialogListView.getFirstVisiblePosition();
                                dialogListView.smoothScrollToPosition(currentPosition, 0);
                            }
                        }
                        startYPosition = 0.0f;
                        endYPosition = 0.0f;
                        dragFlag = false;
                        break;
                }
                return false;
            }
        });

        //버튼 이벤트 리스너
        setEditText = (EditText)findViewById(R.id.alertSet);
        numberEditText = (EditText)findViewById(R.id.alertNumber);
        dialogAddButton = (Button)findViewById(R.id.alertAddButton);
        dialogAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //ScheduleData 변수
                String id;
                String date;
                String Exercise_id;
                String Exercise_name;
                int set;
                int number;

                long now = System.currentTimeMillis();
                if(selectedDate != null){
                    if((!selectedDate.before(currentDate) || selectedDate.getDate() == currentDate.getDate())){
                       if(setEditText.getText().toString().length()!= 0){
                           if(numberEditText.getText().toString().length() != 0){
                               Exercise_name = (String) dialogListView.getItemAtPosition(currentPosition);
                               Exercise_id = SQLiteManager.sqLiteManager.selectExerciseIdFromName(Exercise_name);
                               if(Exercise_id != null) {
                                   set = Integer.parseInt(setEditText.getText().toString());
                                   number = Integer.parseInt(numberEditText.getText().toString());
                                   SimpleDateFormat sdf = new SimpleDateFormat("yyyy. MM. dd");
                                   date = sdf.format(selectedDate);
                                   sdf = new SimpleDateFormat("hhmmss");
                                   currentDate = new Date(System.currentTimeMillis());
                                   id = new String(date.toString() + sdf.format(currentDate));

                                   if(SQLiteManager.sqLiteManager.insertScheduleData(new ScheduleData(id,date,Exercise_id,set,number,0))) {
                                       Toast.makeText(getContext(), "잘됨 무튼 잘됨.", Toast.LENGTH_SHORT).show();
                                       exerciseListViewAdapter.setListViewItemList(SQLiteManager.sqLiteManager.selectScheduleFromDate(date));
                                       exerciseListViewAdapter.notifyDataSetChanged();
                                   }
                                   else
                                       Toast.makeText(getContext(),"DB 삽입 잘못됨.", Toast.LENGTH_SHORT).show();
                               }
                               else
                                   Toast.makeText(getContext(),"운동 id 잘못됨 잘못됨.", Toast.LENGTH_SHORT).show();
                           }
                           else
                               Toast.makeText(getContext(),"운동의 횟수가 입력되지 않았습니다.", Toast.LENGTH_SHORT).show();
                       }
                       else
                           Toast.makeText(getContext(),"운동의 세트 수가 입력되지 않았습니다.", Toast.LENGTH_SHORT).show();
                    }
                    else
                        Toast.makeText(getContext(),"이미 지난 날짜 입니다.", Toast.LENGTH_SHORT).show();
                }

                dismiss();
            }
        });
        dialogCancelButton = (Button)findViewById(R.id.alertCancelButton);
        dialogCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

    }

    public void setSelectedDate(Date date){this.selectedDate = date;}
}
