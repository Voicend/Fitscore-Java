package com.example.fitscore;

import sun.tools.tree.GreaterExpression;

import java.util.ArrayList;
import java.util.Map;
import java.util.PriorityQueue;

public class JobsQueue extends PriorityQueue<JobUnit> {
    void initialize(Configuration cfg){
        while(!this.isEmpty()){
            JobUnit jobtmp = this.poll();
            int flag = 0;
            for(Map.Entry<Integer, ArrayList<Machine>> cfgtmp : cfg.get(0).entrySet()){
                if(jobtmp.model == cfgtmp.getKey()){
                    flag = 1;
                    break;
                }
            }
            if(flag == 1)
                this.add(jobtmp);
        }
    }
}
