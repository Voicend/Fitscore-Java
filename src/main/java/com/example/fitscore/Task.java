package com.example.fitscore;

public class Task {
    int count;
    int IOD;//index of day
    String date;
    int deadline = Integer.MAX_VALUE;
    JobUnitInfo job;
    Task(int i, int model, int count, String date){
        this.IOD = i;
        this.date = date;
        this.count = count;
        JobUnit jobUnit = new JobUnit();
        jobUnit.uid = 0;
        jobUnit.model = model;
        this.job = jobUnit;
    }
    Task(int model, int count){
        JobUnitInfo j = new JobUnit();
        j.model = model;
        this.job = j;
        this.count = count;
    }
}
