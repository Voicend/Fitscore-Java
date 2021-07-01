package com.example.fitscore;

public class JobUnit extends JobUnitInfo{
    enum State {
        JUS_UNSET,
        JUS_WAITING,	//在进料道
        JUS_PROCESSING,		//在加工位
        JUS_COMPLETED,		//在出料道
        JUS_TRANSITING		//运输中
    }
    State state = State.JUS_UNSET;
    int ioo = -1;
    int process = -1;
    double releaseTime = 0;
    int machineid = -1;
    int daysIndex = 0;
    String taskinfodate;
}
