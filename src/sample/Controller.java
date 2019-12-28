package sample;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import static com.fazecast.jSerialComm.SerialPort.*;

public class Controller {

    @FXML
    private ComboBox<SerialPort> ports;

    @FXML
    private Button connectButton;

    @FXML
    private Button disconnectButton;

    @FXML
    private Button resetButton;

    @FXML
    private TextField sendField;

    @FXML
    private RadioButton dataType;

    @FXML
    private Button sendButton;

    @FXML
    private Button clearButton;

    @FXML
    private TextArea hexField;

    @FXML
    private TextArea asciiField;

    @FXML
    private RadioButton BPSK;

    @FXML
    private RadioButton QPSK;

    @FXML
    private RadioButton eightPSK;

    @FXML
    private RadioButton BFSK;

    @FXML
    private ToggleButton phyButton;

    @FXML
    private ToggleButton dlButton;

    @FXML
    private CheckBox FEC;

    private enum STATE {
        LOOK_4_BEGIN, LOOK_4_LEN, LOOK_4_CC, DATA_COLLECT,
        LOOK_4_FCS_1_BYTE, LOOK_4_FCS_2_BYTE, LOOK_4_STATUS
    }

    private enum MOD{
        B_PSK, Q_PSK, eight_PSK, B_FSK,
        B_PSK_coded, Q_PSK_coded, B_PSK_pna
    }

    private enum TYPE{
        PHY, DL
    }

    private enum ANSWER{
        ACK, NACK
    }

    private static int begin, len, cc, FCS_1, FCS_2, status;
    private static Vector<Integer> data;
    private static STATE state;

    private static MOD mod;
    private static boolean fec;
    private static TYPE type;

    private ToggleGroup toggleGroupLay = new ToggleGroup();
    private ToggleGroup toggleGroupMod = new ToggleGroup();

    private static final byte[] ACK = new byte[]{0x06};
    private static final byte[] NACK = new byte[]{0x15};

    private SerialPort comPort = SerialPort.getCommPorts()[0];

    public void initialize(){

        setButtonsDisable(true);
        setToggleGroup();

        SerialPort[] allPorts = SerialPort.getCommPorts();
        ObservableList<SerialPort> allPortsObservList = FXCollections.observableArrayList(allPorts);
        ports.setItems(allPortsObservList);
    }

    private void setToggleGroup(){

        BPSK.setToggleGroup(toggleGroupMod);
        QPSK.setToggleGroup(toggleGroupMod);
        eightPSK.setToggleGroup(toggleGroupMod);
        BFSK.setToggleGroup(toggleGroupMod);

        phyButton.setToggleGroup(toggleGroupLay);
        dlButton.setToggleGroup(toggleGroupLay);
    }

    @FXML
    public void connect() {

        mod = MOD.B_PSK;
        fec = false;
        type = TYPE.DL;

        data = new Vector<>();
        state = STATE.LOOK_4_BEGIN;

        SerialPort chosenPort = ports.getValue();

        if (chosenPort != null) {
            comPort = chosenPort;

            comPort.openPort();
            comPort.setComPortParameters(57600, 8, ONE_STOP_BIT, NO_PARITY);
            comPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

            comPort.addDataListener(new SerialPortDataListener() {
                @Override
                public int getListeningEvents() {
                    return LISTENING_EVENT_DATA_AVAILABLE; }

                @Override
                public void serialEvent(SerialPortEvent event) {

                    byte[] getByte = new byte[1];
                    InputStream in = comPort.getInputStream();

                    while(comPort.bytesAvailable() > 0) {

                        try {
                            in.read(getByte, 0, 1);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        int hex = getByte[0] & 0xff;

                        switch (state){

                            case LOOK_4_BEGIN:
                                if(hex == 0x02 || hex == 0x03){
                                    begin = hex;
                                    state = STATE.LOOK_4_LEN;
                                }
                                else if(hex == 0x3f){
                                    begin = hex;
                                    state = STATE.LOOK_4_STATUS;
                                }
                                else if(hex == 0x06){
                                    displayFrame("[06]", "[" + (char) 0x06 + "]");
                                }
                                else if(hex == 0x15){
                                    displayFrame("[15]", "[" + (char) 0x15 + "]");
                                }
                                data.clear();

                                break;

                            case LOOK_4_LEN:
                                len = hex;
                                state = STATE.LOOK_4_CC;

                                break;

                            case LOOK_4_CC:
                                cc = hex;
                                state = STATE.DATA_COLLECT;

                                break;

                            case DATA_COLLECT:
                                if(len == 0){
                                    state = STATE.LOOK_4_FCS_1_BYTE;
                                }
                                else {
                                    data.add(hex);
                                    len--;

                                    break;
                                }

                            case LOOK_4_FCS_1_BYTE:
                                FCS_1 = hex;
                                state = STATE.LOOK_4_FCS_2_BYTE;

                                break;

                            case LOOK_4_FCS_2_BYTE:
                                FCS_2 = hex;
                                state = STATE.LOOK_4_BEGIN;

                                Frame localFrame = new Frame(begin, data.size(), cc, data, FCS_1, FCS_2);
                                displayFrame(localFrame.toHexString(), localFrame.toAsciiString());

                                int checkFrame = localFrame.checkFrame();

                                if(checkFrame == 1){
                                    sendAnswer(ANSWER.ACK);
                                }
                                else if(checkFrame == -1){
                                    sendAnswer(ANSWER.NACK);
                                }

                                break;

                            case LOOK_4_STATUS:
                                status = hex;
                                state = STATE.LOOK_4_BEGIN;

                                Frame statusFrame = new Frame(begin, status);
                                displayFrame(statusFrame.toHexString(), statusFrame.toAsciiString());

                                break;
                        }

                        /*String hexToString = String.format("%02x", hex) + " ";
                        javafx.application.Platform.runLater(() -> receiveField.appendText(hexToString));*/
                    }
                }
            });

            setButtonsDisable(false);
        }
    }

    private void displayFrame(String hexFrame, String asciiFrame){

        String hexMessage = hexFrame + "\n";
        javafx.application.Platform.runLater(() -> hexField.appendText(hexMessage));

        String asciiMessage = asciiFrame + "\n";
        javafx.application.Platform.runLater(() -> asciiField.appendText(asciiMessage));
    }

    private void setButtonsDisable(boolean bool){

        ports.setDisable(!bool);

        connectButton.setDisable(!bool);
        disconnectButton.setDisable(bool);
        clearButton.setDisable(bool);
        sendButton.setDisable(bool);
        resetButton.setDisable(bool);

        dataType.setDisable(bool);
        dataType.setSelected(!bool);

        BPSK.setDisable(bool);
        QPSK.setDisable(bool);
        eightPSK.setDisable(bool);
        BFSK.setDisable(bool);

        phyButton.setDisable(bool);
        dlButton.setDisable(bool);

        if(bool){
            toggleGroupMod.selectToggle(null);
            toggleGroupLay.selectToggle(null);
        }
        else{
            toggleGroupMod.selectToggle(BPSK);
            toggleGroupLay.selectToggle(dlButton);
        }

        FEC.setDisable(bool);
        FEC.setSelected(false);
    }

    private void sendAnswer(ANSWER answer){

        comPort.setDTR();

        try {
            Thread.sleep(10);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        if(answer == ANSWER.ACK) {
            comPort.writeBytes(ACK, 1);
        }
        else if(answer == ANSWER.NACK){
            comPort.writeBytes(NACK, 1);
        }

        comPort.clearDTR();
    }

    @FXML
    public void disconnect() {

        comPort.removeDataListener();
        comPort.closePort();

        clear();
        setButtonsDisable(true);
    }

    @FXML
    public void reset(){

        Frame resetFrame = new Frame(0x00, 0x3c, new Vector<>());
        byte[] resetBytes = resetFrame.getBytes();

        beforeWrite();
        comPort.writeBytes(resetBytes, resetBytes.length);
        afterWrite();

        type = TYPE.DL;
        toggleGroupLay.selectToggle(dlButton);
    }

    private void beforeWrite(){

        comPort.setRTS();
        comPort.setDTR();

        try {
            Thread.sleep(10);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void afterWrite(){

        comPort.clearRTS();
        comPort.clearDTR();
    }

    @FXML
    public void changeDataType(){

        if(dataType.isSelected()){

            String hexText = sendField.getText();
            String asciiText = hexToAscii(hexText);

            sendField.setText(asciiText);
        }
        else{
            String asciiText = sendField.getText();
            char[] chars = asciiText.toCharArray();

            StringBuilder bytes = new StringBuilder();

            for(char getChar : chars){
                String hex = String.format("%02x", (int) getChar) + " ";
                bytes.append(hex);
            }
            sendField.setText(bytes.toString());
        }
    }

    private String hexToAscii(String hexText){

        String[] bytes = hexText.split("\\s");
        StringBuilder chars = new StringBuilder();

        for (String getByte : bytes) {
            if (getByte.matches("^[a-fA-F0-9]{2}$")) {
                chars.append((char) Integer.parseInt(getByte, 16));
            }
        }

        return chars.toString();
    }

    @FXML
    public void send(){

        int cc;

        if(type == TYPE.PHY) {
            cc = 0x24;
        }
        else {
            cc = 0x50;
        }

        Vector<Integer> data = new Vector<>();

        int firstByte = checkFirstByte();
        data.add(firstByte);

        String fieldText = sendField.getText();

        if(!dataType.isSelected()){
            fieldText = hexToAscii(fieldText);
        }

        char[] chars = fieldText.toCharArray();

        for(char getChar : chars){
            data.add((int) getChar);
        }

        int len = data.size();

        Frame dataFrame = new Frame(len, cc, data);
        byte[] dataBytes = dataFrame.getBytes();

        beforeWrite();
        comPort.writeBytes(dataBytes, dataBytes.length);
        afterWrite();
    }

    private int checkFirstByte() {

        int firstByte = 0;

        switch (mod){
            case B_PSK:
                break;

            case Q_PSK:
                firstByte = 0b001;
                break;

            case eight_PSK:
                firstByte = 0b010;
                break;

            case B_FSK:
                firstByte = 0b011;
                break;

            case B_PSK_coded:
                firstByte = 0b100;
                break;

            case Q_PSK_coded:
                firstByte = 0b101;
                break;

            case B_PSK_pna:
                firstByte = 0b111;
                break;
        }

        firstByte <<= 4;
        firstByte |= 0b0100;

        return firstByte;
    }

    @FXML
    private void clear(){

        sendField.clear();
        hexField.clear();
        asciiField.clear();
    }

    @FXML
    public void changeMod() {

        RadioButton radioButton = (RadioButton) toggleGroupMod.getSelectedToggle();
        String selected = radioButton.getId();

        if (selected.equals("eightPSK")) {
            mod = MOD.eight_PSK;

            FEC.setSelected(false);
            FEC.setDisable(true);
        }
        else {
            FEC.setDisable(false);

            if (!fec) {
                switch (selected) {
                    case "BPSK":
                        mod = MOD.B_PSK;
                        break;

                    case "QPSK":
                        mod = MOD.Q_PSK;
                        break;

                    case "BFSK":
                        mod = MOD.B_FSK;
                        break;
                }
            } else {
                switch (selected) {
                    case "BPSK":
                        mod = MOD.B_PSK_coded;
                        break;

                    case "QPSK":
                        mod = MOD.Q_PSK_coded;
                        break;

                    case "BFSK":
                        mod = MOD.B_PSK_pna;
                        break;
                }
            }
        }
    }

    @FXML
    public void changeFEC(){

        fec = FEC.isSelected();
        changeMod();
    }

    @FXML
    public void changeLay(){

        RadioButton radioButton = (RadioButton) toggleGroupLay.getSelectedToggle();
        String selected = radioButton.getId();

        if(selected.equals("dlButton")) {

            type = TYPE.DL;

            Vector<Integer> dlData = new Vector<>();
            dlData.add(0x00);
            dlData.add(0x11);

            Frame dlFrame = new Frame(0x02, 0x08, dlData);
            byte[] dlBytes = dlFrame.getBytes();

            beforeWrite();
            comPort.writeBytes(dlBytes, dlBytes.length);
            afterWrite();
        }

        else if(selected.equals("phyButton")) {

            type = TYPE.PHY;

            Vector<Integer> phyData = new Vector<>();
            phyData.add(0x00);
            phyData.add(0x10);

            Frame phyFrame = new Frame(0x02, 0x08, phyData);
            byte[] phyBytes = phyFrame.getBytes();

            beforeWrite();
            comPort.writeBytes(phyBytes, phyBytes.length);
            afterWrite();
        }
    }
}