package com.example.FileManagerFX.mainView;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {

    private Socket socket;
    private BufferedReader dataReader;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;

    Client(Socket socket)
    {
        try {
            this.socket = socket;
            dataReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataInputStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e)
        {
            e.printStackTrace();
            CloseResources(socket, dataOutputStream, dataReader);
        }
    }

    public String getStringMessageFromServer()
    {
        try {
            return dataReader.readLine();

        } catch (IOException e)
        {
            e.printStackTrace();
            CloseResources(socket, dataOutputStream, dataReader);
            return null;
        }
    }

    public void downloadFileFromServer(String pathToDownload)
    {
        try
        {
            FileOutputStream fileOutputStream = new FileOutputStream(pathToDownload);

            long fileSize = dataInputStream.readLong();
            int bytes = 0;
            byte[] buffer = new byte[8192];

            while (fileSize > 0
                    && (bytes = dataInputStream.read(
                    buffer, 0,
                    (int)Math.min(buffer.length, fileSize)))
                    != -1) {
                fileOutputStream.write(buffer, 0, bytes);
                fileSize -= bytes;
            }

            fileOutputStream.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            CloseResources(socket, dataOutputStream, dataReader);
        }
    }

    public void sendMessageToServer(char messageType, String path)
    {
        try {
            byte[] dataInBytes = path.getBytes(StandardCharsets.UTF_8);

            dataOutputStream.writeChar(messageType);
            dataOutputStream.writeInt(dataInBytes.length);
            dataOutputStream.write(dataInBytes);
            dataOutputStream.flush();
        } catch (IOException e)
        {
            e.printStackTrace();
            CloseResources(socket, dataOutputStream, dataReader);
        }
    }

    public void sendMessageToServer(File file)
    {
        try {
            String fileName = file.getName();
            byte[] dataInBytes = fileName.getBytes(StandardCharsets.UTF_8);

            dataOutputStream.writeChar('n');
            dataOutputStream.writeInt(dataInBytes.length);
            dataOutputStream.write(dataInBytes);
            dataOutputStream.flush();

            FileInputStream fileInputStream = new FileInputStream(file);

            int count;

            dataOutputStream.writeChar('f');
            dataOutputStream.writeInt(8192);
            dataOutputStream.writeLong(file.length());
            byte[] buffer = new byte[8192]; // or 4096, or more
            while ((count = fileInputStream.read(buffer)) != -1)
            {
                dataOutputStream.write(buffer, 0, count);
                dataOutputStream.flush();
            }
        } catch (IOException e)
        {
            e.printStackTrace();
            CloseResources(socket, dataOutputStream, dataReader);
        }
    }

    public void CloseResources(Socket socket, DataOutputStream bufferedWriter, BufferedReader bufferedReader)
    {
        try {
            if (bufferedReader != null)
            {
                bufferedReader.close();
            }
            if (bufferedWriter != null)
            {
                bufferedWriter.close();
            }
            if (socket != null)
            {
                socket.close();
            }
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
