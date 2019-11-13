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

     public int checkFrame(){

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

        String frame = "[ ]";

        if(begin == 0x02 || begin == 0x03){

            String begin = String.format("%02x", this.begin);
            String len = String.format("%02x", this.len);
            String cc = String.format("%02x", this.cc);
            String data = "";
            String FCS_1 = String.format("%02x", this.FCS_1);
            String FCS_2 = String.format("%02x", this.FCS_2);

            if(this.data != null) {
                for (Integer getData : this.data) {
                    data += String.format("%02x", getData) + " ";
                }
            }

            frame = "[" + begin + " " + len + " " + cc + " " + data + FCS_1 + " " + FCS_2 + "]";
        }

        else if(begin == 0x3f){

            String begin = String.format("%02x", this.begin);
            String status = String.format("%02x", this.status);

            frame = "[" + begin + " " + status + "]";
        }

        return frame;
    }
}
