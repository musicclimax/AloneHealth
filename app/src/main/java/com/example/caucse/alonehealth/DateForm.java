package com.example.caucse.alonehealth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateForm {

    public SimpleDateFormat df_date;
    public SimpleDateFormat df_day;
    String date_str;
    String day_str;
    public DateForm()
    {
        df_date = new SimpleDateFormat("yyyy-MM-dd");
        df_day = new SimpleDateFormat("E", Locale.KOREAN);
    }


}
