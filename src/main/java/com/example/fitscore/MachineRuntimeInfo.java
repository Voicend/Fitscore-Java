package com.example.fitscore;

public class MachineRuntimeInfo {
    enum MachineStatus {MS_IDLE, MS_WORKING, MS_CO, MS_BLOCKING, MS_UNSET}//0空闲态，1生产状态，2换型态，3 blocking,4 Unset
    enum ChangeOverStatus{COS_KEEP,COS_GOING}
    enum MachineState {S_ONLINE,S_OFFLINE,S_PAUSE}
    double toIdle = 0;//预计多少秒后空闲
    int mode = -1;
    int COTarget = -1;//仅当需要换型时有效
    ChangeOverStatus changeStatus = ChangeOverStatus.COS_KEEP;//记录机器是否准备换型
    MachineStatus status = MachineStatus.MS_IDLE;//机器状态
    MachineState state;
    boolean avilable(){
        return !(status == MachineStatus.MS_CO);
    }
    void setCOStatus(ChangeOverStatus coStatus){changeStatus = coStatus;}
    ChangeOverStatus getCOStatus(){return changeStatus;}
    boolean acceptable(JobUnit job){
        if(getCOStatus()==ChangeOverStatus.COS_GOING)return COTarget == job.model;
        else if(status == MachineStatus.MS_CO)return COTarget == job.model;
        else return mode == job.model;
    }
    MachineState getstate(){return state;}
}
