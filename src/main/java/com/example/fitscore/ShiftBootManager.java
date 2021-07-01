package com.example.fitscore;

import java.util.PriorityQueue;

public class ShiftBootManager extends PriorityQueue<ShiftBookItem>{
    private static final ShiftBootManager instance = new ShiftBootManager();
    private ShiftBootManager(){};
    public static ShiftBootManager getInstance(){return instance;}
}
