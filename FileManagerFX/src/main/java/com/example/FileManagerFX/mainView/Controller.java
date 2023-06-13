package com.example.FileManagerFX.mainView;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class Controller implements Initializable {
    @FXML
    private VBox mainForm;
    @FXML
    private Button backButton;
    @FXML
    private TableView<FileInfo> filesViewTable;
    @FXML
    private TextField pathField;
    private Client client;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        backButton.setDisable(true);

        SetTableColumn();

        try {
            client = new Client(new Socket("localhost", 1000));

            pathField.setText(Paths.get(client.getStringMessageFromServer()).normalize().toString());
            String json = client.getStringMessageFromServer();

            UpdateFileList(json);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        CheckDownloadFolder();
    }

    private void SetTableColumn()
    {
        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Имя");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        fileNameColumn.setPrefWidth(300);

        TableColumn<FileInfo, String> fileLastModifiedColumn = new TableColumn<>("Дата изменения");
        fileLastModifiedColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getLastModified()));
        fileLastModifiedColumn.setPrefWidth(150);

        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>("Тип");
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileType()));
        fileTypeColumn.setPrefWidth(150);

        TableColumn<FileInfo, String> fileSizeColumn = new TableColumn<>("Размер");
        fileSizeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileSize()));
        fileSizeColumn.setPrefWidth(60);

        filesViewTable.getColumns().addAll(fileNameColumn, fileLastModifiedColumn, fileTypeColumn, fileSizeColumn);
        filesViewTable.getSortOrder().add(fileTypeColumn);

        filesViewTable.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2)
                {
                    FileInfo fileInfo = filesViewTable.getSelectionModel().getSelectedItem();
                    Path path = Paths.get(pathField.getText()).resolve(fileInfo.getFileName());
                    if (Files.isDirectory(path))
                    {
                        client.sendMessageToServer('p', path.toString());
                        pathField.setText(path.toString());
                        String json = client.getStringMessageFromServer();

                        UpdateFileList(json);
                        backButton.setDisable(false);
                    }
                }
            }
        });
    }

    private void UpdateFileList(String json)
    {
        try {
            filesViewTable.getItems().clear();

            ObjectMapper objectMapper = new ObjectMapper();

            TypeReference<List<FileInfo>> mapType = new TypeReference<List<FileInfo>>() {};
            List<FileInfo> fileInfosList = objectMapper.readValue(json, mapType);

            filesViewTable.getItems().addAll(fileInfosList);
            filesViewTable.sort();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Не удалось вывести список файлов", ButtonType.OK);
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    private String getSelectedFileName()
    {
        return filesViewTable.getSelectionModel().getSelectedItem().getFileName();
    }

    private void CheckDownloadFolder()
    {
        if (!Files.exists(Paths.get("user.home/FileManagerDownloads")))
            new File(System.getProperty("user.home") + "\\FileManagerDownloads").mkdirs();
    }

    public void backButtonAction(ActionEvent actionEvent) {
        Path upperPath = Paths.get(pathField.getText()).getParent();
        if (null != upperPath)
        {
            client.sendMessageToServer('p', upperPath.toString());
            pathField.setText(upperPath.toString());
            String json = client.getStringMessageFromServer();

            UpdateFileList(json);
        }
        if (null == Paths.get(pathField.getText()).getParent()) {
            backButton.setDisable(true);
        }
    }

    public void buttonUploadAction(ActionEvent actionEvent) {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(mainForm.getScene().getWindow());

        try{
            client.sendMessageToServer(file);

            String json = client.getStringMessageFromServer();
            UpdateFileList(json);
        } catch (Exception e)
        {
            new Alert(Alert.AlertType.ERROR, "Не удалось загрузить файл", ButtonType.OK).showAndWait();
        }
    }

    public void buttonDownloadAction(ActionEvent actionEvent) {
        if (null == getSelectedFileName())
        {
            new Alert(Alert.AlertType.ERROR, "Не был выбран файл для загрузки", ButtonType.OK).showAndWait();
            return;
        }

        if (Files.isDirectory(Paths.get(getSelectedFileName())))
        {
            new Alert(Alert.AlertType.WARNING, "Выберете файл для загрузки, а не директорию", ButtonType.OK).showAndWait();
            return;
        }

        try
        {
            String sourcePath = Paths.get(pathField.getText(), getSelectedFileName()).toString();

            String destignationPath = System.getProperty("user.home");
            destignationPath += "\\FileManagerDownloads\\" + getSelectedFileName();



            client.sendMessageToServer('u', sourcePath);

            client.downloadFileFromServer(destignationPath);

        }
        catch (Exception ex)
        {
            new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    public void buttonDeleteAction(ActionEvent actionEvent) {
        if (null == getSelectedFileName())
        {
            new Alert(Alert.AlertType.ERROR, "Не был выбран файл для удаления", ButtonType.OK).showAndWait();
            return;
        }

        if (Files.isDirectory(Paths.get(getSelectedFileName())))
        {
            new Alert(Alert.AlertType.WARNING, "Выберете файл для удаления, а не директорию", ButtonType.OK).showAndWait();
            return;
        }

        try{
            Path sourcePath = Paths.get(pathField.getText(), getSelectedFileName());

            client.sendMessageToServer('d', sourcePath.toString());

            String json = client.getStringMessageFromServer();
            UpdateFileList(json);
        } catch (Exception ex)
        {
            new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK).showAndWait();
            ex.printStackTrace();
        }
    }
}