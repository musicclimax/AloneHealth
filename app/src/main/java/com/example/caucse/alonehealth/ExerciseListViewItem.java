package com.example.caucse.alonehealth;

import android.graphics.drawable.Drawable;

public class ExerciseListViewItem {
    private Drawable gifDrawable;
    private String exerciseName;
    private String numberOfSets;

    public void setGifDrawable(Drawable gif){
        gifDrawable = gif;
    }
    public void setExerciseName(String name){
        this.exerciseName = name;
    }
    public void setNumberOfSets(String number){
        this.numberOfSets = number;
    }

    public Drawable getGifDrawable(){return this.gifDrawable;}
    public String getExerciseName(){return this.exerciseName;}
    public String getNumberOfSets(){return this.numberOfSets;}
}
