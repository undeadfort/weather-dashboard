module com.pablo.weather.weatherdashboard {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.net.http;
    requires org.xerial.sqlitejdbc;
    requires com.fasterxml.jackson.databind;

    opens com.pablo.weather.weatherdashboard to javafx.fxml;
    exports com.pablo.weather.weatherdashboard;
}
