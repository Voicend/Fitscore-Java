package com.example.fitscore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class TaskManager {
    private static final TaskManager instance = new TaskManager();
    private TaskManager(){};
    public static TaskManager getInstance(){return instance;}
    Map<String, ArrayList<Task>> tasks = new HashMap<>();//日期任务表
    Map<Integer, ArrayList<Task>> indexer = new HashMap<>();//索引任务表
    int progress = 0; // 当前进展到哪天
    ArrayList<Task> get(int index){
        return indexer.get(index);
    }
    ArrayList<Task> get(String date){
        return tasks.get(date);
    }
    ArrayList<Task> front() {
        return indexer.get(progress);
    }
    void put(String date, ArrayList<Task>ttasks){
        tasks.put(date, ttasks);
        int index = tasks.size() - 1;
        indexer.put(index, ttasks);
    }
    int fill(PriorityQueue<JobUnit>jobs, int countofdays, ArrayList<Task>tasks){
        int result = 0;
        do{
            if(empty())
                break;
            ArrayList<Task> tasksofoneday = get(progress);
            for(Task e : tasksofoneday){
                for(int i = 0; i < e.count; i ++){
                    JobUnit jobUnit = new JobUnit();
                    jobUnit.model = e.job.model;
                    jobUnit.releaseTime = 0;
                    jobUnit.daysIndex = progress;
                    jobUnit.taskinfodate = e.date;
                    jobUnit.uid = Globalvar.uid++;
                    jobs.add(jobUnit);
                }
            }
            //update gtasks
            tasks.addAll(tasksofoneday);
            //update jop
            PriorityQueue<JobUnit>jobstemp = new PriorityQueue<>(Globalvar.jobs);
            while(!jobstemp.isEmpty()){
                JobUnit e = jobstemp.poll();
                int p = e.ioo < 0 ? -1 : Globalvar.gmodels.get(e.model).processes.get(e.ioo);
                if(!Globalvar.jop.containsKey(e.model))
                    Globalvar.jop.put(e.model,new HashMap<>());
                if(!Globalvar.jop.get(e.model).containsKey(p))
                    Globalvar.jop.get(e.model).put(p,new HashMap<>());
                Globalvar.jop.get(e.model).get(p).put(e.uid,e);
            }
            result += 1;
            progress += 1;
            countofdays-=1;
        }while(countofdays>0);
        return result;
    }
    boolean empty(){
        return progress == indexer.size();
    }
}
