package com.example.fitscore;

import java.util.ArrayList;

public class Voter {
    public static ArrayList<Task> gTasks;
    MachineAssignerForProductLine machineAssigner;
    ArrayList<Integer> currentBatch, lastBatch;
    double bestTimeCost = Integer.MAX_VALUE;
    void vote(ArrayList<Integer>indexofTasks/*任务索引数组*/, int countofparallelable/*最大同时生产数*/){
        countofparallelable = Math.min(indexofTasks.size(), countofparallelable);
        for(int i = 0; i < indexofTasks.size(); i ++){
            currentBatch.add(indexofTasks.get(i));
            if(countofparallelable == 1){
                //评估这个组合
                //从索引变到task
                ArrayList<Task> batch = new ArrayList<>();
                for(int e : currentBatch){
                    batch.add(gTasks.get(e));
                }
                ArrayList<Integer> modelIdAll = new ArrayList<>();
                //在评估的时候，如果需要停下正在生产的零件转去做别的零件，以产能差追上换型代价来判断避免错误换型
                for(Task task : gTasks){
                    modelIdAll.add(task.job.model);
                }
                ArrayList<Integer> modelIdCurrent = new ArrayList<>();
                for(Task task: batch){
                    modelIdCurrent.add(task.job.model);
                }
                int isResonable = 1;
                for(int modelId : lastBatch){
                    boolean isModelInTasks = modelIdAll.contains(modelId);
                    boolean isModelInCurrentBatch = modelIdCurrent.contains(modelId);
                    if(isModelInTasks && !isModelInCurrentBatch){
                        isResonable = 0;
                        break;
                    }
                }
                if(isResonable == 0)
                    machineAssigner.bestTimeCost = Double.MAX_VALUE;
                //计算当前产线状态生产这batch的最佳时间，注意产线状态的保持和更新
                else{
                    for(Task e : batch)
                        System.out.println(e.job.model+" ");
                }
                if(machineAssigner.bestTimeCost < bestTimeCost){
                    //Globalvar.bestConfig = machineAssigner.best
                }
            }
        }
    }
}
