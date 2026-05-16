module fr.idmc.frontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.eclipse.paho.client.mqttv3;
    requires org.slf4j;

    opens fr.idmc.frontend to javafx.fxml, javafx.graphics;

    // FXMLLoader instancie les controllers par reflexion : il faut lui ouvrir l'acces.
    opens fr.idmc.frontend.controller to javafx.fxml;
}
