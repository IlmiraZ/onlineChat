package ru.ilmira.controller;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import ru.ilmira.ChatConnection;
import ru.ilmira.ClientApp;
import ru.ilmira.UserProperties;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

public class ChatWindowController {
    @FXML
    private TextArea messageTA;
    @FXML
    private TextField inputTF;
    @FXML
    private Label nickName;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    @FXML
    private void sendMessage(ActionEvent event) {
        inputTF.requestFocus();
        if (inputTF.getText().isEmpty()) return;
        sendMessage(inputTF.getText());
        messageTA.appendText(inputTF.getText() + "\n");
        inputTF.clear();
    }

    private void sendMessage(String s) {
        try {
            out.writeUTF(s);
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Ошибка отправки сообщения");
            alert.setHeaderText("Ошибка отправки сообщения");
            alert.setContentText("При отправке сообщения возникла ошибка: " + e.getMessage());
            alert.show();
        }
    }

    @FXML
    private void initialize() {
        try {
            openLoginWindow();
            nickName.setText(UserProperties.nickName);
            openConnection();
            addCloseListener();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void addCloseListener() {
        EventHandler<WindowEvent> onCloseRequest = ClientApp.primaryStage.getOnCloseRequest();
        ClientApp.primaryStage.setOnCloseRequest(event -> {
            closeConnection();
            if (onCloseRequest != null) {
                onCloseRequest.handle(event);
            }
        });
    }

    private void openLoginWindow() throws IOException {
        Parent parent = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/fxml/authWindow.fxml")));
        Stage loginStage = new Stage();
        loginStage.setResizable(false);
        loginStage.initModality(Modality.APPLICATION_MODAL);
        loginStage.setScene(new Scene(parent));
        loginStage.setTitle("Авторизация");
        loginStage.setOnCloseRequest(event -> System.exit(0));
        loginStage.showAndWait();
    }

    private void openConnection() throws IOException {
        socket = ChatConnection.getSocket();
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                while (socket.isConnected()) {
                    String msg = in.readUTF();
                    if (msg.equalsIgnoreCase("/end")) {
                        break;
                    }
                    messageTA.appendText(msg + "\n");
                }
            } catch (Exception e) {
            } finally {
                try {
                    messageTA.appendText("Соединение с сервером разорвано...");
                    in.close();
                    out.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void closeConnection() {
        try {
            out.writeUTF("/end");
            socket.close();
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
