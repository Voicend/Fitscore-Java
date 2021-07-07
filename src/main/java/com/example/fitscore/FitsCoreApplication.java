package com.example.fitscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@SpringBootApplication
public class FitsCoreApplication {

    public static void main(String[] args) {
        Globalvar.loadgShiftMatrix(Config.COMatrixfilename);
        Globalvar.loadModelsManager(Config.ModelProcessMatrixfilename);
        Globalvar.loadClockMatrix(Config.MachineClockMatrixfilename);
        Globalvar.loadProductLineMatrix(Config.ProductlineMatrixfilename);
        String inputfile = "data/input.csv";
        Globalvar.parseRequirement(inputfile);
        Globalvar.loadjobs();
        System.out.println(Globalvar.gTasks.get(0).count);
        Simulator simulator = new Simulator(Globalvar.gproductLine);
    }
}
