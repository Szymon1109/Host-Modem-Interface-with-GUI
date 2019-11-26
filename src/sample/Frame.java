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

    public Frame(int begin, int len, int cc, Vector<Integer> data, int FCS_1, int FCS_2) {

        this.begin = begin;
        this.len = len;
        this.cc = cc;
        this.data = data;
        this.FCS_1 = FCS_1;
        this.FCS_2 = FCS_2;
    }

    public Frame(int begin, int status) {

        this.begin = begin;
        this.status = status;
    }

    public Frame(int len, int cc, Vector<Integer> data) {

        this.begin = 0x02;
        this.len = len;
        this.cc = cc;
        this.data = data;

        int fcs = len + cc;

        for (int getByte : data) {
            fcs += getByte;
        }

        if(fcs > 0xff){
            this.FCS_1 = 0xff;
            this.FCS_2 = fcs - 0xff;
        }
        else{
            this.FCS_1 = fcs;
            this.FCS_2 = 0x00;
        }
    }

    public byte[] getBytes(){

        Vector<Integer> frame = new Vector<>();

        if(begin == 0x02 || begin == 0x03){

            frame.add(begin);
            frame.add(len);
            frame.add(cc);
            frame.addAll(data);
            frame.add(FCS_1);
            frame.add(FCS_2);
        }
        else if(begin == 0x3f){

            frame.add(begin);
            frame.add(status);
        }

        byte[] frameBytes = new byte[frame.size()];

        for(int i = 0; i < frame.size(); i++){

            int getByte = frame.get(i);
            frameBytes[i] = (byte) getByte;
        }

        return frameBytes;
    }

    public int checkFrame() {

        if (begin == 0x02) {
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
        } else {
            return 0;
        }
    }

    public String toHexString() {

        String frame = "[ ]";

        if (begin == 0x02 || begin == 0x03) {

            String begin = String.format("%02x", this.begin);
            String len = String.format("%02x", this.len);
            String cc = String.format("%02x", this.cc);
            String data = "";
            String FCS_1 = String.format("%02x", this.FCS_1);
            String FCS_2 = String.format("%02x", this.FCS_2);

            if (! this.data.isEmpty()) {
                for (Integer getData : this.data) {
                    data += String.format("%02x", getData) + " ";
                }
            }

            frame = "[" + begin + " " + len + " " + cc + " " + data + FCS_1 + " " + FCS_2 + "]";

        } else if (begin == 0x3f) {

            String begin = String.format("%02x", this.begin);
            String status = String.format("%02x", this.status);

            frame = "[" + begin + " " + status + "]";
        }

        return frame;
    }

    public String toAsciiString() {

        String frame = "[ ]";

        if (begin == 0x02 || begin == 0x03) {

            char begin = (char) this.begin;
            char len = (char) this.len;
            char cc = (char) this.cc;
            String data = "";
            char FCS_1 = (char) this.FCS_1;
            char FCS_2 = (char) this.FCS_2;

            if (! this.data.isEmpty()) {
                for (Integer getData : this.data) {
                    data += (char) getData.intValue() + " ";
                }
            }

            frame = "[" + begin + " " + len + " " + cc + " " + data + FCS_1 + " " + FCS_2 + "]";

        } else if (begin == 0x3f) {

            char begin = (char) this.begin;
            char status = (char) this.status;

            frame = "[" + begin + " " + status + "]";
        }

        return frame;
    }
}
