package sample;

import java.util.Vector;

public class Frame {

    private int begin;
    private int len;
    private int cc;
    private Vector<Integer> data;
    private int FCS_1;
    private int FCS_2;
    private int status;

    public Frame(int begin, int len, int cc, Vector<Integer> data, int FCS_1, int FCS_2){

        this.begin = begin;
        this.len = len;
        this.cc = cc;
        this.data = data;
        this.FCS_1 = FCS_1;
        this.FCS_2 = FCS_2;
    }

    public Frame(int begin, int status){

        this.begin = begin;
        this.status = status;
    }

    public int makeFrame(){

        if(begin == 0x02) {
            int sumCorrect = FCS_1 + FCS_2;
            int sumCalc = len + cc;

            for (Integer getData : data) {
                sumCalc += getData;
            }

            if (sumCorrect == sumCalc) {
                return 1;
            } else {
                return -1;
            }
        }
        else{
            return 0;
        }
    }

    @Override
    public String toString(){

        String frame = null;

        if(begin == 0x02 || begin == 0x03){

            frame = "[" + String.format("%02x", begin) + " " + String.format("%02x", len) + " " + String.format("%02x", cc) + " ";

            if(data != null) {
                for (Integer getData : data) {
                    frame += String.format("%02x", getData) + " ";
                }
            }

            frame += String.format("%02x", FCS_1) + " " + String.format("%02x", FCS_2) + "]";
        }

        else if(begin == 0x3f){
            frame = "[" + String.format("%02x", begin) + " " + String.format("%02x", status) + "]";
        }

        return frame;
    }
}
