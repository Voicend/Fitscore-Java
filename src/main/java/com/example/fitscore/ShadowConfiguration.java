package com.example.fitscore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ShadowConfiguration extends BaseConfiguration{
    Map<Integer, Integer> getConfig(){
        Map<Integer, Integer> config = new HashMap<>();
        for(Map<Integer/*零件*/, ArrayList<Machine>> e :this.values()){
            for (Map.Entry<Integer/*零件*/, ArrayList<Machine>> e2 : e.entrySet()){
                for(Machine machine : e2.getValue()){
                    config.put(machine._index, e2.getKey());
                }
            }
        }
        return config;
    }
    Map<Integer, Integer> getConfig(ProductLine pl, Map<Integer/*mpdel*/,Map<Integer/*process*/, Map<Integer/*job id*/, JobUnit>>> jobs){
        //统计每种型号进展到哪个道序
        Map<Integer/*model*/, Integer/*process*/> left = new HashMap<>();
        for(int pid:jobs.keySet()){
            int i = 0;
            while(i < 10 && jobs.get(pid).get(i).isEmpty())i++;
            left.put(pid, i);
            System.out.printf("-- mode %s(%d) left side(process): %d\n", Globalvar.gmodels.get(pid).name,pid,i);
        }
        //
        Map<Integer, Integer>config = new HashMap<>();
        for(Map.Entry<Integer/*道序*/,Map<Integer/*零件*/, ArrayList<Machine>>> e1 : this.entrySet()){
            ArrayList<Machine> machinestmp = pl.get(e1.getKey());
            for(Map.Entry<Integer, ArrayList<Machine>> e2 : e1.getValue().entrySet()){
                boolean isModelInProcess = false;
                for(Machine machine : pl.get(e1.getKey())){
                    isModelInProcess = machine.mode == e2.getKey();
                    if(isModelInProcess) break;
                }
                for(Machine machine : e2.getValue()){
                    boolean isOnlyMachineinProcess = false;
                    int modeBefore = pl.machines.get(machine._index).mode;
                    int process = machine.process;
                    int MachineCountForOriginalModel = 0;
                    //判断机器是否是该道序唯一生产该零件的机器
                    for(Machine machinetmp : machinestmp){
                        for(Map.Entry<Integer, Integer> e : config.entrySet()){
                            if(machinetmp._index == e.getKey())
                                machinetmp.mode = e.getValue();
                        }
                    }
                    for(Machine machineTest : machinestmp){
                        if(machineTest.mode == modeBefore)
                            MachineCountForOriginalModel ++;
                    }
                    if(MachineCountForOriginalModel <= 1)
                        isOnlyMachineinProcess = true;
                    System.out.println("process:"+machine.process+"machine.mode:"+modeBefore+"machine.index:"+machine._index
                    +"MachineCountForOriginalModel"+MachineCountForOriginalModel);
                    if(!machine.avilable())continue;
                    else if (modeBefore == machine.mode) continue;
                    else if (process > left.get(modeBefore) && left.get(modeBefore)!=10
                    && isOnlyMachineinProcess && modeBefore != -1){
                        System.out.printf("config denined: machine %s @ process: %d C/O left[%d(%s)] = %d, from mode = %d -> %d\n",
                                machine.fullname(),process,modeBefore, Globalvar.gmodels.get(modeBefore).name,left.get(modeBefore),modeBefore,e2.getKey());
                        continue;
                    }
                    config.put(machine._index, e2.getKey());
                }
            }
        }
        return config;
    }
}

