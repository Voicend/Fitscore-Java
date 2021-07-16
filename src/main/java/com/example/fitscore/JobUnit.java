package com.example.fitscore;

import java.io.Serializable;

public class JobUnit extends JobUnitInfo implements Serializable {
    enum State {
        JUS_UNSET,
        JUS_WAITING,	//在进料道
        JUS_PROCESSING,		//在加工位
        JUS_COMPLETED,		//在出料道
        JUS_TRANSITING		//运输中
    }
    State state = State.JUS_UNSET;
    int ioo = -1;//index of process
    int process = -1;
    double releaseTime = 0;
    int machineid = -1;
    int daysIndex = 0;
    String taskinfodate;
    JobUnit(){}
    JobUnit(JobUnit jobUnit){
        this.process = jobUnit.process;
        this.machineid = jobUnit.machineid;
        this.model = jobUnit.model;
        this.uid = jobUnit.uid;
        this.ioo = jobUnit.ioo;
        this.state = jobUnit.state;
        this.daysIndex = jobUnit.daysIndex;
        this.taskinfodate = jobUnit.taskinfodate;
        this.releaseTime = jobUnit.releaseTime;
    }
}
