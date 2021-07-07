package com.example.fitscore;

public class JobUnitInfo {
    static int CAPACITY = 48;
    int uid = 0;
    int model;
    double COT(double clock){return clock * CAPACITY;}
}
