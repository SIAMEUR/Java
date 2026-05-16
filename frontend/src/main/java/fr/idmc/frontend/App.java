package fr.idmc.frontend;

import fr.idmc.frontend.config.FrontendConfig;
import fr.idmc.frontend.navigation.Navigation;
import fr.idmc.frontend.service.MqttService;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private MqttService mqtt;

    @Override
    public void start(Stage stage) {
        try {
            // Service MQTT unique pour toute l'appli, connecte des le demarrage.
            mqtt = new MqttService();
            mqtt.connecter();

            Navigation nav = new Navigation(stage, mqtt, FrontendConfig.CLIENT_ID);

            stage.setTitle("Lunettes connectees");
            nav.aller("/fr/idmc/frontend/fxml/accueil.fxml");
            stage.show();
        } catch (Exception e) {
            log.error("Demarrage frontend impossible", e);
        }
    }

    @Override
    public void stop() {
        if (mqtt != null) mqtt.deconnecter();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
