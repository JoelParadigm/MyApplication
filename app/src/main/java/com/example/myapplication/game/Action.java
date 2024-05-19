package com.example.myapplication.game;

public class Action {
    public int from;
    public int to;
    public int army = 0;

    public int progress = 0;

    public Action(String action){
        String[] parts = action.split("\\s+");
        from = Integer.parseInt(parts[0]);
        army = Integer.parseInt(parts[1]);
        to = Integer.parseInt(parts[2]);
    }

    public void proceed(){
        progress++;
    }
}
