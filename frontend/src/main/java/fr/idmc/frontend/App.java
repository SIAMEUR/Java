package fr.idmc.frontend;

import fr.idmc.frontend.config.FrontendConfig;
import fr.idmc.frontend.navigation.Navigation;
import fr.idmc.frontend.service.MqttService;
import javafx.application.Application;
import javafx.stage.Stage;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private MqttService mqtt;

    @Override
    public void start(Stage stage) {
        try {
            mqtt = new MqttService();
            Navigation nav = new Navigation(stage, mqtt, FrontendConfig.CLIENT_ID);

            // On tente la connexion mais on n'echoue pas si elle ne passe pas :
            // l'utilisateur verra l'indicateur "Deconnecte" dans l'UI et les actions
            // qu'il essaiera afficheront un message d'erreur explicite.
            // La reconnexion automatique de Paho refera des tentatives en arriere-plan.
            try {
                mqtt.connecter();
            } catch (MqttException e) {
                log.warn("Broker injoignable au demarrage : {}", e.getMessage());
            }

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
