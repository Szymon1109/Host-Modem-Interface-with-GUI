package sample;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;

import java.util.Vector;

public class Controller {

    @FXML
    private TextField sendField;

    @FXML
    private TextArea receiveField;

    private Vector<String> vector = null;
    private int current = 0;

    private SerialPort comPort = SerialPort.getCommPorts()[0];

    public void initialize(){
        comPort.openPort();
        comPort.setBaudRate(57600);

        comPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_RECEIVED; }

            @Override
            public void serialEvent(SerialPortEvent event) {
                byte[] getData = event.getReceivedData();

                StringBuilder message = new StringBuilder();

                for (byte data : getData) {
                    String hex = String.format("%02x", data);

                    message.append(hex);
                    vector.add(hex);
                }

                String oldMessage = receiveField.getText();
                String newMessage = oldMessage + message + "\n";

                receiveField.setText(newMessage);
            }
        });
    }

    @FXML
    public void send(javafx.event.ActionEvent actionEvent){

        beforeWrite();

        String sendText = sendField.getText();
        comPort.writeBytes(sendText.getBytes(), sendText.getBytes().length);

        afterWrite();
    }

    @FXML
    public void reset(javafx.event.ActionEvent actionEvent){

        String resetCode = "30323030334333433030"; //02003C3C00 w 0x

        beforeWrite();
        comPort.writeBytes(resetCode.getBytes(), resetCode.getBytes().length);
        afterWrite();
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

    /* TODO: get all ports names
    SerialPort[] ports = SerialPort.getCommPorts();
    for (SerialPort port: ports)
        System.out.println(port.getSystemPortName());*/
}
