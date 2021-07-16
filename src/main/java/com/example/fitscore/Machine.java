package com.example.fitscore;
import javafx.util.Pair;

import javax.crypto.Mac;
import java.io.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class Machine extends MachineRuntimeInfo implements Serializable{
    int process = 0;
    int _index = -1;//global index
    String number, name;
    Pair<Integer, Integer> bufferSize;
    boolean generic = false;
    Map<Integer, Double> clocks;
    Queue<JobUnit> inputBuffer = new ArrayDeque<>();
    Queue<JobUnit> outputBuffer = new ArrayDeque<>();
    Queue<JobUnit> model = new ArrayDeque<>();
    String fullname(){return name;}
    Map<Integer, Map<Integer, Double>> calculatedTickMatrix = new HashMap<>();
    double clock(int modelId){//获取该机器生产modelId的节拍
        if(!calculatedTickMatrix.containsKey(_index))
            calculatedTickMatrix.put(_index,new HashMap<>());
        if(calculatedTickMatrix.get(_index).containsKey(modelId)){
            double value = calculatedTickMatrix.get(_index).get(modelId);
            if(value > 0)return value;
        }
        if(clocks.containsKey(modelId)) {
            calculatedTickMatrix.get(_index).put(modelId,clocks.get(modelId));
            return clocks.get(modelId);
        }
        return -1;
    }
    boolean support(int model){return generic||(clock(model)>=0);}//机器是否能够生产model型号
    boolean acceptable(JobUnit job){
        if(inputBuffer.size() >= COB())return false;
        else if (super.acceptable(job))return true;
        else return generic;
    }
    int COB(){return bufferSize.getKey();}//capacity of buffer
    //开始加工
    int workOn(JobUnit job, int now){
        status = MachineStatus.MS_WORKING;
        toIdle = now + job.COT(clock(job.model));
        model.offer(job);
        inputBuffer.poll();
        return 0;
    }
    //执行换型
    int changeOver(int cost, int now){
        status = MachineStatus.MS_CO;
        setCOStatus(0);
        toIdle = now + cost;
        return 0;
    }
    //加工完成进入料道
    int finishJob(JobUnit job, int now){
        job.releaseTime = now;
        outputBuffer.add(job);
        model.poll();
        toIdle = now;
        status = MachineStatus.MS_IDLE;
        return 0;
    }
    //判断是否停机
    boolean isMachineOnLine(){return(getstate() != MachineState.S_OFFLINE && getstate() != MachineState.S_PAUSE);}
    void setstate(MachineState state){
        this.state = state;
    }
}
