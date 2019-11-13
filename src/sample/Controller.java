package sample;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;

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
    private Button clearButton;

    @FXML
    private TextField sendField;

    @FXML
    private Button sendButton;

    @FXML
    private Button resetButton;

    @FXML
    private TextArea receiveField;

    public enum STATE {
        LOOK_4_BEGIN, LOOK_4_LEN, LOOK_4_CC, DATA_COLLECT,
        LOOK_4_FCS_1_BYTE, LOOK_4_FCS_2_BYTE, LOOK_4_STATUS
    }

    private static STATE state = STATE.LOOK_4_BEGIN;
    private static int begin, len, cc, FCS_1, FCS_2, status;
    private static Vector<Integer> data = new Vector<>();

    private static final byte[] ACK = new byte[]{0x06};
    private static final byte[] NACK = new byte[]{0x15};

    private SerialPort comPort = SerialPort.getCommPorts()[0];

    public void initialize(){

        setButtonsDisable(true);

        SerialPort[] allPorts = SerialPort.getCommPorts();
        ObservableList<SerialPort> allPortsObservList = FXCollections.observableArrayList(allPorts);
        ports.setItems(allPortsObservList);
    }

    @FXML
    public void connect() {
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

                        int hex = getByte[0];

                        switch (state){

                            case LOOK_4_BEGIN:
                                if(hex == 0x02 || hex == 0x03){
                                    begin = hex;
                                    state = STATE.LOOK_4_LEN;
                                }
                                else if(hex == 0x3f) {
                                    begin = hex;
                                    state = STATE.LOOK_4_STATUS;
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
                                displayFrame(localFrame.toString());

                                int checkFrame = localFrame.checkFrame();

                                if(checkFrame == 1){
                                    comPort.writeBytes(ACK, 1);
                                }
                                else if(checkFrame == -1){
                                    comPort.writeBytes(NACK, 1);
                                }

                                break;

                            case LOOK_4_STATUS:
                                status = hex;
                                state = STATE.LOOK_4_BEGIN;

                                Frame statusFrame = new Frame(begin, status);
                                displayFrame(statusFrame.toString());

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

    private void displayFrame(String frame){

        String message = frame + "\n";
        javafx.application.Platform.runLater(() -> receiveField.appendText(message));
    }

    @FXML
    public void disconnect() {

        comPort.removeDataListener();
        comPort.closePort();

        clear();

        setButtonsDisable(true);
    }

    @FXML
    private void clear(){

        sendField.clear();
        receiveField.clear();
    }

    @FXML
    public void send(){

        beforeWrite();

        String sendText = sendField.getText();
        comPort.writeBytes(sendText.getBytes(), sendText.getBytes().length);

        afterWrite();
    }

    @FXML
    public void reset(){

        byte[] resetBytes = new byte[]{0x02, 0x00, 0x3C, 0x3C, 0x00};

        beforeWrite();
        comPort.writeBytes(resetBytes, resetBytes.length);
        afterWrite();
    }

    private void setButtonsDisable(boolean bool){

        ports.setDisable(!bool);
        connectButton.setDisable(!bool);
        disconnectButton.setDisable(bool);
        clearButton.setDisable(bool);
        sendButton.setDisable(bool);
        resetButton.setDisable(bool);
    }

    private void beforeWrite(){

        comPort.setRTS();
        comPort.setDTR();

        try {
            Thread.sleep(10);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void afterWrite(){

        comPort.clearRTS();
        comPort.clearDTR();
    }
}