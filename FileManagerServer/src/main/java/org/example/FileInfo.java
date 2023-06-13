package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FileInfo {
    private String fileName;
    private String lastModified;
    private String fileType;
    private String fileSize;

    private static final int kilobyte = 1024;

    public FileInfo(Path path)
    {
        try {
            fileName = path.getFileName().toString();

            lastModified = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ENGLISH)
                    .withZone(ZoneId.systemDefault())
                    .format(Files.getLastModifiedTime(path).toInstant());

            if (Files.isDirectory(path))
            {
                fileType = "Папка с файлами";
                fileSize = "";
            }
            else
            {
                fileType = "Файл";
                fileSize = String.format("%,d Kb", ConvertFileSize(Files.size(path)));
            }
        } catch (IOException ex) {
            throw  new RuntimeException("Невозможно получить информацию о файле");
        }
    }

    private long ConvertFileSize(long size)
    {
        return (size > kilobyte) ? (size / kilobyte) : 1;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }
}
