package com.example.fitscore;

import javafx.util.Pair;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@SpringBootApplication
public class FitsCoreApplication {
    public static void main(String[] args) {
        Globalvar.loadgShiftMatrix(Config.COMatrixfilename);
        Globalvar.loadModelsManager(Config.ModelProcessMatrixfilename);
        Globalvar.loadClockMatrix(Config.MachineClockMatrixfilename);
        Globalvar.loadProductLineMatrix(Config.ProductlineMatrixfilename);
        Globalvar.loadgContext(Config.settingsfilename);
        Globalvar.loadjobs();
        String inputfile = "data/input.csv";
        long startTime = System.currentTimeMillis();
        Globalvar.parseRequirement(inputfile);
        /*变量存现在任务的每一种零件，最早属于第几天,
		用于判断空闲机器为了加速其他零件，需要对目前零件能够按时交付进行判断，
		该变量是为了防止重复遍历jop*/
        Map<Integer/*modelId*/, Map<Integer/*Process*/, Integer/*minIndex*/>> minDayIndexEachModel = new HashMap<>();
        Globalvar.refresh(0,minDayIndexEachModel);
        /*
		用于存放触发重新规划或者空闲机器的index,
		一次触发只会重新规划一台机器，但是若该机器重新规划结果为不发生换型，
		则在集合中继续寻找下一个机器，直到找到需要换型的机器
		*/
        Simulator simulator = new Simulator(Globalvar.gproductLine);
        ArrayList<Integer>triggerIndexVec = new ArrayList<>();
        Map<Integer, Integer>config = new HashMap<>();
        Globalvar.gproductLine.setCapacitySurplusMachineOffLine();
        while(!Globalvar.gTasks.isEmpty()){
            triggerIndexVec = simulator.triggerIndexVex;
            Pair<Integer, Integer> COInfo = Globalvar.getMostSuitableCOMachineInMachines2(triggerIndexVec,(int)simulator.realTime,minDayIndexEachModel);
            //test;
            if(COInfo.getKey()!=-1){
                config = new HashMap<>();
                config.put(COInfo.getKey(),COInfo.getValue());
                Globalvar.gproductLine.machines.get(COInfo.getKey()).setstate(MachineRuntimeInfo.MachineState.S_ONLINE);
            }
            simulator.triggerIndexVex = new ArrayList<>();
            /*更新任务并开始仿真*/
            simulator.update(Globalvar.jobs);
            int ok = simulator.setup(config).simulate();
            if(simulator.realTime - simulator.lastJobReleaseTime > Globalvar.endlessLoopWaitTime){
                //log
                System.out.println("jump out endless loop\n JOP:\n");
                for(Map.Entry<Integer,Map<Integer,Map<Integer,JobUnit>>> e1 : Globalvar.jop.entrySet()){
                    for(Map.Entry<Integer,Map<Integer,JobUnit>> e2 : e1.getValue().entrySet()){
                        //log
                        System.out.printf("\tmodel: %d, process: %d, count: %d\n", e1.getKey(), e2.getKey(), e2.getValue().size());
                    }
                }
                return;
            }
            Globalvar.jobs = simulator.jobs;
            Globalvar.jop = simulator.Jop;
            triggerIndexVec = simulator.triggerIndexVex;
            //触发重新枚举前，对每一个机器的状态信息，jobs状态信息和需要传入逐秒仿真的信息进行更新
            Globalvar.refresh((int)simulator.realTime,minDayIndexEachModel);
        }
        //write into csvfile
        long endTime = System.currentTimeMillis();
        System.out.printf("all passed!    total: %d sec",(endTime - startTime)/1000);
    }
}
