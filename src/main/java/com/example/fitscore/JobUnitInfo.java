package com.example.fitscore;

import java.io.Serializable;

public class JobUnitInfo implements Serializable {
    static int CAPACITY = 48;
    int uid;
    int model;
    double COT(double clock){return clock * CAPACITY;}
}
