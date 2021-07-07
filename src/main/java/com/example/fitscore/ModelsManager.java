package com.example.fitscore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ModelsManager extends ArrayList<Models>{
    private static final ModelsManager instance = new ModelsManager();
    ModelsManager(){}
    public static ModelsManager getInstance(){return instance;}
    static Models InvalidModel;
    public Models get(int id){//根据ID查找
        for(Models m : this){
            if(m.id == id) return m;
        }
        return InvalidModel;
    }
    public Models get(String name){//根据name查找
        for(Models m : this){
            if(m.name.equals(name)) return m;
        }
        return InvalidModel;
    }
}
