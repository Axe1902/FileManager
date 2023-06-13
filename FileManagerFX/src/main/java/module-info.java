module com.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;


    opens com.example.FileManagerFX.mainView to javafx.fxml;
    exports com.example.FileManagerFX.mainView;
}