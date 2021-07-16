package com.example.fitscore;

import com.google.gson.Gson;

import javafx.util.Pair;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import com.alibaba.fastjson.*;


public class Globalvar {//存放全局变量，以及数据读入
    public static ModelsManager gmodels = new ModelsManager();
    public static Map<String, ShiftMatrix> gShiftMatrixs = new HashMap<>();
    public static ClockMatrix gclocks = new ClockMatrix();
    public static ProductLine gproductLine = new ProductLine();
    public static Map<Integer, Pair<Double, Double>> workAndWaitTime = new HashMap<>();
    public static Map<Integer, Pair<Double, Integer>> coTimeAndCount = new HashMap<>();
    public static ArrayList<Task> gTasks = new ArrayList<>();
    public static ShadowConfiguration bestConfig = new ShadowConfiguration();
    public static PriorityQueue<JobUnit> jobs = new PriorityQueue<JobUnit>(
            (o1, o2) -> o1.releaseTime > o2.releaseTime ? -1 : 1
    );
    public static Map<Integer, Map<Integer, Map<Integer, JobUnit>>>jop = new TreeMap<>();//(model/process/jobid/job)
    public static String mqHost = "127.0.0.1";
    public static String mqUsername;
    public static String mqPassword;
    public static String mqQueueName = "fits.core";
    public static int port = 5672;
    public static int windowPhase = 3;
    public static int windowMaxPreFetchDays = 3;
    public static int windowMaxDelaySeconds = 79200*3;
    public static int workingTimePerDay = 86400;
    public static int checkTime = 60;
    public static int endlessLoopWaitTime = 50000;
    public static double coSusceptibility = 1.0;
    public static int offset = 3600;
    public static int uid = 0;
    public static int isProcessDataShown = 0;
    static void loadgShiftMatrix(String filename){
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
    static void loadgContext(String filename){
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
            String instring = reader.readLine();
            while((instring = reader.readLine())!=null){
                String[] row = instring.split(",");
                switch (row[0]) {
                    case "MQInfo":
                        mqHost = row[1];
                        mqUsername = row[2];
                        mqPassword = row[3];
                        mqQueueName = row[4];
                        port = Integer.parseInt(row[5]);
                        break;
                    case "WindowPhase": {
                        String value = row[1];
                        if (value.trim().isEmpty()) value = "3";
                        windowPhase = Integer.parseInt(value);
                        value = row[2];
                        if (value.trim().isEmpty()) value = "3";
                        windowMaxPreFetchDays = Integer.parseInt(value);    ///最多预取多少天内的任务

                        value = row[3];
                        if (value.trim().isEmpty()) value = "237600";
                        windowMaxDelaySeconds = Integer.parseInt(value);    ///最大可延迟交付时间

                        break;
                    }
                    case "fits.core.co.susceptibility": {
                        String value = row[1];
                        if (value.trim().isEmpty()) value = "1";
                        coSusceptibility = Double.parseDouble(value);    ///换型敏感度

                        break;
                    }
                    case "fits.machine.coldtime": {
                        String value = row[1];
                        if (value.trim().isEmpty()) value = "3600";
                        offset = Integer.parseInt(value);    ///冷却时间

                        break;
                    }
                    case "fits.core.time-length-of-day": {
                        String value = row[1];
                        if (value.trim().isEmpty()) value = "86400";
                        workingTimePerDay = Integer.parseInt(value);    ///一日干活时间

                        break;
                    }
                    case "fits.simulator.checktime": {
                        String value = row[1];
                        if (value.trim().isEmpty()) value = "60";
                        checkTime = Integer.parseInt(value);    ///检查?型机器时间

                        break;
                    }
                    case "fits.simulator.endless-loop-waitTime": {
                        String value = row[1];
                        if (value.trim().isEmpty()) value = "50000";
                        endlessLoopWaitTime = Integer.parseInt(value);    ///跳出循环时间防止卡死

                        break;
                    }
                }
            }
        } catch (
                FileNotFoundException ex) {
            System.out.println("Can't find file:\"" + filename + "\"");
        } catch (IOException ex) {
            System.out.println("Read file error!");
        }
    }
    static void loadModelsManager(String filename){
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
    static void loadClockMatrix(String filename){
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
    static void loadProductLineMatrix(String filename){
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
                    if(!jmachine.producting.isEmpty()&&machinetmp.getstate()== MachineRuntimeInfo.MachineState.S_ONLINE)
                        machinetmp.status = MachineRuntimeInfo.MachineStatus.MS_WORKING;
                    machinetmp.mode = jmachine.mode;
                    for(Jbuffer jbuffer : jmachine.inputBuffer){
                        JobUnit jobUnit = new JobUnit();
                        jobUnit.ioo = jbuffer.ioo;
                        jobUnit.process = jbuffer.process;
                        jobUnit.daysIndex = jbuffer.daysIndex;
                        jobUnit.model = jbuffer.model;
                        jobUnit.uid = uid++;
                        machinetmp.inputBuffer.add(jobUnit);
                    }
                    for(Jbuffer jbuffer : jmachine.producting){
                        JobUnit jobUnit = new JobUnit();
                        jobUnit.ioo = jbuffer.ioo;
                        jobUnit.process = jbuffer.process;
                        jobUnit.daysIndex = jbuffer.daysIndex;
                        jobUnit.model = jbuffer.model;
                        jobUnit.uid = uid++;
                        machinetmp.model.add(jobUnit);
                    }
                    for(Jbuffer jbuffer : jmachine.outputBuffer){
                        JobUnit jobUnit = new JobUnit();
                        jobUnit.ioo = jbuffer.ioo;
                        jobUnit.process = jbuffer.process;
                        jobUnit.daysIndex = jbuffer.daysIndex;
                        jobUnit.model = jbuffer.model;
                        jobUnit.uid = uid++;
                        machinetmp.outputBuffer.add(jobUnit);
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
    static void loadjobs(){
        for(ArrayList<Machine> machines : gproductLine){//将现场信息加载进jobs
            for (Machine machine : machines){
                if(machine.getstate() != MachineRuntimeInfo.MachineState.S_ONLINE)continue;
                Queue<JobUnit> inputBufferTemp = new ArrayDeque<>(machine.inputBuffer);
                Queue<JobUnit> outputBufferTemp = new ArrayDeque<>(machine.outputBuffer);
                Queue<JobUnit> productingTemp = new ArrayDeque<>(machine.model);
                //避免machine.statue=working的情况下没有生产零件
                if(productingTemp.isEmpty()&&machine.status== MachineRuntimeInfo.MachineStatus.MS_WORKING){
                    machine.status = MachineRuntimeInfo.MachineStatus.MS_IDLE;
                }
                while(!inputBufferTemp.isEmpty())jobs.add(inputBufferTemp.poll());
                while(!outputBufferTemp.isEmpty())jobs.add(outputBufferTemp.poll());
                while(!productingTemp.isEmpty())jobs.add(productingTemp.poll());
            }
        }
        //加载现场信息进jop
        PriorityQueue<JobUnit> jobstemp = new PriorityQueue<>(jobs);
        while(!jobstemp.isEmpty()){
            JobUnit e = jobstemp.poll();
            int p = e.ioo < 0 ? -1 : gmodels.get(e.model).processes.get(e.ioo);
            if(!jop.containsKey(e.model))
                jop.put(e.model,new TreeMap<>());
            if(!jop.get(e.model).containsKey(p))
                jop.get(e.model).put(p, new TreeMap<>());
            jop.get(e.model).get(p).put(e.uid,e);
        }
        //加载现场信息进gtasks
        Map<Integer, Integer> modelscount = new HashMap<>();//<model,count>
        for(JobUnit jobtmp : jobs){
            if(modelscount.containsKey(jobtmp.model)){
                modelscount.put(jobtmp.model,modelscount.get(jobtmp.model)+1);
            }else {
                modelscount.put(jobtmp.model,1);
            }
        }
        for(Map.Entry<Integer, Integer>e : modelscount.entrySet()){
            gTasks.add(new Task(e.getKey(), e.getValue()));
        }
    }
    static void refresh(int simulatorRealTime, Map<Integer, Map<Integer, Integer>> minDayIndexEachModel){//modelid/process/minIndex
        //遍历Jop，统计model和数量
        TaskManager ptm = TaskManager.getInstance();
        Map<Integer, Integer>counting = new HashMap<>();
        //遍历jop，统计model和数量
        for(Map.Entry<Integer, Map<Integer, Map<Integer, JobUnit>>> e : jop.entrySet()){
            int model = e.getKey();
            for(Map.Entry<Integer, Map<Integer, JobUnit>> e2: e.getValue().entrySet()){
                Map<Integer, JobUnit> m = e2.getValue();
                if(!m.isEmpty())
                    if(counting.containsKey(model))
                        counting.put(model, counting.get(model)+m.entrySet().size());
                    else
                        counting.put(model, m.size());
            }
            Task task = find_task(model);
            if(task != null){
                task.count = counting.getOrDefault(model, 0);
                if(task.count == 0)
                    gTasks.remove(task);
            }
        }
        //保证gTask中有两天的任务
        Map<String,Integer>countofdays = new HashMap<>();
        ArrayList<Task>tasks = new ArrayList<>(gTasks);
if(simulatorRealTime==409594)
    System.out.println();
        do{
            tasks.removeIf(t -> t.count <= 0);
            int isFirstProcessMachinesIdle = 0;
            for(Machine machine : gproductLine.get(0)){
                if (machine.status == MachineRuntimeInfo.MachineStatus.MS_IDLE
                        && Math.abs(machine.toIdle - simulatorRealTime) > offset) {
                    isFirstProcessMachinesIdle = 1;
                    break;
                }
            }
            minDayIndexEachModel.clear();
            for(Models i : gmodels){
                for(int j = 0; j < i.processes.size(); j++){
                    if(!minDayIndexEachModel.containsKey(i.id))
                        minDayIndexEachModel.put(i.id,new HashMap<>());
                    minDayIndexEachModel.get(i.id).put(i.processes.get(j),Integer.MAX_VALUE);
                }
            }
            Map<Integer,Integer>testDayWindow = new HashMap<>();
            for(Map.Entry<Integer,Map<Integer,Map<Integer,JobUnit>>> e1 : jop.entrySet()){
                for(Map.Entry<Integer,Map<Integer,JobUnit>> e2 : e1.getValue().entrySet()){
                    for(Map.Entry<Integer,JobUnit> e3 : e2.getValue().entrySet()){
                        int minIndex = minDayIndexEachModel.get(e3.getValue().model).getOrDefault(e3.getValue().process, 0);
                        if(minIndex > e3.getValue().daysIndex) minIndex = e3.getValue().daysIndex;
                        minDayIndexEachModel.get(e3.getValue().model).put(e3.getValue().process,minIndex);
                        testDayWindow.put(e3.getValue().daysIndex,1);
                    }
                }
            }
            int minIndex = Integer.MAX_VALUE;
            int maxIndex = 0;
            for(int e1 : testDayWindow.keySet()){
                minIndex = Math.min(minIndex, e1);
                maxIndex = Math.max(maxIndex, e1);
            }
            if(((maxIndex - minIndex >= windowPhase)&&(isFirstProcessMachinesIdle!=0))||(maxIndex-minIndex>=windowMaxPreFetchDays))
                break;
            int state = ptm.fill(jobs,1,tasks);
            tasks.removeIf(t -> t.count <= 0);
            for(Task e : tasks){
                if(!countofdays.containsKey(e.date))
                    countofdays.put(e.date,1);
                else
                    countofdays.put(e.date,countofdays.get(e.date)+1);
            }
            if(state==0){
                break;
            }
        }while(tasks.size()!=0);
        for(Task e : tasks){
            boolean find = false;
            Task tmp = null;
            for(Task t : gTasks){
                if(t.job.model==e.job.model){
                    find = true;
                    tmp = t;
                }
            }
            if(find){
                tmp.count += e.count;
                tmp.deadline = Math.max(e.deadline,tmp.deadline);
            }
            else gTasks.add(e);
        }
    }

    static void parseRequirement(String filename) {
        ArrayList<String> days = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(filename)));
            String instring = reader.readLine();
            String reges = "\\d{4}/\\d{1,2}/\\d{1,2}";
            String[] tmpstring = instring.split(",");
            for(String day : tmpstring){
                if(Pattern.matches(reges, day)){
                    days.add(day);
                }
            }
            Map<String, ArrayList<Integer>> mapping = new HashMap<>();
            while((instring = reader.readLine())!=null){
                tmpstring = instring.split(",");
                ArrayList<Integer> tmparray = new ArrayList<>();
                for(int i = 1; i < tmpstring.length; i ++){
                    tmparray.add(Integer.parseInt(tmpstring[i]));
                }
                mapping.put(tmpstring[0], tmparray);
            }
            for(int i = 0; i < days.size(); i ++){
                ArrayList<Task> tasks = new ArrayList<>();
                for(Map.Entry<String, ArrayList<Integer>> e : mapping.entrySet()){
                    int id = gmodels.get(e.getKey()).id;
                    if(id < 0){
                        System.out.println("un-supported model:"+e.getKey());
                    }
                    int cnt = 0;
                    if(e.getValue().size()>i){
                        cnt = (e.getValue().get(i)+JobUnit.CAPACITY-1)/JobUnit.CAPACITY;
                    }
                    Task task = new Task(i, id,cnt,days.get(i).replace("\r","\0"));
                    task.deadline = (i + windowPhase - 1) * workingTimePerDay;//若考虑假期需要扣除部分天数
                    tasks.add(task);
                }
                TaskManager.getInstance().put(days.get(i).replace("\r","\0"),tasks);
            }
        } catch (
                FileNotFoundException ex) {
            System.out.println("Can't find file:\"" + filename + "\"");
        } catch (IOException ex) {
            System.out.println("Read file error!");
        }
    }
    static Task find_task(int model){
        for(Task t : gTasks)
            if(t.job.model == model)
                return t;
        return null;
    }
    public static void printjop(Map<Integer, Map<Integer, Map<Integer, JobUnit>>>jop){
        System.out.println("---------------------------------------------------");
        for(Map.Entry<Integer,Map<Integer,Map<Integer,JobUnit>>> e : jop.entrySet()){
            System.out.printf("%d\n",e.getKey());
            for(Map.Entry<Integer,Map<Integer,JobUnit>> e2:e.getValue().entrySet()){
                System.out.printf("%d:",e2.getKey());
                int a3=0, a4=0;
                for(Map.Entry<Integer,JobUnit> e3:e2.getValue().entrySet()){
                    if(e3.getValue().daysIndex==0)a3++;
                    else a4++;
                }
                System.out.printf("(%d,%d)\n",a3,a4);
            }
        }
    }
    public static void printconfig(Double realTime, Map<Integer,Integer>config){
        System.out.println(realTime+":");
        for(Map.Entry<Integer,Integer> i : config.entrySet()){
            System.out.printf("(%d->%d)",i.getKey(),i.getValue());
        }
        System.out.println();
    }
    public static void printtasks(ArrayList<Task> tasks){
        for(Task t:tasks){
            System.out.printf("(%d,%d)",t.job.model,t.count);
        }
        System.out.print("\n");
    }
    public static void printmachines(ArrayList<Machine>machines){
        for(Machine machine:machines){
            System.out.printf("\nMachine %d: \n",machine._index);
            System.out.print("Inputbuffer: ");
            for(JobUnit jobUnit:machine.inputBuffer)
                System.out.printf("%d, ",jobUnit.uid);
            System.out.print("\nOnputbuffer: ");
            for(JobUnit jobUnit:machine.outputBuffer)
                System.out.printf("%d, ",jobUnit.uid);
            System.out.print("\nmodel: ");
            for(JobUnit jobUnit:machine.model)
                System.out.printf("%d, ",jobUnit.uid);
        }
    }
    public static void printjopcount(Simulator simulator, Map<Integer, Map<Integer, Map<Integer, JobUnit>>>jop){
        System.out.printf("%d----------------------------------------------------------\n",(int)simulator.realTime);
        for(Map.Entry<Integer,Map<Integer,Map<Integer,JobUnit>>>e:jop.entrySet()){
            System.out.printf("%d:",e.getKey());
            for(Map.Entry<Integer,Map<Integer,JobUnit>>e2:e.getValue().entrySet()){
                System.out.printf("(%d,%d)",e2.getKey(),e2.getValue().size());
            }
            System.out.print("\n");
        }
    }
    public static void printjobs(Queue<JobUnit>jobs){
        for(JobUnit j : jobs){
            System.out.printf("%d : model:%d/process:%d/daysindex:%d\n",j.uid,j.model,j.process,j.daysIndex);
        }
    }
    static Pair<Integer, Integer> getMostSuitableCOMachineInMachines2(
            ArrayList<Integer>triggerIndexVec,int realTime,
            Map<Integer/*modelId*/,Map<Integer/*process*/,Integer/*minIndex*/>>minDayIndexEachModel){
        int finalMachineIndex = -1;
        int COMode = -1;
        double minDiff = Double.MAX_VALUE;
        for(Integer triggerIndex : triggerIndexVec){
            if(gproductLine.machines.get(triggerIndex).generic){
                continue;
            }
            int originalMode = gproductLine.machines.get(triggerIndex).mode;
            int machineProcess = gproductLine.machines.get(triggerIndex).process;
            for(Task task: gTasks){
                int COTomode = task.job.model;
                if(COTomode == originalMode || !gproductLine.machines.get(triggerIndex).support(COTomode))
                    continue;
                //如果零件不经过该道序也跳
                if(!gmodels.get(COTomode).processes.contains(machineProcess))
                    continue;
                //如果该机器目前道序加工该零件的产能就已经大于前面一个道序（不含通用道序）的产能，则也不予以考虑
                boolean isCapacityOverLastProcess = gproductLine.judgeCapacityOverLastProcess(machineProcess, COTomode);
                if(isCapacityOverLastProcess)
                    continue;
                //如果配置了不能换型这个零件也跳
                int minDayIndex = minDayIndexEachModel.get(COTomode).get(machineProcess);
                int requiredCountForEarliestForToMode = 0;
                int requiredCountForAllForToMode = 0;
                int requiredCountForAllForFromMode = 0;
                int maxIndexForToMode = 0;
                int maxIndexForFromMode = 0;
                for(Map.Entry<Integer,Map<Integer,JobUnit>> e : jop.get(COTomode).entrySet()){
                    if(e.getKey()>machineProcess)
                        break;
                    for(Map.Entry<Integer,JobUnit> modelMap:e.getValue().entrySet()){
                        if(modelMap.getValue().daysIndex == minDayIndex)
                            requiredCountForEarliestForToMode++;
                        if(modelMap.getValue().daysIndex > maxIndexForFromMode)
                            maxIndexForToMode = modelMap.getValue().daysIndex;
                        requiredCountForAllForToMode++;
                    }
                }
                for(Map.Entry<Integer,Map<Integer,JobUnit>> e : jop.get(originalMode).entrySet()){
                    if(e.getKey()>machineProcess)
                        break;
                    for(Map.Entry<Integer,JobUnit> modelMap:e.getValue().entrySet()){
                        if(modelMap.getValue().daysIndex > maxIndexForFromMode)
                            maxIndexForFromMode = modelMap.getValue().daysIndex;
                        requiredCountForAllForFromMode++;
                    }
                }
                //处理偏移量
                minDayIndex++;
                maxIndexForToMode++;
                maxIndexForFromMode++;
                int minDeadLineForEarlistForToMode = windowMaxDelaySeconds + minDayIndex * workingTimePerDay;
                int minDeadLineForAllForToMode = windowMaxDelaySeconds + maxIndexForToMode * workingTimePerDay;
                int minDeadLineForAllForFromMode = windowMaxDelaySeconds + maxIndexForFromMode * workingTimePerDay;
                //若不换型在交付之前，还能生产多少框零件
                minDeadLineForAllForToMode = minDeadLineForAllForToMode < realTime ? realTime + workingTimePerDay : minDeadLineForAllForToMode;
                minDeadLineForAllForFromMode = minDeadLineForAllForFromMode < realTime ? realTime + workingTimePerDay : minDeadLineForAllForFromMode;

                double CountToFinishBeforeDeadLineForEarliestForToMode = gproductLine.CountToFinishBeforeDeadLineInCertainProcess(minDeadLineForEarlistForToMode, realTime, COTomode, machineProcess);
                double CountToFinishBeforeDeadLineForAllforToMode = gproductLine.CountToFinishBeforeDeadLineInCertainProcess(minDeadLineForAllForToMode, realTime, COTomode, machineProcess);
                double CountToFinishBeforeDeadLineForAllforFromMode = gproductLine.CountToFinishBeforeDeadLineInCertainProcess(minDeadLineForAllForFromMode, realTime, originalMode, machineProcess);
                //换型之后的产能预估
                ProductLine productLineTemp = gproductLine.myclone();
                for(ArrayList<Machine> process:productLineTemp){
                    for(Machine machine:process){
                        if(machine._index==triggerIndex){
                            machine.COTarget = COTomode;
                            machine.setstate(MachineRuntimeInfo.MachineState.S_ONLINE);
                        }
                    }
                }
                double CountToFinishBeforeDeadLineForAllForToModeAfterCO = productLineTemp.CountToFinishBeforeDeadLineInCertainProcess(minDeadLineForAllForToMode, realTime, COTomode, machineProcess);
                double CountToFinishBeforeDeadLineForAllForTFromModeAfterCO = productLineTemp.CountToFinishBeforeDeadLineInCertainProcess(minDeadLineForAllForFromMode, realTime, originalMode, machineProcess);

                /*换型之前目标产能的偏移百分比*/
                double offsetPercentageForToModeBeforeCO = requiredCountForAllForToMode == 0 ? 0 :
                        (CountToFinishBeforeDeadLineForAllforToMode == 0 ? 0 : Math.abs(CountToFinishBeforeDeadLineForAllforToMode - requiredCountForAllForToMode) / requiredCountForAllForToMode);
                double offsetPerfcentageForFromModeBeforeCo = requiredCountForAllForFromMode == 0 ? 0 : Math.abs(CountToFinishBeforeDeadLineForAllforFromMode - requiredCountForAllForFromMode) / requiredCountForAllForFromMode;
                /*换型之后目标产能的偏移百分比*/
                double offsetPercentageForToModeAfterCO = requiredCountForAllForToMode == Double.MAX_VALUE ? 0 : Math.abs(CountToFinishBeforeDeadLineForAllForToModeAfterCO - requiredCountForAllForToMode) / requiredCountForAllForToMode;
                double offsetPercentageForFromModeAfterCo = requiredCountForAllForFromMode == 0 ? 0 : Math.abs(CountToFinishBeforeDeadLineForAllForTFromModeAfterCO - requiredCountForAllForFromMode) / requiredCountForAllForFromMode;
                double diffPercentage = offsetPercentageForToModeAfterCO - offsetPercentageForToModeBeforeCO;
                //如果这个道序并没有任何机器生产这个零件，则考虑此道序上一个道序是否生产该零件，或者该道序是首道序
                if(offsetPercentageForToModeBeforeCO == 0){
                    int isProcessFirstProcess = 0;
                    int isLastProcessMachinesFits = 0;
                    int isLastProcessMachineOutputBufferFits = 0;
                    int isFirstProcessMachinesFits = 0;
                    //先检验道序是否是首道序
                    if(Globalvar.gmodels.get(COTomode).processes.get(0) == machineProcess){
                        isProcessFirstProcess = 1;
                    }
                    else{
                        //检验第一道序是否有机器适配或者将要适配该零件
                        int firstProcessIndex = Globalvar.gmodels.get(COTomode).processes.get(0);
                        for(Machine machineInFirstProcess : gproductLine.get(firstProcessIndex)){
                            if(machineInFirstProcess.mode == COTomode || machineInFirstProcess.COTarget == COTomode){
                                isFirstProcessMachinesFits = 1;
                                break;
                            }
                        }
                        //检验上一道序是否有机器适配或者将要适配该零件
                        int processIndex = 0;
                        int lastBufferProcess = 0;
                        for(int index = 0; index < gmodels.get(COTomode).processes.size();index++){
                            if(gmodels.get(COTomode).processes.get(index) == machineProcess){
                                processIndex = index - 1;
                                lastBufferProcess = index - 1;
                                boolean isProcessGeneric = gproductLine.isGenericProcess(processIndex);
                                if(isProcessGeneric)
                                    processIndex--;
                                break;
                            }
                        }
                        int lastProcess = gmodels.get(COTomode).processes.get(processIndex);
                        for(Machine machine:gproductLine.get(lastProcess)){
                            if(machine.mode==COTomode||machine.COTarget==COTomode){
                                isLastProcessMachinesFits = 1;
                                break;
                            }
                        }
                        //检验是否会出现前道序后料道零件因为临时开工所以与该道序机器不匹配
                        for(Machine machine:gproductLine.get(lastBufferProcess)){
                            Queue<JobUnit> bufferTest = new ArrayDeque<>(machine.outputBuffer);
                            while(!bufferTest.isEmpty()){
                                JobUnit jobTest = bufferTest.poll();
                                if(jobTest.model == COTomode){
                                    isLastProcessMachineOutputBufferFits = 1;
                                    break;
                                }
                            }
                        }
                    }
                    if(isProcessFirstProcess == 1||isFirstProcessMachinesFits==1||isLastProcessMachineOutputBufferFits==1){
                        diffPercentage = offsetPercentageForToModeAfterCO - 1000;
                    }
                    else{
                        diffPercentage = 0;
                    }
                }
                if(diffPercentage < -0.1 && ((diffPercentage < minDiff
                        && CountToFinishBeforeDeadLineForAllForToModeAfterCO - CountToFinishBeforeDeadLineForAllforToMode > 10)
                        ||(offsetPercentageForToModeBeforeCO > 0.9))){
                    finalMachineIndex = triggerIndex;
                    COMode = COTomode;
                    minDiff = diffPercentage;
                }
            }
        }
        return new Pair<>(finalMachineIndex,COMode);
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