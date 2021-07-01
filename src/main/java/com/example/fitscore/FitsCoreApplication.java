package com.example.fitscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FitsCoreApplication {

    public static void main(String[] args) {
        Globalvar globalvar = new Globalvar();
        globalvar.loadgShiftMatrix(Config.COMatrixfilename);
        globalvar.loadModelsManager(Config.ModelProcessMatrixfilename);
        globalvar.loadClockMatrix(Config.MachineClockMatrixfilename);
        globalvar.loadProductLineMatrix(Config.ProductlineMatrixfilename);
        System.out.println(Globalvar.gproductLine.get("02-051-007").number);
    }
}
