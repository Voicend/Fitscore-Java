package com.example.fitscore;

import javax.crypto.Mac;
import javax.security.auth.login.Configuration;
import java.util.ArrayList;
import java.util.Map;

public class MachineAssignerForProductLine {
    Map<Integer/*工序*/, Map<Integer/*车床*/, ArrayList<Machine>/*机器*/>> A;
    boolean check = false;
    Map<Integer/*道序*/, Map<Integer/*零件*/, Map<Integer/*车床*/, ArrayList<Machine>>>>  assignment, best;
    int K;//零件种类数量
    double bestTimeCost;//已知最短时间
    int countChangeType = 0;
    ArrayList<Task> tasks;
    ProductLine productLine;
    ShadowConfiguration bestConfigutation;
    Map<Integer/*global*/, MachineRuntimeInfo>snapshotOfMachines, bestSnapshotofMachines;
}
