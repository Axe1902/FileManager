package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Server {
    private ServerSocket serverSocket;
    private Socket socket;
    private DataInputStream dataInputStream;
    public DataOutputStream dataOutputStream;
    private BufferedWriter bufferedWriter;
    private String currentPath = null;
    private String newFileName;
    private FileOutputStream fileOutputStream;

    Server(ServerSocket serverSocket)
    {
        try {
            this.serverSocket = serverSocket;
            socket = serverSocket.accept();
            System.out.println("Client connect to server");
            dataInputStream = new DataInputStream(socket.getInputStream());
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            System.out.println("Error in server!");
            e.printStackTrace();
            CloseResources(socket, bufferedWriter, dataInputStream);
        }

    }

    public void sendStartMessage(String rootDirectory)
    {
        try {
            bufferedWriter.write(rootDirectory);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e)
        {
            e.printStackTrace();
            CloseResources(socket, bufferedWriter, dataInputStream);
        }
    }

    public void sendMessageToClient(Path path)
    {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            List<FileInfo> fileInfoList = Files.list(path).map(FileInfo::new).toList();
            String json = objectMapper.writeValueAsString(fileInfoList);

            bufferedWriter.write(json);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e)
        {
            e.printStackTrace();
            CloseResources(socket, bufferedWriter, dataInputStream);
        }
    }

    public void receiveMessageFromClient()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (socket.isConnected()) {
                    try {
                        char dataType = dataInputStream.readChar();
                        int length = dataInputStream.readInt();

                        if (dataType == 'p')
                        {
                            currentPath = GetStringFromByte(length);

                            assert currentPath != null;
                            sendMessageToClient(Paths.get(currentPath));

                        }
                        else if (dataType == 'n')
                        {
                            byte[] messageByte = new byte[length];
                            boolean end = false;
                            StringBuilder dataString = new StringBuilder(length);
                            int totalBytesRead = 0;
                            while(!end) {
                                int currentBytesRead = dataInputStream.read(messageByte);
                                totalBytesRead = currentBytesRead + totalBytesRead;
                                if(totalBytesRead <= length) {
                                    dataString
                                            .append(new String(messageByte, 0, currentBytesRead, StandardCharsets.UTF_8));

                                    end = true;
                                }
                            }

                            newFileName = dataString.toString();
                            fileOutputStream = new FileOutputStream(currentPath + "\\" + newFileName);

                        }
                        else if (dataType == 'f')
                        {
                            DownloadFileFromClient(length);

                            sendMessageToClient(Paths.get(currentPath));
                        }
                        else if (dataType == 'd')
                        {
                            String deletedPath = GetStringFromByte(length);

                            assert deletedPath != null;
                            Files.delete(Paths.get(deletedPath));

                            sendMessageToClient(Paths.get(currentPath));
                        }
                        else if (dataType == 'u')
                        {
                            String downloadedPath = GetStringFromByte(length);

                            assert downloadedPath != null;

                            UploadFileToClient(downloadedPath);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        CloseResources(socket, bufferedWriter, dataInputStream);
                        break;
                    }
                }
            }
        }).start();
    }

    public void CloseResources(Socket socket, BufferedWriter bufferedWriter, DataInputStream bufferedReader)
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

    private String GetStringFromByte(int length)
    {
        byte[] messageByte = new byte[length];
        boolean end = false;
        StringBuilder dataString = new StringBuilder(length);
        int totalBytesRead = 0;
        while(!end) {
            int currentBytesRead;
            try{
                currentBytesRead = dataInputStream.read(messageByte);
            } catch (IOException e){
                e.printStackTrace();
                return null;
            }
            totalBytesRead = currentBytesRead + totalBytesRead;
            if(totalBytesRead <= length) {
                dataString
                        .append(new String(messageByte, 0, currentBytesRead, StandardCharsets.UTF_8));
            } else {
                dataString
                        .append(new String(messageByte, 0, length - totalBytesRead + currentBytesRead,
                                StandardCharsets.UTF_8));
            }
            if(totalBytesRead >= length) {
                end = true;
            }
        }

        return dataString.toString();
    }

    private void UploadFileToClient(String downloadedPath)
    {
        try
        {
            File file = Paths.get(downloadedPath).toFile();
            FileInputStream fileInputStream = new FileInputStream(file);

            int count;

            dataOutputStream.writeLong(file.length());
            byte[] buffer = new byte[8192]; // or 4096, or more
            while ((count = fileInputStream.read(buffer)) != -1)
            {
                dataOutputStream.write(buffer, 0, count);
                dataOutputStream.flush();
            }
            fileInputStream.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void DownloadFileFromClient(int length)
    {
        try
        {
            long fileSize = dataInputStream.readLong();
            int bytes = 0;
            byte[] buffer = new byte[length];

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
        }
    }


    public String getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }
}
