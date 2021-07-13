package com.example.fitscore;

import java.util.*;

public class BaseSimulator {
    ProductLine ppl = new ProductLine();
    PriorityQueue<JobUnit> jobs = new PriorityQueue<>((o1, o2) -> o1.releaseTime < o2.releaseTime ? -1 : 1);
    BaseSimulator(ProductLine pl){
        this.ppl = pl;
    }
}
