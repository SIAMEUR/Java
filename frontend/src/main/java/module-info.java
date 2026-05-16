module fr.idmc.frontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.eclipse.paho.client.mqttv3;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires org.slf4j;

    opens fr.idmc.frontend to javafx.fxml, javafx.graphics;

    // FXMLLoader instancie les controllers par reflexion : il faut lui ouvrir l'acces.
    opens fr.idmc.frontend.controller to javafx.fxml;

    // Jackson lit les getters/setters par reflexion pour serialiser/deserialiser.
    opens fr.idmc.frontend.model to com.fasterxml.jackson.databind;
}
