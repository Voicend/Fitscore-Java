package com.example.fitscore;

import java.util.ArrayList;
import java.util.Map;
import java.util.PriorityQueue;

public class TaskManager {
    private static final TaskManager instance = new TaskManager();
    private TaskManager(){};
    public static TaskManager getInstance(){return instance;}
    Map<String, ArrayList<Task>> tasks;//日期任务表
    Map<Integer, ArrayList<Task>> indexer;//索引任务表
    int progress = 0; // 当前进展到哪天
    void put(String date, ArrayList<Task>ttasks){
        tasks.put(date, ttasks);
        int index = tasks.size() - 1;
        indexer.put(index, ttasks);
    }
    ArrayList<Task> get(int index){
        return indexer.get(index);
    }
    ArrayList<Task> get(String date){
        return tasks.get(date);
    }
    ArrayList<Task> front() {
        return indexer.get(progress);
    }
}
