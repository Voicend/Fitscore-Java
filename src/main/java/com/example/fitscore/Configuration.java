package com.example.fitscore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Configuration extends BaseConfiguration{
    Configuration(Map<Integer/*道序*/, Map<Integer/*零件*/,Map<Integer/*车床*/, ArrayList<Machine>>>> matrix, ProductLine productLine){
        for(Map.Entry<Integer/*道序*/, Map<Integer/*零件*/,Map<Integer/*车床*/, ArrayList<Machine>>>> e : matrix.entrySet()) {
            int o = e.getKey();
            Map<Integer/*零件*/, Map<Integer/*车床*/, ArrayList<Machine>>> mapping = e.getValue();
            Map<Integer, ArrayList<Machine>> m2m = new HashMap<>();
            for (Map.Entry<Integer/*零件*/, Map<Integer/*车床*/, ArrayList<Machine>>> e1 : mapping.entrySet()) {
                Map<Integer/*车床*/, ArrayList<Machine>> m2m_mapping = e1.getValue();

                for (Map.Entry<Integer/*车床*/, ArrayList<Machine>> e2 : m2m_mapping.entrySet()) {
                    for (Machine pm : e2.getValue()) {
                        //将第几种零件换成零件的model-id
                        int id = productLine.tasks.get(e1.getKey()).job.model;
                        if (!m2m.containsKey(id)) m2m.put(id, new ArrayList<Machine>());
                        ArrayList<Machine> tmplist = m2m.get(id);
                        tmplist.add(pm);
                        m2m.put(id, tmplist);
                        if(pm.mode < 0)pm.mode = id;
                    }
                }
            }
            this.put(o,m2m);
        }
    }
    Configuration(ProductLine productLine){
        for(int i = 0; i < productLine.size(); i ++){
            for(Machine machinetmp : productLine.get(i)){
                this.get(i).get(machinetmp.mode).add(machinetmp);
            }
        }
    }
    Configuration(){};
    void refreshCfgMachinesModeAndToIdle(ProductLine productLine){
        for(Map.Entry<Integer/*道序*/,Map<Integer/*零件*/, ArrayList<Machine>>> e : this.entrySet()){
            ArrayList<Machine> machines = productLine.get(e.getKey());
            Map<Integer, Machine> mm = new HashMap<>();
            for(Machine m1 : machines){
                mm.put(m1._index, m1);
            }
            for(Map.Entry<Integer, ArrayList<Machine>> e1 : e.getValue().entrySet()){
                for(Machine e2 : e1.getValue()){
                    e2.toIdle = mm.get(e2._index).toIdle;
                    e2.mode = mm.get(e2._index).mode;
                }
            }
        }
    }
    double getCapacityBymodelId(int id){
        double minCapacity = Integer.MAX_VALUE*1.0;
        for(int process : Globalvar.gmodels.get(id).processes){
            if(process == 3 || process == 5)
                continue;
            double PiecesPerSecond = 0.0;
            for(Machine machine : this.get(process).get(id)){
                PiecesPerSecond += 1/machine.clock(id);
            }
            if(PiecesPerSecond < minCapacity)
                minCapacity = PiecesPerSecond;
        }
        return minCapacity;
    }
}
