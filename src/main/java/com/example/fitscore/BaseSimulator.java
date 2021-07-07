package com.example.fitscore;

import java.util.ArrayList;

public class BaseSimulator {
    ProductLine ppl = new ProductLine();
    ArrayList<JobUnit> jobs = new ArrayList<>();
    BaseSimulator(ProductLine pl){
        this.ppl = pl;
    }
}
