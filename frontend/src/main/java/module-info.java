module fr.idmc.frontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.eclipse.paho.client.mqttv3;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;

    opens fr.idmc.frontend to javafx.fxml, javafx.graphics;
}
