package com.example.GameBetApp;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
//mesw autou ginetai h sundesh me ton master
public class MasterConnection {

    public static Object sendCommand(String host, int port, String cmd, Object data) {
        Socket socket = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            out.writeObject(cmd);
            out.flush();

            out.writeObject(data);
            out.flush();

            return in.readObject();

        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {}
            try {
                if (out != null) out.close();
            } catch (Exception ignored) {}
            try {
                if (socket != null) socket.close();
            } catch (Exception ignored) {}
        }
    }
}

