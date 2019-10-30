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

    private Vector<String> vector = new Vector<>();
    private int current = 0;

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
            int off = 0;

            comPort.addDataListener(new SerialPortDataListener() {
                @Override
                public int getListeningEvents() {
                    return LISTENING_EVENT_DATA_AVAILABLE; }

                @Override
                public void serialEvent(SerialPortEvent event) {
                    byte[] getData = new byte[]{};

                    InputStream in = comPort.getInputStream();

                    try {
                        in.read(getData, 0, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    StringBuilder message = new StringBuilder();

                    for (byte data : getData) {
                        String hex = String.format("%02x", data);

                        message.append(hex);
                        vector.add(hex);
                    }

                    String oldMessage = receiveField.getText();
                    String newMessage = oldMessage + message;

                    receiveField.setText(newMessage);
                }
            });

            off++;
            setButtonsDisable(false);
        }
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