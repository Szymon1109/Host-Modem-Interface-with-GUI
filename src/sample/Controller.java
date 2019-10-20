package sample;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;

public class Controller {

    @FXML
    private TextField sendField;

    @FXML
    private TextArea receiveField;

    private SerialPort comPort = SerialPort.getCommPorts()[0];

    public void initialize(){
        comPort.openPort();

        comPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_RECEIVED; }

            @Override
            public void serialEvent(SerialPortEvent event) {
                byte[] getData = event.getReceivedData();

                StringBuilder message = new StringBuilder();

                for (byte data : getData) {
                    message.append((char) data);
                }

                String oldMessage = receiveField.getText();
                String newMessage = oldMessage + message + "\n";

                receiveField.setText(newMessage);
            }
        });
    }

    @FXML
    public void send(javafx.event.ActionEvent actionEvent){

        String sendText = sendField.getText();
        comPort.writeBytes(sendText.getBytes(), sendText.getBytes().length);
    }

    @FXML
    public void reset(javafx.event.ActionEvent actionEvent){

        String resetCode = "02003C3C00";

        sendField.setText(resetCode);
        comPort.writeBytes(resetCode.getBytes(), resetCode.getBytes().length);
    }

    /* TODO: get all ports names
    SerialPort[] ports = SerialPort.getCommPorts();
    for (SerialPort port: ports)
        System.out.println(port.getSystemPortName());*/
}
