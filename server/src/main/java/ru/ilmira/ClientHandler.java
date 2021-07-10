package ru.ilmira;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {

    private final MyServer myServer;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    private String nickName;

    public String getNickName() {
        return nickName;
    }

    public ClientHandler(MyServer myServer, Socket socket) {
        try {
            this.myServer = myServer;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.nickName = "";
            new Thread(() -> {
                try {
                    authentication();
                    readMessages();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            }).start();
        } catch (IOException e) {
            throw new RuntimeException("Проблемы при создании обработчика клиента!");
        }
    }

    public void authentication() throws IOException {
        while (true) {
            String str = in.readUTF();
            if (str.startsWith("/auth")) {
                String[] parts = str.split("\\s");
                nickName = myServer.getAuthService().getNickByLoginPass(parts[1], parts[2]);
                if (nickName != null) {
                    if (!myServer.isNickBusy(nickName)) {
                        sendMsg("/authok " + nickName);
                        myServer.broadcastMsg(nickName + " зашел в чат...");
                        myServer.subscribe(this);
                        return;
                    } else {
                        sendMsg("Учетная запись уже используется!!!");
                    }
                } else {
                    sendMsg("Неверные логин/пароль!");
                }
            }
        }
    }

    public void readMessages() throws IOException {
        while (true) {
            String clientMsg = in.readUTF();
            System.out.println("от " + nickName + ": " + clientMsg);
            if (clientMsg.equalsIgnoreCase("/end")) {
                return;
            } else if (clientMsg.startsWith("/w ")) {
                String[] parts = clientMsg.split(" ");
                String nickNameTo = parts[1];
                String msgText = clientMsg.substring(3 + nickNameTo.length() + 1);
                myServer.sendPersonalMessage(this, nickNameTo, msgText);
            } else {
                myServer.broadcastMsg(this, clientMsg);
            }
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        myServer.unsubscribe(this);
        myServer.broadcastMsg(nickName + " вышел из чата");
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
