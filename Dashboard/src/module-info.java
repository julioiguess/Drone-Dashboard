module Dashboard {
    requires javafx.controls;
    requires javafx.fxml;
    
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens models to com.fasterxml.jackson.databind;
    opens controllers to javafx.fxml;

    opens application to javafx.fxml;
    exports application;
}
