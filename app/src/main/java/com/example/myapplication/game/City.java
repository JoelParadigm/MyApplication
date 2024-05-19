package com.example.myapplication.game;

import org.opencv.core.Point;

import java.util.List;
import java.util.Random;

public class City {
    public String  color;
    public int id;
    public int x, y;
    public int population = 50;
    Random random = new Random();
    public List<Integer> enemies;
    public List<Integer> friends;
    int passivProb = 20;
    int activProb = 20;


    public City(String color, int id, int x, int y){
        this.color = color;
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public String move(){
        int action = random.nextInt(100);
        if(action <= passivProb && population > 1){
            return help();
        } else if (action <= passivProb + activProb && population > 1) {
            return attack();
        }
        else {
            growPopulation();
            return "rest";
        }
    }

    private String attack() {
        String result = id +" ";
        if(population >  2){
            result += "2 ";
            population -= 2;
        } else {
            result += "1 ";
            population -= 1;
        }

        if(enemies.isEmpty()) return "rest";
        int eId = enemies.get(random.nextInt(enemies.size()));
        return result += eId;
    }

    private String help() {
        String result = id+" ";
        if(population >  2){
            result += "2 ";
            population -= 2;
        } else {
            result += "1 ";
            population -= 1;
        }

        if(friends.isEmpty()) return "rest";
        int eId = friends.get(random.nextInt(friends.size()));
        return result += eId;
    }

    public void growPopulation(){
        if(population > 0){
            population++;
        }
    }
}
