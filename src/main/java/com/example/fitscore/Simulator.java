package com.example.fitscore;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import javafx.util.Pair;

import java.util.*;
import java.math.*;

public class Simulator extends BaseSimulator{
    int triggerIndex = -1;
    ArrayList<Integer>triggerIndexVex = new ArrayList<>();
    Map<Integer/*model*/,Map<Integer/*Process*/, Map<Integer/*jobid*/, JobUnit>>>Jop = new HashMap<>();
    double realTime = 0;
    double lastJobReleaseTime = 0;
    void log(Machine m, JobUnit j){
        String s = m.process+","+m._index+","+m.name+"_"+m.number+","+Globalvar.gmodels.get(j.model).name+","
                +realTime+","+(int)j.COT(m.clock(j.model))+","+j.taskinfodate+","+j.uid+","+(int)realTime/86400;
        System.out.println(s);
    }
    Simulator(ProductLine pl) {
        super(pl);
    }
    boolean moveJobToNextProcess(Map<Integer/*model*/,Map<Integer/*Process*/, Map<Integer/*jobid*/, JobUnit>>>Jop,
                                 JobUnit j, Machine fromMachine, Machine toMachine, boolean finalProcess){
        if(fromMachine != null && !fromMachine.outputBuffer.isEmpty())
            fromMachine.outputBuffer.poll();
        int from = j.process;
        j.machineid = toMachine != null?toMachine._index : -1;
        if(!finalProcess && toMachine != null){
            j.ioo += 1;
            j.process = Globalvar.gmodels.get(j.model).processes.get(j.ioo);
            toMachine.inputBuffer.add(j);
            if(!Jop.containsKey(j.model))
                Jop.put(j.model,new HashMap<>());
            if(!Jop.get(j.model).containsKey(j.process))
                Jop.get(j.model).put(j.process, new HashMap<>());
            Jop.get(j.model).get(j.process).put(j.uid,j);
        }
        Jop.get(j.model).get(from).remove(j.uid);
//System.out.println("finish"+j.uid+" at "+this.realTime);
        if(Jop.get(j.model).containsKey(-1)&&Jop.get(j.model).get(-1).isEmpty()){
            for (int i = 0; i < from; i ++)
                if(! Jop.get(j.model).get(i).isEmpty())return false;
            if(fromMachine != null && fromMachine.model.isEmpty()//生产位空
            && fromMachine.outputBuffer.isEmpty()//出料口空
            && (fromMachine.inputBuffer.isEmpty() || fromMachine.inputBuffer.peek().model != fromMachine.mode)//还有未生产的
            ){
                triggerIndex = fromMachine._index;
                fromMachine.toIdle += Globalvar.offset;
                triggerIndexVex.add(triggerIndex);
                return true;
            }
        }
        return false;
    }
    void log_on_machine_status_change(Machine m){
        if(Globalvar.isProcessDataShown == 1){
            String s = realTime + "," + m._index+ "," +m.name+ "-" +m.number+ "," +
                    m.process+ "," +m.status+ "," +m.inputBuffer.size()+ "," +m.outputBuffer.size()
                    + "," +Globalvar.gmodels.get(m.mode).name+"\n";
            System.out.print(s);
        }
        if(m.status == MachineRuntimeInfo.MachineStatus.MS_CO){
            int deviatedRealTime = 0;
            if (realTime - m.toIdle < Globalvar.offset) {
                deviatedRealTime = (int)realTime + Globalvar.offset;
                m.toIdle += Globalvar.offset;
            }
            else {
                deviatedRealTime = (int)realTime;
            }
        }
    }
    int simulate(){
        ShiftBootManager psbm = ShiftBootManager.getInstance();
        boolean working = true;
        int count = 0;
        while(working){
            if(!psbm.isEmpty()){
                ShiftBookItem sbi = psbm.poll();
                Machine pm = ppl.get(sbi.target);
                double extraTimeCost = 0;
                if(pm != null){
                    double timeCostPerUnit = pm.clock(pm.mode)*JobUnit.CAPACITY;
                    extraTimeCost += timeCostPerUnit *(pm.inputBuffer.size()+1);
                }
                if(realTime+extraTimeCost >= sbi.beginTime){
                    switch (sbi.type){
                        case SBI_OFF:{
                            if(pm != null)
                                pm.setstate(MachineRuntimeInfo.MachineState.S_OFFLINE);
                        }break;
                        case SBI_ON:{
                            if(pm != null)
                                pm.setstate(MachineRuntimeInfo.MachineState.S_ONLINE);
                        }break;
                    }
                    break;
                }
            }
            //从最后道序开始往前遍历
            for(int i = ppl.size() - 1; i >= 0; i --){
                ArrayList<Machine> process = ppl.get(i);
                for(Machine m : process){
                    //如果机器为第一道序则在机器前buffer不满的情况下补满buffer
                    if(m.mode >= 0 && m.process == Globalvar.gmodels.get(m.mode).processes.get(0)
                    && m.getstate() == MachineRuntimeInfo.MachineState.S_ONLINE){
                        if(!Jop.get(m.mode).containsKey(-1))
                            Jop.get(m.mode).put(-1,new HashMap<>());
                        if(m.inputBuffer.size() < m.COB() && Jop.get(m.mode).get(-1).size()>0){
                            for(Map.Entry<Integer, JobUnit> e1 : Jop.get(m.mode).get(-1).entrySet()){
                                if(!m.acceptable(e1.getValue()))break;
                                working = !moveJobToNextProcess(Jop, e1.getValue(),null, m, false);
                                break;
                            }
                        }
                    }
                    /*
                    当时间等于预计的机器空闲时间时查看：零件能否放到后料道，可以的话状态为0，由下一个判断语句执行
                    不可以的话状态改为3，视为阻塞
                     */
                    if(m.status == MachineRuntimeInfo.MachineStatus.MS_WORKING){
                        if(realTime >= m.toIdle){
                            JobUnit e = m.model.peek();
                            if(m.outputBuffer.size() < m.COB()){
                                assert e != null;
                                m.finishJob(e, (int)realTime);
                                log(m,e);
                                log_on_machine_status_change(m);
                                lastJobReleaseTime = realTime;
                            }
                            else {
                                m.status = MachineRuntimeInfo.MachineStatus.MS_BLOCKING;
                                log_on_machine_status_change(m);
                            }
                        }
                    }
                    else if(m.status == MachineRuntimeInfo.MachineStatus.MS_CO){
                        if(m.toIdle <= realTime){
                            m.status = MachineRuntimeInfo.MachineStatus.MS_IDLE;
//System.out.println(m.fullname()+" C/0 DONE from " + m.mode + "->" + m.COTarget + " width input: "+m.inputBuffer.size());
                            m.mode = m.COTarget;
                            m.COTarget = -1;
                            log_on_machine_status_change(m);
                        }
                    }
                    else if(m.status == MachineRuntimeInfo.MachineStatus.MS_BLOCKING){
                        if(m.outputBuffer.size() < m.COB()){
                            JobUnit e = m.model.peek();
                            if(!Globalvar.workAndWaitTime.containsKey(m._index))
                                Globalvar.workAndWaitTime.put(m._index,new Pair<Double,Double>(0.0,0.0));
                            Pair<Double,Double> pair = Globalvar.workAndWaitTime.get(m._index);
                            Globalvar.workAndWaitTime.put(m._index,
                                    new Pair<>(pair.getKey(),pair.getValue()+(realTime>m.toIdle?realTime-m.toIdle:0)));
                            //
                            assert e != null;
                            m.finishJob(e, (int)realTime);
//                            log(m,e);
                            log_on_machine_status_change(m);
                        }
                    }
                    else if(m.status == MachineRuntimeInfo.MachineStatus.MS_IDLE){
                        if(m.getCOStatus() == 1){
                            if(!m.inputBuffer.isEmpty()){
                                JobUnit e = m.inputBuffer.peek();
                                if(e.model == m.COTarget){
                                    int x = ShiftMatrix.id2index.get(m.mode);
                                    int y = ShiftMatrix.id2index.get(m.COTarget);
                                    Pair<Double, Integer> pair = Globalvar.coTimeAndCount.get(m._index);
                                    Globalvar.coTimeAndCount.put(m._index,
                                            new Pair<>(pair.getKey()+Globalvar.gShiftMatrixs.get(m.name).matrix[x][y]*60,
                                            pair.getValue()+1));
                                    int outputTime = (int)realTime;
                                    if(realTime - m.toIdle < Globalvar.offset)
                                        outputTime = (int)realTime + Globalvar.offset;
                                    //log
                                    //startCO
                                    m.changeOver((int)Globalvar.gShiftMatrixs.get(m.name).matrix[x][y]*60,(int)realTime);
                                    log_on_machine_status_change(m);
                                }
                                else if(e.model == m.mode || m.generic){
                                    //count waitTime
                                    Pair<Double, Double>pair = Globalvar.workAndWaitTime.get(m._index);
                                    double costTime = e.COT(m.clock(e.model));
                                    Globalvar.workAndWaitTime.put(m._index,
                                            new Pair<>(pair.getKey()+costTime,
                                            pair.getValue()+realTime>m.toIdle?(realTime-m.toIdle):0));
                                    if(realTime > m.toIdle){
                                        //waitlog
                                    }
                                    m.workOn(e,(int)realTime);
                                    log_on_machine_status_change(m);
                                }
                            }
                            else{
                                //count waitTime
                                if(!Globalvar.workAndWaitTime.containsKey(m._index))
                                    Globalvar.workAndWaitTime.put(m._index,new Pair<>(0.0,0.0));
                                Pair<Double, Double>pair = Globalvar.workAndWaitTime.get(m._index);
                                Globalvar.workAndWaitTime.put(m._index,
                                        new Pair<>(pair.getKey(),
                                                pair.getValue()+realTime>m.toIdle?(realTime-m.toIdle):0));
                                if(realTime > m.toIdle){
                                    //waitlog
                                }
                                int x = ShiftMatrix.id2index.get(m.mode);
                                int y = ShiftMatrix.id2index.get(m.COTarget);
                                Pair<Double,Integer>pair1 = Globalvar.coTimeAndCount.containsKey(m._index)?Globalvar.coTimeAndCount.get(m._index):new Pair<>(0.0,0);
                                Globalvar.coTimeAndCount.put(m._index,
                                        new Pair<>(pair1.getKey()+Globalvar.gShiftMatrixs.get(m.name).matrix[x][y]*60,
                                                pair1.getValue()+1));
                                int outputTime = (int)realTime;
                                if(realTime-m.toIdle<Globalvar.offset)
                                    outputTime = (int)realTime + Globalvar.offset;
                                //log
                                m.changeOver((int)Globalvar.gShiftMatrixs.get(m.name).matrix[x][y]*60,(int)realTime);
                                log_on_machine_status_change(m);
                                //log
                            }
                        }
                        else if(!m.inputBuffer.isEmpty()){
                            JobUnit e = m.inputBuffer.peek();
                            if(e.model == m.mode || m.generic){
                                //count workTime and waitTime
                                Pair<Double,Double> pair = Globalvar.workAndWaitTime.get(m._index);
                                double costTime = e.COT(m.clock(e.model));
                                double key, value;
                                if(pair == null){
                                    key = 0;
                                    value = 0;
                                }else{
                                    key = pair.getKey();
                                    value = pair.getValue();
                                }
                                Globalvar.workAndWaitTime.put(m._index,
                                        new Pair<>(key+costTime,
                                                value+(realTime > m.toIdle ? realTime - m.toIdle : 0)));
                                //log
                                log_on_machine_status_change(m);
                                m.workOn(e, (int)realTime);
                            }
                        }
                    }
                    //如果机器的后料道不为空，就尝试向下一道序运送零件，如果为最后一道序，则直接pop出来，如果无法向下运则不运
                    if(!m.outputBuffer.isEmpty()){
                        JobUnit e = m.outputBuffer.peek();
                        //获取该道序后道序存在该零件的机器
                        ArrayList<Machine> macs = new ArrayList<>();
                        int nextProcess = m.process + 1;
                        if(ppl.isGenericProcess(nextProcess)){
                            for(Machine p : ppl.machines){
                                if(p.process == m.process
                                &&!p.outputBuffer.isEmpty())
                                    macs.add(p);
                            }
                        }
                        else{
                            for(Machine p : ppl.machines){
                                if(p.process == m.process
                                && !p.outputBuffer.isEmpty()
                                && p.outputBuffer.peek().model == e.model)
                                    macs.add(p);
                            }
                        }
                        //从macs获取零件时间最早的零件往下传
                        //return l->outputBuffer.front().releaseTime < r->outputBuffer.front().releaseTime
                        int min = 0;
                        for(int idx = 1; idx < macs.size(); idx++){
                            if(macs.get(idx).outputBuffer.peek().releaseTime<macs.get(min).outputBuffer.peek().releaseTime)
                                min = idx;
                        }
                        Machine macBest = macs.get(min);
                        JobUnit e1 = macBest.outputBuffer.peek();
                        Models mi = Globalvar.gmodels.get(e1.model);
                        int FINAL = mi.processes.get(mi.processes.size()-1);
                        if(macBest.process == FINAL){
                            working = !moveJobToNextProcess(Jop, e1, macBest, null,true);
                            continue;
                        }
                        int COUNT = (int)macBest.outputBuffer.size();
                        int tries = 0;
                        Queue<JobUnit> push_back = new ArrayDeque<>();
                        while(tries < COUNT){
                            JobUnit e2 = macBest.outputBuffer.peek();
                            //如果机器不是最后一道序查看下一道序是否是通用道序并且不是最后一道序
                            int next = mi.processes.get(e2.ioo+1);
                            {
                                ArrayList<Machine>macs2 = new ArrayList<>();
                                ProductLine ppltmp = (ProductLine)ppl.clone();
                                for(Machine mac : ppltmp.get(next)){
                                    if(mac.acceptable(e2)/*exclude offLineMachine*/&&
                                    mac.getstate()!= MachineRuntimeInfo.MachineState.S_OFFLINE&&
                                    mac.getstate()!= MachineRuntimeInfo.MachineState.S_PAUSE)
                                        macs2.add(mac);
                                }
                                //选出最早空闲的机器，把零件传下去
                                Machine minreleasemac = new Machine();
                                double minreleasetime = Double.MAX_VALUE;
                                for(Machine machine:macs2){
                                    double machinereleasetime = machine.toIdle;
                                    if(machine.getCOStatus()== 1
                                    ||machine.status == MachineRuntimeInfo.MachineStatus.MS_CO)
                                        machinereleasetime += Globalvar.gShiftMatrixs.get(machine.name).matrix
                                                [ShiftMatrix.id2index.get(machine.mode)]
                                                [ShiftMatrix.id2index.get(machine.COTarget)]*60;
                                    if(machinereleasetime < minreleasetime){
                                        minreleasetime = machinereleasetime;
                                        minreleasemac = machine;
                                    }
                                }
                                if(!macs2.isEmpty()){
                                    working = !moveJobToNextProcess(Jop,e2,macBest,minreleasemac,false);
                                }
                                else if (macBest.generic){
                                    push_back.add(e2);
                                    macBest.outputBuffer.poll();
                                }
                            }
                            if(!macBest.generic)break;
                            tries += 1;
                        }//end of while: if generic machine, check every job in output queue until one match or all failed
                        if(!push_back.isEmpty()){
                            while(!push_back.isEmpty()){
                                macBest.outputBuffer.add(push_back.poll());
                            }
                        }
                        if(tries == COUNT){
                            //log
                        }
                    }
                    if(!working)
                        break;
                }//end of machines in process loop
                if(!working)
                    break;
            }//end of processes loop
            realTime++;
            count++;
            int initialCount = Globalvar.checkTime;
            if(initialCount==count){
                int intervalTime = Globalvar.offset;
                for(Machine machine : ppl.machines){
                    if((realTime - Math.abs(machine.toIdle)>intervalTime)
                        &&machine.status == MachineRuntimeInfo.MachineStatus.MS_IDLE
                        &&machine.getstate()!= MachineRuntimeInfo.MachineState.S_OFFLINE)
                        triggerIndexVex.add(machine._index);
                }
                if(!triggerIndexVex.isEmpty()){
                    //log
                    break;
                }
                count = 0;
            }
        }
        for(Map.Entry<Integer,Map<Integer,Map<Integer,JobUnit>>>e : Jop.entrySet()){
            for(Map.Entry<Integer,Map<Integer,JobUnit>>e1:e.getValue().entrySet()){
                for(Map.Entry<Integer,JobUnit>e2:e1.getValue().entrySet()){
                    if(e2.getValue().machineid >= 0)
                        continue;
                    jobs.add(new JobUnit(e2.getValue()));
                }
            }
        }
        return 0;
    }
    Simulator setup(Map<Integer/*index of machines*/, Integer/*model*/>configs){
        for(Map.Entry<Integer,Integer>e:configs.entrySet()){
            Machine pm = ppl.machines.get(e.getKey());
            if(pm.mode < 0)pm.mode = e.getValue();
            else if(pm.getCOStatus() == 1)
            {
                //已经准备换型的暂不参与规划
                //log
                System.out.printf("machine  %d  %s will C/O from %s(%d) to %s(%d), discard to %s(%d) \n",
                        pm._index,pm.fullname(),Globalvar.gmodels.get(pm.mode),pm.mode,
                        Globalvar.gmodels.get(pm.COTarget),pm.COTarget,
                        Globalvar.gmodels.get(e.getValue()),e.getValue());
            }
            else if(pm.status!= MachineRuntimeInfo.MachineStatus.MS_CO
                    &&pm.mode != e.getValue()){
                pm.setCOStatus(1);
                pm.COTarget = e.getValue();
            }
        }
        return this;
    }
    //--
    void update(PriorityQueue<JobUnit> jobs){
        while(!jobs.isEmpty()){
            JobUnit e = jobs.poll();
            int p = e.ioo < 0 ? -1 : Globalvar.gmodels.get(e.model).processes.get(e.ioo);
            if(!Jop.containsKey(e.model))
                Jop.put(e.model,new HashMap<>());
            if(!Jop.get(e.model).containsKey(p))
                Jop.get(e.model).put(p,new HashMap<>());
            Jop.get(e.model).get(p).put(e.uid,e);
        }
    }
}
