package org.example;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Paths;

public class Main {
    private static final String rootDir = "./Data";
    private static Server server;

    public static void main(String[] args) {
        try {
            server = new Server(new ServerSocket(1000));
            server.setCurrentPath(rootDir);
            server.sendStartMessage(rootDir);
            server.sendMessageToClient(Paths.get(rootDir));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error in server!");
        }

        server.receiveMessageFromClient();
    }
}