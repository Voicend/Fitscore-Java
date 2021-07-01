package com.example.fitscore;

public class ShiftBookItem {
    String target;
    int beginTime = 0;
    int interval = 0;
    int endTime(){return beginTime + interval;}
    enum Type{SBI_UNSET,SBI_ON,SBI_OFF};
    Type type = Type.SBI_UNSET;
}
