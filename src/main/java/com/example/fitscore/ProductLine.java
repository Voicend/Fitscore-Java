package com.example.fitscore;

import javax.crypto.Mac;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProductLine extends ArrayList<ArrayList<Machine>>{
    //一个道序的设备集合
    //N个道序组成产线
    //唯一存放machines信息的地方
    ArrayList<Machine>machines = new ArrayList<>();
    ArrayList<Task>tasks = new ArrayList<>();
    Map<String, Machine>indexrByFullname = new HashMap<>();
    void index(Machine m){
        if(m._index < 0){
            m._index = machines.size();
            machines.add(m);
            ArrayList<JobUnit>inputBuffer = new ArrayList<>();
            ArrayList<JobUnit>outputBuffer = new ArrayList<>();
            ArrayList<JobUnit>producing = new ArrayList<>();
            while(!m.inputBuffer.isEmpty()) {
                JobUnit jobTemp = m.inputBuffer.peek();
                m.inputBuffer.poll();
                inputBuffer.add(jobTemp);
            }
            for(JobUnit jobtemp : inputBuffer){
                jobtemp.machineid = m._index;
                m.inputBuffer.add(jobtemp);
            }
            while(!m.outputBuffer.isEmpty()) {
                JobUnit jobTemp = m.outputBuffer.peek();
                m.outputBuffer.poll();
                outputBuffer.add(jobTemp);
            }
            for(JobUnit jobtemp : outputBuffer){
                jobtemp.machineid = m._index;
                m.outputBuffer.add(jobtemp);
            }
            while(!m.model.isEmpty()){
                JobUnit jobTemp = m.model.peek();
                m.model.poll();
                producing.add(jobTemp);
            }
            for(JobUnit jobtemp : producing){
                jobtemp.machineid = m._index;
                m.model.add(jobtemp);
            }
        }
        else machines.set(m._index,m);
        indexrByFullname.put(m.fullname(),m);
    }
    Machine get(String fullname){
        if(indexrByFullname.containsKey(fullname))return indexrByFullname.get(fullname);
        return null;
    }
    void snapshot(Map<Integer, MachineRuntimeInfo> clone){
        for(ArrayList<Machine> p : this){
            for(Machine m : p)clone.put(m._index, m);
        }
    }
    void restore(Map<Integer, MachineRuntimeInfo> clone){
        for (ArrayList<Machine> pProcess : this) {
            for (int j = 0; j < pProcess.size(); j++) {
                pProcess.set(j, (Machine) clone.get(pProcess.get(j)._index));
            }
        }
    }
}
