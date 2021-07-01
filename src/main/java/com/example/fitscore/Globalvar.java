package com.example.fitscore;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.util.Pair;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.jackson.JsonObjectSerializer;

import java.io.*;
import java.util.*;
import com.alibaba.fastjson.*;


public class Globalvar {//存放全局变量，以及实现了数据读入
    public static ModelsManager gmodels = new ModelsManager();
    public static Map<String, ShiftMatrix> gShiftMatrixs = new HashMap<>();
    public static ClockMatrix gclocks = new ClockMatrix();
    public static ProductLine gproductLine = new ProductLine();
    public static Map<Integer, Pair<Double, Double>> workAndWaitTime = new HashMap<>();
    public static Map<Integer, Pair<Double, Integer>> coTimeAndCount = new HashMap<>();
    public static ArrayList<Task> gTasks = new ArrayList<>();
    public static ShadowConfiguration bestConfig = new ShadowConfiguration();
    public static JobsQueue jobs = new JobsQueue();
    public static Map<Integer, Map<Integer, Map<Integer, JobUnit>>>jop = new HashMap<>();//(model/process/jobid/job)
    void loadgShiftMatrix(String filename){
        String inString = "";
        String[] ModelIds = new String[0];
        try{
            BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
            int lines = 0;
            while(reader.readLine()!=null)lines++;
            lines -= 2;
            reader = new BufferedReader(new FileReader(new File(filename)));
            reader.readLine();
            if ((inString = reader.readLine())!= null) {
                ModelIds = inString.split(",");
            }
            for (int i = 2; i < ModelIds.length; i++) {
                ShiftMatrix.id2index.put(Integer.parseInt(ModelIds[i]), i - 2);
                ShiftMatrix.index2id.put(i - 2, Integer.parseInt(ModelIds[i]));
            }
            int count = 0, l = ModelIds.length - 2;
            while(count < lines) {
                ShiftMatrix shiftMatrix = new ShiftMatrix();
                String name = "";
                shiftMatrix.matrix = new double[l][l];
                for(int n = 0; n < l; n ++) {
                    inString = reader.readLine();
                    String[] c = inString.split(",");
                    name = c[0];
                    int from = ShiftMatrix.id2index.get(Integer.parseInt(c[1]));
                    for (int i = 2; i < c.length; i++) {
                        int to = ShiftMatrix.id2index.get(Integer.parseInt(ModelIds[i]));
                        double time = c[i].equals("") ? 0 : Double.parseDouble(c[i]);
                        shiftMatrix.matrix[from][to] = time;
                    }
                }
                gShiftMatrixs.put(name,shiftMatrix);
                count += l;
            }

        }catch (FileNotFoundException ex){
            System.out.println("Can't find file:\""+filename+"\"");
        }catch (IOException ex){
            System.out.println("Read file error!");
        }
    }
    void loadModelsManager(String filename){
        String instring = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
            reader.readLine();
            while((instring = reader.readLine())!=null){
                String[] tmp = instring.split(",");
                Models model = new Models();
                model.id = Integer.parseInt(tmp[0]);
                model.name = tmp[1];
                for(int i = 2; i < tmp.length; i ++){
                    if(tmp[i].equals("1"))
                        model.processes.add(i - 2);
                }
                gmodels.add(model);
            }
        }catch (FileNotFoundException ex){
            System.out.println("Can't find file:\""+filename+"\"");
        }catch (IOException ex){
            System.out.println("Read file error!");
        }
    }
    void loadClockMatrix(String filename){
        String inString = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
            inString = reader.readLine();
            String[] tmp = inString.split(",");
            for(int i = 1; i < tmp.length; i ++){
                int id = gmodels.get(tmp[i]).id;
                gclocks.id2index.put(id, i - 1);
                gclocks.index2id.put(i - 1, id);
            }
            while((inString = reader.readLine())!=null){
                tmp = inString.split(",");
                String name = tmp[0];
                MachineClockMarix machineClockMarix = new MachineClockMarix();
                for(int i = 1; i < tmp.length; i ++){
                    double time = tmp[i].equals("")?0:Double.parseDouble(tmp[i]);
                    int id = gclocks.index2id.get(i - 1);
                    machineClockMarix.put(id, time);
                }
                gclocks.put(name, machineClockMarix);
            }
        }catch (FileNotFoundException ex){
            System.out.println("Can't find file:\""+filename+"\"");
        }catch (IOException ex){
            System.out.println("Read file error!");
        }
    }
    void loadProductLineMatrix(String filename){
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
            String jsonString = reader.readLine();
            Gson gson = new Gson();
            Jsonclass[] J = new Gson().fromJson(jsonString, Jsonclass[].class);
            for(Jsonclass process : J){
                ArrayList<Machine> processTemp = new ArrayList<>();
                for(Jmachine jmachine : process.machines){
                    Machine machinetmp = new Machine();
                    machinetmp.process = jmachine.process;
                    machinetmp.number = jmachine.machineNumber;
                    machinetmp.name = jmachine.machineName;
                    machinetmp.generic = !(jmachine.generic==0);
                    machinetmp.bufferSize = new Pair<>(jmachine.bufferSize,jmachine.bufferSize);
                    machinetmp.toIdle = jmachine.toIdle;
                    machinetmp.COTarget = jmachine.COTarget;
                    machinetmp.state = MachineRuntimeInfo.MachineState.values()[jmachine.state];
                    machinetmp.status = machinetmp.state==MachineRuntimeInfo.MachineState.S_ONLINE?MachineRuntimeInfo.MachineStatus.values()[jmachine.status]: MachineRuntimeInfo.MachineStatus.MS_IDLE;
                    machinetmp.mode = jmachine.mode;
                    for(Jbuffer jbuffer : jmachine.inputBuffer){
                        JobUnit jobUnit = new JobUnit();
                        jobUnit.ioo = jbuffer.ioo;
                        jobUnit.process = jbuffer.process;
                        jobUnit.daysIndex = jbuffer.daysIndex;
                        machinetmp.inputBuffer.add(jobUnit);
                    }
                    for(Jbuffer jbuffer : jmachine.outputBuffer){
                        JobUnit jobUnit = new JobUnit();
                        jobUnit.ioo = jbuffer.ioo;
                        jobUnit.process = jbuffer.process;
                        jobUnit.daysIndex = jbuffer.daysIndex;
                        machinetmp.outputBuffer.add(jobUnit);
                    }
                    for(Jbuffer jbuffer : jmachine.producting){
                        JobUnit jobUnit = new JobUnit();
                        jobUnit.ioo = jbuffer.ioo;
                        jobUnit.process = jbuffer.process;
                        jobUnit.daysIndex = jbuffer.daysIndex;
                        machinetmp.model.add(jobUnit);
                    }
                    processTemp.add(machinetmp);
                }
                gproductLine.add(processTemp);
            }
            for(ArrayList<Machine> process : gproductLine){
                for(Machine m : process){
                    m.clocks = gclocks.get(m.name);
                    gproductLine.index(m);
                }
            }
        } catch (JSONException | IOException e){
            e.printStackTrace();
        }
    }
    void loadjobs(){
        for(ArrayList<Machine> machines : gproductLine){//将现场信息加载进jobs
            for (Machine machine : machines){
                if(machine.getstate() != MachineRuntimeInfo.MachineState.S_ONLINE)continue;
                Queue<JobUnit> inputBufferTemp = machine.inputBuffer;
                Queue<JobUnit> outputBufferTemp = machine.outputBuffer;
                Queue<JobUnit> productingTemp = machine.model;
                //避免machine.statue=working的情况下没有生产零件
                if(productingTemp.isEmpty()&&machine.status== MachineRuntimeInfo.MachineStatus.MS_WORKING){
                    machine.status = MachineRuntimeInfo.MachineStatus.MS_IDLE;
                }
                while(!inputBufferTemp.isEmpty())jobs.add(inputBufferTemp.poll());
                while(!outputBufferTemp.isEmpty())jobs.add(inputBufferTemp.poll());
                while(!productingTemp.isEmpty())jobs.add(inputBufferTemp.poll());
            }
        }
        JobsQueue jobstmp = jobs;
        int total = jobstmp.size();
        while(!jobstmp.isEmpty()){
            JobUnit jobtmp = jobstmp.poll();
            int p = jobtmp.ioo < 0 ? -1 : gmodels.get(jobtmp.model).processes.get(jobtmp.ioo);
            if(!jop.containsKey(jobtmp.model))
                jop.put(jobtmp.model, new HashMap<>());
            if(!jop.get(jobtmp.model).containsKey(p))
                jop.get(jobtmp.model).put(p, new HashMap<>());
            jop.get(jobtmp.model).get(p).put(jobtmp.uid,jobtmp);
        }
    }
}

class Jsonclass{
    public int process = 0;
    public ArrayList<Jmachine> machines = new ArrayList<>();
    void setProcess(int p){
        this.process = p;
    }
    void setMachines(ArrayList<Jmachine> m){
        machines.addAll(m);
    }
}
class Jmachine{
    public int mode = -1;
    public ArrayList<Jbuffer> outputBuffer = new ArrayList<>();
    public int process = 0;
    public int COTarget = -1;
    public ArrayList<Jbuffer> inputBuffer = new ArrayList<>();
    public ArrayList<Jbuffer> producting = new ArrayList<>();
    public String machineNumber = "";
    public int state = 0;
    public String machineName = "";
    public int generic = 0;
    public int toIdle = 0;
    public int bufferSize = 0;
    public int status = 0;
    void setProcess(int p){
        this.process = p;
    }
    void setMode(int m){
        this.mode = m;
    }
    void setOutputBuffer(ArrayList<Jbuffer> o){
        outputBuffer.addAll(o);
    }
    void setCOTarget(int c){
        this.COTarget = c;
    }
    void setMachineNumber(String s){
        this.machineNumber = s;
    }
    void setInputBuffer(ArrayList<Jbuffer> o) {
        this.inputBuffer.addAll(o);
    }
    void setProducting(ArrayList<Jbuffer> o){
        this.producting.addAll(o);
    }
    void setState(int s){
        this.state = s;
    }
    void setMachineName(String s){
        this.machineName = s;
    }
    void setGeneric(int g){
        this.generic = g;
    }
    void setToIdle(int t){
        this.toIdle = t;
    }
    void setBufferSize(int b){
        this.bufferSize = b;
    }
    void setStatus(int s){
        this.status = s;
    }
}
class Jbuffer{
    int process = 0;
    int ioo = 0;
    int model = 0;
    int daysIndex = 0;
    void setProcess(int p){
        this.process = p;
    }
    void setIoo(int i){
        this.ioo = i;
    }
    void setModel(int m){
        this.model = m;
    }
    void setDaysIndex(int d){
        this.daysIndex = d;
    }
}