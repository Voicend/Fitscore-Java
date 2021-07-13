package com.example.fitscore;

import javafx.util.Pair;

import javax.crypto.Mac;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProductLine extends ArrayList<ArrayList<Machine>> implements Serializable{
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
    boolean isGenericProcess(int process){
        if(process>=0 && process < this.size())
            return this.get(process).get(0).generic;
        return false;
    }
    void machineChangeMode(int index, int mode){
        for(ArrayList<Machine>machines:this){
            for(Machine machine : machines){
                if(machine._index == index)
                    machine.mode = mode;
            }
        }
    }
    double getCapacityInProcess(int modelId, int process){
        double PiecesPerSecond = 0;
        for(Machine machine: this.get(process)){
            if((machine.mode == modelId)&&machine.getstate()== MachineRuntimeInfo.MachineState.S_ONLINE)
                PiecesPerSecond += 1/(machine.clock(modelId));
        }
        return PiecesPerSecond;
    }
    double CountToFinishBeforeDeadLineInCertainProcess(int deadLine, int realTime, int modelId, int _process){
        //根据生产线产能预估在交付时间之前还能完成多少框零件
        if(deadLine < realTime)
            return -1;
        Map<Integer/*toIdle*/, Integer/*index*/>COTable = new HashMap<>();
        ArrayList<Pair<Integer/*time period*/, Double/*capacity*/>> capacityTable = new ArrayList<>();
        int processIndex = 0;
        for(ArrayList<Machine>process : this){
            if(processIndex != _process){
                processIndex++;
                continue;
            }
            for(Machine machine : process){
                if(machine.COTarget == modelId && machine.COTarget != machine.mode && machine.isMachineOnLine()){
                    int x = ShiftMatrix.id2index.get(machine.mode);
                    int y = ShiftMatrix.id2index.get(modelId);
                    if(machine.toIdle < realTime)
                        COTable.put(realTime+(int)Globalvar.gShiftMatrixs.get(machine.name).matrix[x][y]*60,machine._index);
                    else
                        COTable.put((int)machine.toIdle+(int)Globalvar.gShiftMatrixs.get(machine.name).matrix[x][y]*60,machine._index);
                }
            }
            processIndex++;
        }
        int lastTimePoint = realTime;
        ProductLine productLineTemp = this.myclone();
        double capacity = productLineTemp.getCapacityInProcess(modelId, _process);
        double finishedModels = 0;
        for(Map.Entry<Integer,Integer>e:COTable.entrySet()){
            finishedModels += (e.getKey() - lastTimePoint)*capacity;
            productLineTemp.machineChangeMode(e.getValue(),modelId);
            capacity = productLineTemp.getCapacityInProcess(modelId,_process);
            lastTimePoint = e.getKey();
        }
        finishedModels += (deadLine - lastTimePoint) * capacity;
        return finishedModels / JobUnitInfo.CAPACITY;
    }
    ArrayList<Integer> getLowestCapacityProcesses(int modelId){
        ProductLine productLineTemp = this.myclone();
        ArrayList<Integer>minCapacityProcesses = new ArrayList<>();
        double minCapacity = Double.MIN_NORMAL;
        for(int process : Globalvar.gmodels.get(modelId).processes){
            if(productLineTemp.isGenericProcess(process))
                continue;
            double PiecePerSecond = 0;
            for(Machine machine:productLineTemp.get(process)){
                if(machine.mode == modelId && machine.isMachineOnLine())
                    PiecePerSecond += 1.0/machine.clock(modelId);
            }
            if(PiecePerSecond < minCapacity)
                minCapacity = PiecePerSecond;
        }
        for(int process : Globalvar.gmodels.get(modelId).processes){
            if(productLineTemp.isGenericProcess(process))
                continue;
            double PiecesPerSecond = 0;
            for(Machine machine : productLineTemp.get(process)){
                if((machine.mode == modelId || machine.COTarget == modelId)
                    &&machine.isMachineOnLine())
                    PiecesPerSecond += 1.0/machine.clock(modelId);
            }
            if(PiecesPerSecond == minCapacity)
                minCapacityProcesses.add(process);
        }
        return minCapacityProcesses;
    }
    void setCapacitySurplusMachineOffLine(){
        for(ArrayList<Machine>machines : this){
            for(Machine machine : machines){
                if(machine.generic||machine.getstate()== MachineRuntimeInfo.MachineState.S_OFFLINE)
                    continue;
                ArrayList<Integer>capacityVec = this.getLowestCapacityProcesses(machine.mode);
                if(!capacityVec.contains(machine.process)){
                    ProductLine productLineTemp = this.myclone();
                    for(ArrayList<Machine> e : productLineTemp){
                        for (Machine machinetmp : e){
                            if(machinetmp._index == machine._index) {
                                machinetmp.mode = -1;
                            }
                        }
                    }
                    ArrayList<Integer>capacityVec2 = productLineTemp.getLowestCapacityProcesses(machine.mode);
                    if(!capacityVec2.contains(machine.process)){
                        System.out.println("machine_index:"+machine._index);
                    }
                }
            }
        }
    }
    boolean judgeCapacityOverLastProcess(int machineProcess, int mode){
        int processIndex = 0;
        for(int index = 0; index < Globalvar.gmodels.get(mode).processes.size(); index++){
            if(Globalvar.gmodels.get(mode).processes.get(index) == machineProcess){
                processIndex = index - 1;
                boolean isProcessGeneric = Globalvar.gproductLine.isGenericProcess(processIndex);
                if(isProcessGeneric)
                    processIndex --;
                break;
            }
        }
        if(processIndex < 0)
            return false;
        int lastProcess = Globalvar.gmodels.get(mode).processes.get(processIndex);
        double capacityInProcess = Globalvar.gproductLine.getCapacityInProcess(mode,machineProcess);
        double capacityInLastProcess = Globalvar.gproductLine.getCapacityInProcess(mode, lastProcess);
        return capacityInProcess > capacityInLastProcess;
    }
    public ProductLine myclone() {
        ProductLine stu = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            stu = (ProductLine) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return stu;
    }
}
