package sample;

import java.util.Vector;

public class Frame {

    private int begin;
    private int len;
    private int cc;
    private Vector<Integer> data;
    private int FCS_1;
    private int FCS_2;

    public Frame(int begin, int len, int cc, Vector<Integer> data, int FCS_1, int FCS_2){
        this.begin = begin;
        this.len = len;
        this.cc = cc;
        this.data = data;
        this.FCS_1 = FCS_1;
        this.FCS_2 = FCS_2;
    }

    public boolean makeFrame(){

        int sumCorrect = FCS_1 + FCS_2;
        int sumCalc = len + cc;

        for (Integer getData: data) {
            sumCalc += getData;
        }

        if(sumCorrect == sumCalc){
            return true;
        }
        else{
            return false;
        }
    }
}
